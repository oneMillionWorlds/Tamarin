package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * One source of gestures for the gesture bindings in the manifest (see {@link Action.ActionBuilder#withGestureBinding}),
 * layered on top of an action's state from the runtime. An action is on if the layer below says so or a gesture bound
 * to it in this layer is active.
 * <p>
 *     Each frame each hand's state (e.g. Tamarin's own gesture detection, or the runtime's
 *     {@link HandTrackingAimState}) is fed in with {@link #update}, and then {@link #combine} merges the gestures into
 *     the action's state.
 * </p>
 * <p>
 *     Primarily for internal use (it is used by {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState}).
 * </p>
 *
 * @param <S> the per hand state the gestures are read from
 */
public class GestureBindingLayer<S>{

    private final Map<ActionHandle, List<GestureBinding>> gestureBindings = new HashMap<>();

    private final BiPredicate<Gesture, S> isGestureActive;

    private final BiFunction<Gesture, S, Float> gestureStrength;

    /*
     * The states this frame and last frame (needed to say if an action has changed). Missing if the hand wasn't tracked
     */
    private final EnumMap<HandSide, S> currentStates = new EnumMap<>(HandSide.class);
    private final EnumMap<HandSide, S> previousStates = new EnumMap<>(HandSide.class);

    /**
     * @param manifest the action manifest, for its gesture bindings
     * @param isInLayer which gestures this layer provides
     * @param isGestureActive reads if a gesture is on from a hand's state (for boolean actions)
     * @param gestureStrength reads how strongly a gesture is being made (0 to 1) from a hand's state (for float actions)
     */
    GestureBindingLayer(ActionManifest manifest, Predicate<Gesture> isInLayer, BiPredicate<Gesture, S> isGestureActive, BiFunction<Gesture, S, Float> gestureStrength){
        this.isGestureActive = isGestureActive;
        this.gestureStrength = gestureStrength;
        for(ActionSet actionSet : manifest.getActionSets()){
            for(Action action : actionSet.getActions()){
                List<GestureBinding> bindingsInLayer = new ArrayList<>();
                for(GestureBinding binding : action.getGestureBindings()){
                    if (isInLayer.test(binding.getGesture())){
                        bindingsInLayer.add(binding);
                    }
                }
                if (!bindingsInLayer.isEmpty()){
                    gestureBindings.put(action.getActionHandle(), bindingsInLayer);
                }
            }
        }
    }

    /**
     * Gestures detected by Tamarin itself, for gestures the runtime can't detect.
     *
     * @param isExtensionLoaded returns true if the given extension is loaded
     */
    public static GestureBindingLayer<HandGestureTamarinProvidedState> tamarinDetected(ActionManifest manifest, Predicate<String> isExtensionLoaded){
        return new GestureBindingLayer<>(manifest,
                gesture -> gesture.isDetectedByTamarin(isExtensionLoaded),
                (gesture, state) -> gesture.booleanEvaluator.apply(state),
                (gesture, state) -> gesture.floatEvaluator.apply(state));
    }

    /**
     * Gestures detected by the runtime and reported via XR_FB_hand_tracking_aim (only if it is loaded).
     *
     * @param isExtensionLoaded returns true if the given extension is loaded
     */
    public static GestureBindingLayer<HandTrackingAimState> aimStateDetected(ActionManifest manifest, Predicate<String> isExtensionLoaded){
        return new GestureBindingLayer<>(manifest,
                gesture -> gesture.isDetectedByAimState(isExtensionLoaded),
                (gesture, state) -> gesture.aimStateBooleanEvaluator.get().apply(state),
                (gesture, state) -> gesture.aimStateFloatEvaluator.get().apply(state));
    }

    /**
     * A layer with no gestures in it (e.g. before the action manifest has been registered, or in desktop simulation).
     */
    public static <S> GestureBindingLayer<S> none(){
        return new GestureBindingLayer<>(new ActionManifest(List.of()), gesture -> false, (gesture, state) -> false, (gesture, state) -> 0f);
    }

    /**
     * If any action has a gesture bound to it in this layer (if not there is no point calling {@link #update}).
     */
    public boolean hasGestures(){
        return !gestureBindings.isEmpty();
    }

    /**
     * Call once per frame for each hand.
     *
     * @param state the hand's state, or empty if it isn't available (e.g. the hand isn't tracked)
     */
    public void update(HandSide handSide, Optional<S> state){
        S previous = currentStates.remove(handSide);
        if (previous == null){
            previousStates.remove(handSide);
        }else{
            previousStates.put(handSide, previous);
        }
        state.ifPresent(s -> currentStates.put(handSide, s));
    }

    /**
     * The hand's state this frame, or empty if it isn't available.
     */
    public Optional<S> getState(HandSide handSide){
        return Optional.ofNullable(currentStates.get(handSide));
    }

    /**
     * Combines the action's state from the layer below with any gestures bound to it in this layer. The action is
     * on if either says so.
     *
     * @param restrictToInput the hand to consider (e.g. {@link HandSide#restrictToInputString}), or null for both
     */
    public BooleanActionState combine(ActionHandle action, String restrictToInput, BooleanActionState stateBelow){
        List<GestureBinding> bindings = gestureBindings.get(action);
        if (bindings == null){
            return stateBelow;
        }

        boolean gestureNow = false;
        boolean gestureBefore = false;
        for(GestureBinding binding : bindings){
            if (restrictToInput != null && !restrictToInput.equals(binding.getHandSide().restrictToInputString)){
                continue;
            }
            gestureNow |= isActive(currentStates.get(binding.getHandSide()), binding.getGesture());
            gestureBefore |= isActive(previousStates.get(binding.getHandSide()), binding.getGesture());
        }

        // if the state below changed it must have been the other value last frame
        boolean belowBefore = stateBelow.getState() != stateBelow.hasChanged();

        boolean combinedNow = stateBelow.getState() || gestureNow;
        boolean combinedBefore = belowBefore || gestureBefore;
        return new BooleanActionState(combinedNow, combinedNow != combinedBefore);
    }

    /**
     * Combines the action's state from the layer below with any gestures bound to it in this layer. The action's
     * value is the largest of the layer below's value and the bound gestures' strengths.
     * <p>
     *     If the layer below changed its previous value isn't known, so the result is reported as changed (even in the
     *     rare case that a stronger gesture hid the change).
     * </p>
     *
     * @param restrictToInput the hand to consider (e.g. {@link HandSide#restrictToInputString}), or null for both
     */
    public FloatActionState combine(ActionHandle action, String restrictToInput, FloatActionState stateBelow){
        List<GestureBinding> bindings = gestureBindings.get(action);
        if (bindings == null){
            return stateBelow;
        }

        float gestureNow = 0;
        float gestureBefore = 0;
        for(GestureBinding binding : bindings){
            if (restrictToInput != null && !restrictToInput.equals(binding.getHandSide().restrictToInputString)){
                continue;
            }
            gestureNow = Math.max(gestureNow, strength(currentStates.get(binding.getHandSide()), binding.getGesture()));
            gestureBefore = Math.max(gestureBefore, strength(previousStates.get(binding.getHandSide()), binding.getGesture()));
        }

        float combinedNow = Math.max(stateBelow.getState(), gestureNow);
        if (stateBelow.hasChanged()){
            return new FloatActionState(combinedNow, true);
        }
        // the layer below didn't change, so its value now was also its value last frame
        float combinedBefore = Math.max(stateBelow.getState(), gestureBefore);
        return new FloatActionState(combinedNow, combinedNow != combinedBefore);
    }

    private boolean isActive(S stateOrNull, Gesture gesture){
        return stateOrNull != null && isGestureActive.test(gesture, stateOrNull);
    }

    private float strength(S stateOrNull, Gesture gesture){
        return stateOrNull == null ? 0 : gestureStrength.apply(gesture, stateOrNull);
    }
}

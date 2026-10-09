package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tamarin's own detection of the gestures bound to actions in the manifest (see
 * {@link Action.ActionBuilder#withGestureBinding}). Each frame the hands' joints are fed in with {@link #update}, and
 * then {@link #combine} merges the detected gestures into the runtime's state for an action.
 * <p>
 *     Primarily for internal use (it is used by {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState}).
 * </p>
 */
public class TamarinGestureActions{

    private final Map<ActionHandle, List<GestureBinding>> gestureBindings = new HashMap<>();

    private final EnumMap<HandSide, HandGestureRules> rules = new EnumMap<>(HandSide.class);
    /*
     * The states this frame and last frame (needed to say if an action has changed). Missing if the hand wasn't tracked
     */
    private final EnumMap<HandSide, HandGestureTamarinProvidedState> currentStates = new EnumMap<>(HandSide.class);
    private final EnumMap<HandSide, HandGestureTamarinProvidedState> previousStates = new EnumMap<>(HandSide.class);

    /**
     * @param manifest the action manifest, for its gesture bindings
     * @param isExtensionLoaded returns true if the given extension is loaded. Gestures whose runtime equivalent
     *                          extension is loaded are left to the runtime (via the bindings they suggested) and
     *                          Tamarin doesn't detect them itself
     */
    public TamarinGestureActions(ActionManifest manifest, Predicate<String> isExtensionLoaded){
        for(ActionSet actionSet : manifest.getActionSets()){
            for(Action action : actionSet.getActions()){
                List<GestureBinding> tamarinDetected = new ArrayList<>();
                for(GestureBinding binding : action.getGestureBindings()){
                    boolean runtimeProvidesIt = binding.getGesture().runtimeEquivalentRequiredExtension.filter(isExtensionLoaded).isPresent();
                    if (!runtimeProvidesIt){
                        tamarinDetected.add(binding);
                    }
                }
                if (!tamarinDetected.isEmpty()){
                    gestureBindings.put(action.getActionHandle(), tamarinDetected);
                }
            }
        }
        for(HandSide handSide : HandSide.values()){
            rules.put(handSide, new HandGestureRules());
        }
    }

    /**
     * If any action has a gesture bound to it that Tamarin detects itself (if not there is no point calling
     * {@link #update}).
     */
    public boolean hasGestures(){
        return !gestureBindings.isEmpty();
    }

    /**
     * Call once per frame for each hand.
     *
     * @param stageJoints the hand's joints relative to the stage, or empty if the hand isn't tracked as a bare hand
     *                    (gestures aren't detected from joints that come from a held controller)
     */
    public void update(HandSide handSide, Optional<Map<HandJoint, BonePose>> stageJoints){
        HandGestureTamarinProvidedState previous = currentStates.remove(handSide);
        if (previous == null){
            previousStates.remove(handSide);
        }else{
            previousStates.put(handSide, previous);
        }

        if (stageJoints.isPresent()){
            currentStates.put(handSide, rules.get(handSide).apply(new HandJointMeasurements(stageJoints.get())));
        }else{
            rules.get(handSide).reset();
        }
    }

    /**
     * Tamarin's detected gestures for the hand this frame, or empty if it isn't tracked as a bare hand.
     */
    public Optional<HandGestureTamarinProvidedState> getState(HandSide handSide){
        return Optional.ofNullable(currentStates.get(handSide));
    }

    /**
     * Combines the runtime's state for the action with any gestures bound to it. The action is on if either the
     * runtime or a bound gesture says so.
     *
     * @param restrictToInput the hand to consider (e.g. {@link HandSide#restrictToInputString}), or null for both
     */
    public BooleanActionState combine(ActionHandle action, String restrictToInput, BooleanActionState runtimeState){
        List<GestureBinding> bindings = gestureBindings.get(action);
        if (bindings == null){
            return runtimeState;
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

        // if the runtime's state changed it must have been the other value last frame
        boolean runtimeBefore = runtimeState.getState() != runtimeState.hasChanged();

        boolean combinedNow = runtimeState.getState() || gestureNow;
        boolean combinedBefore = runtimeBefore || gestureBefore;
        return new BooleanActionState(combinedNow, combinedNow != combinedBefore);
    }

    private static boolean isActive(HandGestureTamarinProvidedState stateOrNull, Gesture gesture){
        return stateOrNull != null && gesture.booleanEvaluator.apply(stateOrNull);
    }
}

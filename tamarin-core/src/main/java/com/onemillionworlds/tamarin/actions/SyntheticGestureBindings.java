package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.actionprofile.GestureBinding;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;
import com.onemillionworlds.tamarin.handgestures.HandGestureState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Evaluates the Tamarin side (synthetic) gesture bindings of actions (see
 * {@link Action.ActionBuilder#withGestureBinding}) and combines them, hand by hand, with the runtime's bindings.
 * <p>
 *     The rule (per hand) depends on the {@link HandGestureSourcePreference}:
 * </p>
 * <ul>
 *     <li>{@link HandGestureSourcePreference#AUTOMATIC}: both together. A boolean action is on if either the runtime
 *     or a gesture binding has it on, a float action takes the larger value.</li>
 *     <li>{@link HandGestureSourcePreference#RUNTIME_ONLY}: the runtime takes precedence. Its value is used if any
 *     runtime binding is currently supplying the action (OpenXR's isActive, e.g. Meta's pinch_ext on hands),
 *     otherwise the gesture binding's value is.</li>
 *     <li>{@link HandGestureSourcePreference#TAMARIN_ONLY}: Tamarin takes precedence. The gesture binding's value is
 *     used whenever the hand is tracked.</li>
 *     <li>In all cases, if the hand isn't tracked as a bare hand (e.g. it is holding a controller), or the runtime
 *     reports the system gesture on it, only the runtime's value is used.</li>
 * </ul>
 * <p>
 *     Reading an action without restricting it to a hand combines the per hand results (on if either is on, or the
 *     larger value for floats), which is what OpenXR does for runtime bindings.
 * </p>
 * <p>
 *     Call {@link #update} once per frame, then combine whenever an action state is read.
 * </p>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public class SyntheticGestureBindings{

    private final Map<ActionHandle, ActionGestureBindings> bindingsByAction = new HashMap<>();

    private HandGestureSourcePreference preference = HandGestureSourcePreference.AUTOMATIC;

    public SyntheticGestureBindings(ActionManifest manifest){
        for(ActionSet actionSet : manifest.getActionSets()){
            for(Action action : actionSet.getActions()){
                if (action.getGestureBindings().isEmpty()){
                    continue;
                }
                ActionGestureBindings actionBindings = new ActionGestureBindings(action.getSupportedSubActionPaths());
                for(GestureBinding gestureBinding : action.getGestureBindings()){
                    actionBindings.byHand.computeIfAbsent(gestureBinding.getHandSide(), hand -> new ArrayList<>())
                            .add(new BindingState(gestureBinding, action.getActionSetName()));
                }
                bindingsByAction.put(action.getActionHandle(), actionBindings);
            }
        }
    }

    /**
     * For when there is no manifest (yet).
     */
    public static SyntheticGestureBindings none(){
        return new SyntheticGestureBindings(new ActionManifest(List.of()));
    }

    public boolean hasAny(){
        return !bindingsByAction.isEmpty();
    }

    public boolean hasGestureBindings(ActionHandle action){
        return bindingsByAction.containsKey(action);
    }

    /**
     * Evaluates all the gesture bindings for this frame.
     *
     * @param handStates the current gesture state of each hand (empty if the hand isn't tracked)
     * @param activeActionSets the names of the currently active action sets. Bindings in other action sets are treated as off
     * @param preference controls whether runtime bindings or gesture bindings take precedence
     */
    public void update(Function<HandSide, Optional<HandGestureState>> handStates, Collection<String> activeActionSets, HandGestureSourcePreference preference){
        this.preference = preference;
        Map<HandSide, Optional<HandGestureState>> statesThisFrame = new EnumMap<>(HandSide.class);
        for(HandSide handSide : HandSide.values()){
            statesThisFrame.put(handSide, handStates.apply(handSide));
        }

        for(ActionGestureBindings actionBindings : bindingsByAction.values()){
            for(List<BindingState> handBindings : actionBindings.byHand.values()){
                for(BindingState binding : handBindings){
                    Optional<HandGestureState> handState = activeActionSets.contains(binding.actionSetName)
                            ? statesThisFrame.get(binding.gestureBinding.getHandSide())
                            : Optional.empty();
                    binding.update(handState);
                }
            }
        }
    }

    /**
     * Works out a boolean action's state from the runtime's state and any gesture bindings.
     *
     * @param action the action
     * @param restrictToInput the sub action path (e.g. /user/hand/left) being read, or null for all
     * @param runtimeState gets the runtime's state of the action for a sub action path (or null for all)
     */
    public BooleanActionState combineBoolean(ActionHandle action, String restrictToInput, Function<String, RuntimeActionState> runtimeState){
        ActionGestureBindings actionBindings = bindingsByAction.get(action);
        if (actionBindings == null){
            RuntimeActionState state = runtimeState.apply(restrictToInput);
            return new BooleanActionState(state.getBooleanValue(), state.isChangedSinceLastSync());
        }
        boolean now = false;
        boolean previous = false;
        for(String subActionPath : subActionPathsToRead(actionBindings, restrictToInput)){
            SubActionResult result = resolve(actionBindings, subActionPath, runtimeState.apply(subActionPath), true);
            now |= result.now != 0;
            previous |= result.previous != 0;
        }
        return new BooleanActionState(now, now != previous);
    }

    /**
     * Works out a float action's state from the runtime's state and any gesture bindings.
     *
     * @param action the action
     * @param restrictToInput the sub action path (e.g. /user/hand/left) being read, or null for all
     * @param runtimeState gets the runtime's state of the action for a sub action path (or null for all)
     */
    public FloatActionState combineFloat(ActionHandle action, String restrictToInput, Function<String, RuntimeActionState> runtimeState){
        ActionGestureBindings actionBindings = bindingsByAction.get(action);
        if (actionBindings == null){
            RuntimeActionState state = runtimeState.apply(restrictToInput);
            return new FloatActionState(state.getValue(), state.isChangedSinceLastSync());
        }
        float now = 0;
        boolean changed = false;
        for(String subActionPath : subActionPathsToRead(actionBindings, restrictToInput)){
            SubActionResult result = resolve(actionBindings, subActionPath, runtimeState.apply(subActionPath), false);
            now = Math.max(now, result.now);
            changed |= result.changed;
        }
        return new FloatActionState(now, changed);
    }

    /**
     * Human-readable descriptions of the gesture bindings for an action, e.g. "Left Hand Thumbs up (gesture)".
     */
    public List<String> describe(ActionHandle action){
        ActionGestureBindings actionBindings = bindingsByAction.get(action);
        if (actionBindings == null){
            return List.of();
        }
        List<String> descriptions = new ArrayList<>();
        for(HandSide handSide : HandSide.values()){
            for(BindingState binding : actionBindings.byHand.getOrDefault(handSide, List.of())){
                String hand = handSide == HandSide.LEFT ? "Left Hand" : "Right Hand";
                descriptions.add(hand + " " + binding.gestureBinding.getGesture().getDescription() + " (gesture)");
            }
        }
        return descriptions;
    }

    private static List<String> subActionPathsToRead(ActionGestureBindings actionBindings, String restrictToInput){
        return restrictToInput == null ? actionBindings.subActionPaths : List.of(restrictToInput);
    }

    /**
     * Decides between the runtime and the gesture bindings for one sub action path (usually a hand).
     *
     * @param isBoolean booleans can work out their previous runtime value from the changed flag, floats can't
     */
    private SubActionResult resolve(ActionGestureBindings actionBindings, String subActionPath, RuntimeActionState runtimeState, boolean isBoolean){
        float runtimeNow = runtimeState.getValue();
        // a boolean that changed must have been the other value before. A float that changed could have been anything
        float runtimePrevious = !runtimeState.isChangedSinceLastSync() ? runtimeNow : (isBoolean ? 1 - runtimeNow : Float.NaN);

        List<BindingState> handBindings = handFor(subActionPath).map(hand -> actionBindings.byHand.get(hand)).orElse(null);
        if (handBindings == null){
            return new SubActionResult(runtimeNow, runtimePrevious, runtimeState.isChangedSinceLastSync());
        }

        float gestureNow = 0;
        float gesturePrevious = 0;
        boolean trackedNow = false;
        boolean trackedPrevious = false;
        for(BindingState binding : handBindings){
            gestureNow = Math.max(gestureNow, isBoolean ? (binding.active ? 1 : 0) : binding.value);
            gesturePrevious = Math.max(gesturePrevious, isBoolean ? (binding.previousActive ? 1 : 0) : binding.previousValue);
            trackedNow |= binding.tracked;
            trackedPrevious |= binding.previousTracked;
        }

        if (preference == HandGestureSourcePreference.AUTOMATIC){
            // both sources together, either can turn the action on
            float now = Math.max(runtimeNow, gestureNow);
            if (isBoolean){
                float previous = Math.max(runtimePrevious, gesturePrevious);
                return new SubActionResult(now, previous, now != previous);
            }
            return new SubActionResult(now, Float.NaN, runtimeState.isChangedSinceLastSync() || gestureNow != gesturePrevious);
        }

        // whether the runtime was active last frame isn't known, so assume it was the same as now
        boolean useGestureNow = useGesture(runtimeState.isActive(), trackedNow);
        boolean useGesturePrevious = useGesture(runtimeState.isActive(), trackedPrevious);

        float now = useGestureNow ? gestureNow : runtimeNow;
        float previous = useGesturePrevious ? gesturePrevious : runtimePrevious;
        boolean changed = !useGestureNow && !useGesturePrevious
                ? runtimeState.isChangedSinceLastSync()
                : !(now == previous); // NaN (an unknown previous float) counts as changed
        return new SubActionResult(now, previous, changed);
    }

    private boolean useGesture(boolean runtimeActive, boolean handTracked){
        if (!handTracked){
            return false;
        }
        return preference == HandGestureSourcePreference.TAMARIN_ONLY || !runtimeActive;
    }

    private static Optional<HandSide> handFor(String subActionPath){
        for(HandSide handSide : HandSide.values()){
            if (handSide.restrictToInputString.equals(subActionPath)){
                return Optional.of(handSide);
            }
        }
        return Optional.empty();
    }

    private static final class SubActionResult{
        final float now;
        final float previous;
        final boolean changed;

        SubActionResult(float now, float previous, boolean changed){
            this.now = now;
            this.previous = previous;
            this.changed = changed;
        }
    }

    private static final class ActionGestureBindings{
        final List<String> subActionPaths;
        final Map<HandSide, List<BindingState>> byHand = new EnumMap<>(HandSide.class);

        ActionGestureBindings(List<String> subActionPaths){
            this.subActionPaths = subActionPaths;
        }
    }

    private static final class BindingState{
        final GestureBinding gestureBinding;
        final String actionSetName;

        float value = 0;
        float previousValue = 0;
        boolean active = false;
        boolean previousActive = false;
        /**
         * If the hand is tracked (as a bare hand), in an active action set, and not doing the system gesture
         */
        boolean tracked = false;
        boolean previousTracked = false;

        BindingState(GestureBinding gestureBinding, String actionSetName){
            this.gestureBinding = gestureBinding;
            this.actionSetName = actionSetName;
        }

        void update(Optional<HandGestureState> handState){
            previousValue = value;
            previousActive = active;
            previousTracked = tracked;
            // while the system gesture is in progress (only the runtime knows about it) leave the hand to the runtime
            if (handState.isPresent() && !handState.get().isSystemGestureActive()){
                tracked = true;
                value = gestureBinding.getGesture().evaluate(handState.get());
                active = gestureBinding.getGesture().isActive(handState.get(), value, previousActive);
            } else{
                tracked = false;
                value = 0;
                active = false;
            }
        }
    }
}

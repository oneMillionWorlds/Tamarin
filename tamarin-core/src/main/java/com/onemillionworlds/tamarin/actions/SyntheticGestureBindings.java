package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.actionprofile.GestureBinding;
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
 * {@link Action.ActionBuilder#withGestureBinding}), producing the state of each action as supplied by its gesture
 * bindings alone. Combining that with the runtime's state for the action is done by {@link ActionSourceCombiner}.
 * <p>
 *     Call {@link #update} once per frame, then read the states.
 * </p>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public class SyntheticGestureBindings{

    private final Map<ActionHandle, Map<HandSide, List<BindingState>>> bindingsByAction = new HashMap<>();

    public SyntheticGestureBindings(ActionManifest manifest){
        for(ActionSet actionSet : manifest.getActionSets()){
            for(Action action : actionSet.getActions()){
                for(GestureBinding gestureBinding : action.getGestureBindings()){
                    bindingsByAction.computeIfAbsent(action.getActionHandle(), handle -> new EnumMap<>(HandSide.class))
                            .computeIfAbsent(gestureBinding.getHandSide(), hand -> new ArrayList<>())
                            .add(new BindingState(gestureBinding, action.getActionSetName()));
                }
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
     * @param activeActionSets the names of the currently active action sets. Bindings in other action sets are inactive
     */
    public void update(Function<HandSide, Optional<HandGestureState>> handStates, Collection<String> activeActionSets){
        Map<HandSide, Optional<HandGestureState>> statesThisFrame = new EnumMap<>(HandSide.class);
        for(HandSide handSide : HandSide.values()){
            statesThisFrame.put(handSide, handStates.apply(handSide));
        }

        for(Map<HandSide, List<BindingState>> actionBindings : bindingsByAction.values()){
            for(List<BindingState> handBindings : actionBindings.values()){
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
     * The state of a boolean action as supplied by its gesture bindings for one hand (on if any of the hand's gesture
     * bindings are on). Inactive if the action has no gesture bindings for that hand, or they can't currently supply
     * it (the hand isn't tracked, the action set isn't active, or the system gesture is in progress).
     */
    public ActionSourceState getBooleanState(ActionHandle action, HandSide handSide){
        List<BindingState> handBindings = handBindings(action, handSide);
        if (handBindings.isEmpty()){
            return ActionSourceState.INACTIVE;
        }
        boolean now = false;
        boolean previous = false;
        boolean active = false;
        for(BindingState binding : handBindings){
            now |= binding.isOn;
            previous |= binding.previousIsOn;
            active |= binding.available;
        }
        return ActionSourceState.ofBoolean(now, now != previous, active);
    }

    /**
     * The state of a float action as supplied by its gesture bindings for one hand (the largest value of the hand's
     * gesture bindings). Inactive if the action has no gesture bindings for that hand, or they can't currently supply
     * it (the hand isn't tracked, the action set isn't active, or the system gesture is in progress).
     */
    public ActionSourceState getFloatState(ActionHandle action, HandSide handSide){
        List<BindingState> handBindings = handBindings(action, handSide);
        if (handBindings.isEmpty()){
            return ActionSourceState.INACTIVE;
        }
        float now = 0;
        float previous = 0;
        boolean active = false;
        for(BindingState binding : handBindings){
            now = Math.max(now, binding.value);
            previous = Math.max(previous, binding.previousValue);
            active |= binding.available;
        }
        return new ActionSourceState(now, now != previous, active);
    }

    /**
     * Human-readable descriptions of the gesture bindings for an action, e.g. "Left Hand Thumbs up (gesture)".
     */
    public List<String> describe(ActionHandle action){
        List<String> descriptions = new ArrayList<>();
        for(HandSide handSide : HandSide.values()){
            for(BindingState binding : handBindings(action, handSide)){
                String hand = handSide == HandSide.LEFT ? "Left Hand" : "Right Hand";
                descriptions.add(hand + " " + binding.gestureBinding.getGesture().getDescription() + " (gesture)");
            }
        }
        return descriptions;
    }

    private List<BindingState> handBindings(ActionHandle action, HandSide handSide){
        return bindingsByAction.getOrDefault(action, Map.of()).getOrDefault(handSide, List.of());
    }

    private static final class BindingState{
        final GestureBinding gestureBinding;
        final String actionSetName;

        float value = 0;
        float previousValue = 0;
        boolean isOn = false;
        boolean previousIsOn = false;
        /**
         * If the binding can currently supply the action: the hand is tracked (as a bare hand), the action set is
         * active, and the system gesture isn't in progress
         */
        boolean available = false;

        BindingState(GestureBinding gestureBinding, String actionSetName){
            this.gestureBinding = gestureBinding;
            this.actionSetName = actionSetName;
        }

        void update(Optional<HandGestureState> handState){
            previousValue = value;
            previousIsOn = isOn;
            // while the system gesture is in progress (only the runtime knows about it) leave the hand to the runtime
            if (handState.isPresent() && !handState.get().isSystemGestureActive()){
                available = true;
                value = gestureBinding.getGesture().evaluate(handState.get());
                isOn = gestureBinding.getGesture().isActive(handState.get(), value, previousIsOn);
            } else{
                available = false;
                value = 0;
                isOn = false;
            }
        }
    }
}

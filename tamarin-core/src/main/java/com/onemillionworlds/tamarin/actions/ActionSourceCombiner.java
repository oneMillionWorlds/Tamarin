package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;

import java.util.List;

/**
 * Combines an action's state from the OpenXR runtime's bindings with its state from Tamarin's gesture bindings (see
 * {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}), for one hand.
 * <ul>
 *     <li>{@link HandGestureSourcePreference#AUTOMATIC}: both together. A boolean action is on if either is on, a
 *     float action takes the larger value.</li>
 *     <li>{@link HandGestureSourcePreference#RUNTIME_ONLY}: the runtime takes precedence. It is used if it is active
 *     (any runtime binding is currently supplying the action), otherwise the gesture bindings are.</li>
 *     <li>{@link HandGestureSourcePreference#TAMARIN_ONLY}: the gesture bindings take precedence. They are used if they
 *     are active (the hand is tracked), otherwise the runtime is (so controllers still work).</li>
 * </ul>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class ActionSourceCombiner{

    private ActionSourceCombiner(){}

    public static BooleanActionState combineBoolean(ActionSourceState runtime, ActionSourceState gesture, HandGestureSourcePreference preference){
        if (preference == HandGestureSourcePreference.AUTOMATIC){
            boolean now = runtime.getBooleanValue() || gesture.getBooleanValue();
            boolean previous = runtime.getPreviousBooleanValue() || gesture.getPreviousBooleanValue();
            return new BooleanActionState(now, now != previous);
        }
        ActionSourceState chosen = choose(runtime, gesture, preference);
        return new BooleanActionState(chosen.getBooleanValue(), chosen.isChangedSinceLastSync());
    }

    public static FloatActionState combineFloat(ActionSourceState runtime, ActionSourceState gesture, HandGestureSourcePreference preference){
        if (preference == HandGestureSourcePreference.AUTOMATIC){
            return new FloatActionState(Math.max(runtime.getValue(), gesture.getValue()), runtime.isChangedSinceLastSync() || gesture.isChangedSinceLastSync());
        }
        ActionSourceState chosen = choose(runtime, gesture, preference);
        return new FloatActionState(chosen.getValue(), chosen.isChangedSinceLastSync());
    }

    /**
     * Combines the states of several sub action paths (e.g. both hands) into one, the same way OpenXR does when an
     * action is read without a sub action path: on if any are on.
     */
    public static BooleanActionState anyOf(List<BooleanActionState> states){
        boolean now = false;
        boolean previous = false;
        for(BooleanActionState state : states){
            now |= state.getState();
            previous |= state.hasChanged() != state.getState();
        }
        return new BooleanActionState(now, now != previous);
    }

    /**
     * Combines the states of several sub action paths (e.g. both hands) into one, the same way OpenXR does when an
     * action is read without a sub action path: the largest value.
     */
    public static FloatActionState largestOf(List<FloatActionState> states){
        float value = 0;
        boolean changed = false;
        for(FloatActionState state : states){
            value = Math.max(value, state.getState());
            changed |= state.hasChanged();
        }
        return new FloatActionState(value, changed);
    }

    private static ActionSourceState choose(ActionSourceState runtime, ActionSourceState gesture, HandGestureSourcePreference preference){
        if (preference == HandGestureSourcePreference.TAMARIN_ONLY){
            return gesture.isActive() ? gesture : runtime;
        }
        // RUNTIME_ONLY
        return !runtime.isActive() && gesture.isActive() ? gesture : runtime;
    }
}

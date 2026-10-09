package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;

import java.util.List;

/**
 * Decides, for one hand, whether an action's state comes from the OpenXR runtime's bindings or from Tamarin's gesture
 * bindings (see {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}).
 * Only one source is ever used at a time (combining them would mean two slightly different versions of the same
 * gesture fighting each other).
 * <ul>
 *     <li>{@link HandGestureSourcePreference#AUTOMATIC} and {@link HandGestureSourcePreference#RUNTIME_ONLY}: the
 *     runtime takes precedence. It is used if it is active (a runtime binding is currently supplying the action, e.g.
 *     a controller button, or the hand interaction profile's pinch), otherwise the gesture bindings are.</li>
 *     <li>{@link HandGestureSourcePreference#TAMARIN_ONLY}: the gesture bindings take precedence. They are used if they
 *     are active (the hand is tracked as a bare hand, see {@link com.onemillionworlds.tamarin.handgestures.HandTrackingDataSource}),
 *     otherwise the runtime is (so held controllers still work).</li>
 * </ul>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class ActionSourceCombiner{

    private ActionSourceCombiner(){}

    public static BooleanActionState combineBoolean(ActionSourceState runtime, ActionSourceState gesture, HandGestureSourcePreference preference){
        ActionSourceState chosen = choose(runtime, gesture, preference);
        return new BooleanActionState(chosen.getBooleanValue(), chosen.isChangedSinceLastSync());
    }

    public static FloatActionState combineFloat(ActionSourceState runtime, ActionSourceState gesture, HandGestureSourcePreference preference){
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
        // AUTOMATIC and RUNTIME_ONLY
        return !runtime.isActive() && gesture.isActive() ? gesture : runtime;
    }
}

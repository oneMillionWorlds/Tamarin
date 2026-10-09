package com.onemillionworlds.tamarin.actions;

/**
 * The state of a boolean or float action as reported by the OpenXR runtime, including whether any of the runtime's
 * bindings is currently supplying it. Used to decide whether a runtime binding or a Tamarin gesture binding takes
 * precedence.
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class RuntimeActionState{
    private final float value;
    private final boolean changedSinceLastSync;
    private final boolean active;

    /**
     * @param value the action's value (for boolean actions 1 for true and 0 for false)
     * @param changedSinceLastSync if the value changed since the last action sync
     * @param active if an input source is currently supplying the action (OpenXR's isActive). False if nothing
     *               bound to the action is available (e.g. it is only bound to controller buttons and the user is
     *               using their bare hands)
     */
    public RuntimeActionState(float value, boolean changedSinceLastSync, boolean active){
        this.value = value;
        this.changedSinceLastSync = changedSinceLastSync;
        this.active = active;
    }

    public static RuntimeActionState ofBoolean(boolean value, boolean changedSinceLastSync, boolean active){
        return new RuntimeActionState(value ? 1 : 0, changedSinceLastSync, active);
    }

    public float getValue(){
        return value;
    }

    public boolean getBooleanValue(){
        return value != 0;
    }

    public boolean isChangedSinceLastSync(){
        return changedSinceLastSync;
    }

    public boolean isActive(){
        return active;
    }
}

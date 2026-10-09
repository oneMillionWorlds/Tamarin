package com.onemillionworlds.tamarin.actions;

/**
 * The state of a boolean or float action as supplied by one source: either the OpenXR runtime's bindings or
 * Tamarin's gesture bindings. Includes whether that source can currently supply the action at all, which is used to
 * decide which source takes precedence.
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class ActionSourceState{

    /**
     * A source that can't currently supply the action.
     */
    public static final ActionSourceState INACTIVE = new ActionSourceState(0, false, false);

    private final float value;
    private final boolean changedSinceLastSync;
    private final boolean active;

    /**
     * @param value the action's value (for boolean actions 1 for true and 0 for false)
     * @param changedSinceLastSync if the value changed since the last frame (action sync)
     * @param active if the source can currently supply the action. For the runtime this is OpenXR's isActive (false if
     *               nothing bound to the action is available, e.g. it is only bound to controller buttons and the
     *               user is using their bare hands). For gesture bindings it is false if the hand isn't tracked
     */
    public ActionSourceState(float value, boolean changedSinceLastSync, boolean active){
        this.value = value;
        this.changedSinceLastSync = changedSinceLastSync;
        this.active = active;
    }

    public static ActionSourceState ofBoolean(boolean value, boolean changedSinceLastSync, boolean active){
        return new ActionSourceState(value ? 1 : 0, changedSinceLastSync, active);
    }

    public float getValue(){
        return value;
    }

    public boolean getBooleanValue(){
        return value != 0;
    }

    /**
     * For a boolean action, what the value was at the previous sync.
     */
    public boolean getPreviousBooleanValue(){
        return changedSinceLastSync ? !getBooleanValue() : getBooleanValue();
    }

    public boolean isChangedSinceLastSync(){
        return changedSinceLastSync;
    }

    public boolean isActive(){
        return active;
    }

    @Override
    public String toString(){
        return "ActionSourceState{value=" + value + ", changedSinceLastSync=" + changedSinceLastSync + ", active=" + active + '}';
    }
}

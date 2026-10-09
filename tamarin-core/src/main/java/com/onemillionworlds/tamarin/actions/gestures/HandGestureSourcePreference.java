package com.onemillionworlds.tamarin.actions.gestures;

/**
 * Where gesture bindings (see {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding})
 * get their gestures from: the runtime's own gesture recognition, or Tamarin's (worked out from the hand's joints).
 * Set globally with {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#setHandGestureSourcePreference}.
 */
public enum HandGestureSourcePreference{
    /**
     * The runtime's version of a gesture if it has one, otherwise Tamarin's. This is the default and is what most
     * applications want.
     */
    AUTOMATIC,
    /**
     * Only the runtime's gestures. Gestures the runtime doesn't recognise (e.g. a fist) never activate.
     */
    RUNTIME_ONLY,
    /**
     * Always Tamarin's gestures, even where the runtime has its own (mostly useful for comparing them).
     * <p>
     *     Note that gestures whose runtime version is a suggested binding (e.g. {@link Gesture#PINCH_INDEX}, which
     *     suggests the runtime's pinch_ext) still also get the runtime's version. That binding is part of the action
     *     registered with the runtime at start up, so it can't be switched off afterwards.
     * </p>
     */
    TAMARIN_ONLY
}

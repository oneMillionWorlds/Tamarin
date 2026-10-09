package com.onemillionworlds.tamarin.handgestures;

/**
 * Controls how the runtime's gesture recognition and Tamarin's own are used. It affects two things:
 * <ul>
 *     <li>Which {@link HandGestureSource} supplies the pinches and aim in a {@link HandGestureState} (only pinches and
 *     aim have a runtime version, the other gestures are always Tamarin's).</li>
 *     <li>For actions with gesture bindings (see
 *     {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}), whether the
 *     runtime's bindings (e.g. a controller button, or the hand interaction profile's pinch) or Tamarin's gesture
 *     bindings take precedence. Only one is used at a time for each hand.</li>
 * </ul>
 */
public enum HandGestureSourcePreference{
    /**
     * Use the runtime's version of a gesture if it has one, otherwise Tamarin's. Pinches and aim come from the runtime
     * if it provides them (currently Meta runtimes). For actions, a runtime binding takes precedence whenever the
     * runtime is supplying the action, otherwise the gesture binding is used. This is the default and is the right
     * choice for most applications.
     */
    AUTOMATIC,
    /**
     * Only use the runtime's pinches and aim (on runtimes that don't provide them no gesture state is available at
     * all). For actions, as {@link #AUTOMATIC}, but gesture bindings can then only use the runtime's pinches. Mostly
     * useful for testing and comparison.
     */
    RUNTIME_ONLY,
    /**
     * Only use Tamarin's pinches and aim (worked out from the hand joints), even when the runtime could provide them.
     * For actions, gesture bindings take precedence whenever the hand is tracked as a bare hand. The runtime's
     * bindings are still used while a controller is held, provided the runtime either reports no hand joints then
     * (e.g. Quest) or supports XR_EXT_hand_tracking_data_source (so joints worked out from a controller, e.g. on
     * SteamVR with Index controllers, can be told apart). Useful for testing and comparison.
     */
    TAMARIN_ONLY
}

package com.onemillionworlds.tamarin.handgestures;

/**
 * Controls how the runtime's gesture recognition and Tamarin's own are used. It affects two things:
 * <ul>
 *     <li>Which {@link HandGestureSource} supplies the pinches and aim in a {@link HandGestureState} (only pinches and
 *     aim have a runtime version, the other gestures are always Tamarin's).</li>
 *     <li>How actions with gesture bindings (see
 *     {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}) combine
 *     the gesture bindings with the runtime's own bindings for that action (e.g. Meta's pinch_ext).</li>
 * </ul>
 */
public enum HandGestureSourcePreference{
    /**
     * Use the runtime's pinches and aim if it provides them (currently Meta runtimes), otherwise Tamarin's. Actions
     * use both runtime and gesture bindings together (a boolean action is on if either is on, a float action takes the
     * larger value). This is the default and is the right choice for most applications.
     */
    AUTOMATIC,
    /**
     * Only use the runtime's pinches and aim. On runtimes that don't provide them no gesture state will be available
     * at all. Actions use their runtime bindings in preference: a gesture binding is only used if no runtime binding
     * is currently supplying the action. Mostly useful for testing and comparison.
     */
    RUNTIME_ONLY,
    /**
     * Only use Tamarin's pinches and aim (worked out from the hand joints), even when the runtime could provide them.
     * Actions use their gesture bindings in preference whenever the hand is tracked (the runtime's bindings are still
     * used when it isn't, e.g. while holding a controller). Useful for consistent behaviour across all runtimes, or
     * for testing and comparison.
     */
    TAMARIN_ONLY
}

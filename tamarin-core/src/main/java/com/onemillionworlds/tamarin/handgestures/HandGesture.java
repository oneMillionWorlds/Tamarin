package com.onemillionworlds.tamarin.handgestures;

/**
 * A hand gesture that can be bound to an action (see
 * {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}). The built-in
 * gestures are in {@link HandGestures}.
 * <p>
 *     Custom gestures can be made by implementing this interface (or using {@link HandGestures#custom}). The
 *     {@link HandGestureState} gives access to the hand's joints (bones) for working out custom gestures.
 * </p>
 */
public interface HandGesture{

    /**
     * How strongly the gesture is being made, from 0 (not at all) to 1 (fully). This is the value a float action
     * bound to the gesture will have.
     */
    float evaluate(HandGestureState state);

    /**
     * Whether the gesture is "on" (the value a boolean action bound to the gesture will have).
     * <p>
     *     The default uses {@link #activateThreshold()} and {@link #deactivateThreshold()} as hysteresis on the
     *     evaluated value, so the gesture doesn't flicker on and off.
     * </p>
     *
     * @param state the hand's current gesture state
     * @param value the value {@link #evaluate} returned this frame
     * @param wasActive if the gesture was active last frame
     */
    default boolean isActive(HandGestureState state, float value, boolean wasActive){
        return Hysteresis.isActive(value, wasActive, activateThreshold(), deactivateThreshold());
    }

    /**
     * The value at which the gesture turns on (only used by the default {@link #isActive}).
     */
    default float activateThreshold(){
        return 0.8f;
    }

    /**
     * The value below which the gesture turns off again once on (only used by the default {@link #isActive}).
     */
    default float deactivateThreshold(){
        return 0.6f;
    }

    /**
     * A short, human-readable (English, not localised) description, e.g. "Thumbs up". Used when describing what an
     * action is bound to.
     */
    String getDescription();
}

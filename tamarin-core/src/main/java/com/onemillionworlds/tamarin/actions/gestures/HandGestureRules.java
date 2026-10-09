package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * Applies the gesture rules (e.g. a thumbs up is the four fingers curled with the thumb straight and pointing up) to
 * a hand's {@link HandJointMeasurements}, producing a {@link HandGestureTamarinProvidedState}.
 * <p>
 *     One instance per hand, as it holds the on/off state of each gesture between frames (so gestures don't flicker
 *     when they are near their thresholds). Call {@link #apply} once per frame while the hand is tracked, and
 *     {@link #reset()} while it isn't.
 * </p>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public class HandGestureRules{

    static final float PINCH_ACTIVATE = 0.85f;
    static final float PINCH_DEACTIVATE = 0.65f;
    /**
     * To start a pinch, every other finger's pinch strength must be at most this. If two fingers are pinching (or
     * nearly pinching) at once it is ambiguous which finger is meant (e.g. all the fingers bunched together against the
     * thumb) and it isn't a pinch at all.
     */
    static final float PINCH_OTHER_FINGERS_MAX_TO_START = 0.4f;
    /**
     * A pinch is released if any other finger's pinch strength goes above this (higher than the start threshold, so
     * the pinch doesn't flicker on and off).
     */
    static final float PINCH_OTHER_FINGERS_MAX_TO_CONTINUE = 0.6f;
    static final float GESTURE_ACTIVATE = 0.75f;
    static final float GESTURE_DEACTIVATE = 0.55f;
    /**
     * How much the thumb must point upwards (the dot product of its direction with the stage's up, i.e. the user's real up) for a thumbs up. At or
     * below the minimum it doesn't count at all, at or above the full value it counts completely.
     */
    static final float THUMB_UP_MINIMUM = 0.5f;
    static final float THUMB_UP_FULL = 0.9f;

    /**
     * Pinches are exclusive, at most one finger is pinching at a time. Null if none.
     */
    private Finger activePinch = null;
    private final Hysteresis fistHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis pointHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis thumbsUpHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);

    public HandGestureTamarinProvidedState apply(HandJointMeasurements measurements){
        HandGestureTamarinProvidedState.Builder builder = HandGestureTamarinProvidedState.builder();

        float fourFingersCurled = Math.min(
                Math.min(measurements.getCurl(Finger.INDEX), measurements.getCurl(Finger.MIDDLE)),
                Math.min(measurements.getCurl(Finger.RING), measurements.getCurl(Finger.LITTLE)));

        // in a fist the thumb is tucked over the fingers
        float fistStrength = Math.min(fourFingersCurled, measurements.getThumbTucked());
        builder.fist(fistStrength, fistHysteresis.update(fistStrength));

        float otherFingersCurled = Math.min(measurements.getCurl(Finger.MIDDLE), Math.min(measurements.getCurl(Finger.RING), measurements.getCurl(Finger.LITTLE)));
        float pointStrength = Math.min(1 - measurements.getCurl(Finger.INDEX), otherFingersCurled);
        builder.point(pointStrength, pointHysteresis.update(pointStrength));

        float thumbUpwards = FastMath.clamp((measurements.getThumbDirection().dot(Vector3f.UNIT_Y) - THUMB_UP_MINIMUM) / (THUMB_UP_FULL - THUMB_UP_MINIMUM), 0, 1);
        float thumbStraightAndOut = Math.min(1 - measurements.getCurl(Finger.THUMB), 1 - measurements.getThumbTucked());
        float thumbsUpStrength = Math.min(fourFingersCurled, Math.min(thumbStraightAndOut, thumbUpwards));
        builder.thumbsUp(thumbsUpStrength, thumbsUpHysteresis.update(thumbsUpStrength));

        updateActivePinch(measurements);
        for(Finger finger : Finger.PINCHING_FINGERS){
            builder.pinch(finger, measurements.getPinchStrength(finger), finger == activePinch);
        }
        return builder.build();
    }

    /**
     * Forgets all gesture state (e.g. because the hand has stopped being tracked).
     */
    public void reset(){
        activePinch = null;
        fistHysteresis.reset();
        pointHysteresis.reset();
        thumbsUpHysteresis.reset();
    }

    /**
     * A finger is only pinching if no other finger is pinching or nearly pinching. If two are, neither counts (rather
     * than the strongest winning).
     */
    private void updateActivePinch(HandJointMeasurements measurements){
        if (activePinch != null){
            boolean stillStrong = measurements.getPinchStrength(activePinch) >= PINCH_DEACTIVATE;
            boolean othersStillWeak = strongestOtherPinch(measurements, activePinch) <= PINCH_OTHER_FINGERS_MAX_TO_CONTINUE;
            if (!stillStrong || !othersStillWeak){
                activePinch = null;
            }
        }
        if (activePinch == null){
            for(Finger finger : Finger.PINCHING_FINGERS){
                boolean strong = measurements.getPinchStrength(finger) >= PINCH_ACTIVATE;
                boolean othersWeak = strongestOtherPinch(measurements, finger) <= PINCH_OTHER_FINGERS_MAX_TO_START;
                if (strong && othersWeak){
                    activePinch = finger;
                }
            }
        }
    }

    private static float strongestOtherPinch(HandJointMeasurements measurements, Finger finger){
        float strongest = 0;
        for(Finger other : Finger.PINCHING_FINGERS){
            if (other != finger){
                strongest = Math.max(strongest, measurements.getPinchStrength(other));
            }
        }
        return strongest;
    }
}

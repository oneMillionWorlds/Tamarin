package com.onemillionworlds.tamarin.handgestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks the gestures of one hand over time (which is needed for hysteresis). Call
 * {@link #update} once per frame, then read the results with {@link #getState}.
 * <p>
 *     Each frame it produces up to two states: one using Tamarin's own gesture recognition and (if the runtime
 *     provides it) one using the runtime's. Both are always kept up to date so that the hysteresis is correct whichever
 *     is asked for, and the preference can be changed at any time.
 * </p>
 * <p>
 *     Primarily for internal use (it is used by {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState}).
 * </p>
 */
public class HandGestureTracker{

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

    private final HandSide handSide;

    /**
     * Tamarin's pinches are exclusive (like the runtime's), at most one finger is pinching at a time. Null if none
     */
    private Finger activePinch = null;
    private final Hysteresis fistHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis pointHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis thumbsUpHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);

    private Optional<HandGestureState> tamarinState = Optional.empty();
    private Optional<HandGestureState> runtimeState = Optional.empty();

    public HandGestureTracker(HandSide handSide){
        this.handSide = handSide;
    }

    /**
     * @param trackedHand the hand's data this frame, or empty if the hand isn't currently tracked. Joints that come from
     *                    a held controller ({@link HandTrackingDataSource#CONTROLLER}) are treated as not tracked, as the
     *                    hand isn't free to make gestures
     */
    public void update(Optional<TrackedHand> trackedHand){
        if (!trackedHand.isPresent() || trackedHand.get().getDataSource() == HandTrackingDataSource.CONTROLLER){
            reset();
            return;
        }
        Map<HandJoint, BonePose> joints = trackedHand.get().getWorldJoints();

        // these are shared by both the Tamarin and runtime states
        fistHysteresis.update(JointGestureCalculations.fistStrength(joints));
        pointHysteresis.update(JointGestureCalculations.pointStrength(joints));
        thumbsUpHysteresis.update(JointGestureCalculations.thumbsUpStrength(joints));

        HandGestureState.Builder tamarinBuilder = HandGestureState.builder(handSide, HandGestureSource.TAMARIN);
        addTamarinOnlyGestures(tamarinBuilder, joints);
        addTamarinPinches(tamarinBuilder, joints);
        tamarinState = Optional.of(tamarinBuilder.build());

        runtimeState = trackedHand.get().getRuntimeAimState().map(runtimeAimState -> {
            HandGestureState.Builder runtimeBuilder = HandGestureState.builder(handSide, HandGestureSource.RUNTIME);
            addTamarinOnlyGestures(runtimeBuilder, joints);
            addRuntimePinches(runtimeBuilder, runtimeAimState);
            return runtimeBuilder.build();
        });
    }

    /**
     * @param preference which source to use for the pinches
     * @return the hand's gestures, or empty if the hand isn't tracked (or the preference is
     *         {@link HandGestureSourcePreference#RUNTIME_ONLY} and the runtime doesn't provide gestures)
     */
    public Optional<HandGestureState> getState(HandGestureSourcePreference preference){
        switch(preference){
            case RUNTIME_ONLY:
                return runtimeState;
            case TAMARIN_ONLY:
                return tamarinState;
            case AUTOMATIC:
            default:
                return runtimeState.isPresent() ? runtimeState : tamarinState;
        }
    }

    private void reset(){
        activePinch = null;
        fistHysteresis.reset();
        pointHysteresis.reset();
        thumbsUpHysteresis.reset();
        tamarinState = Optional.empty();
        runtimeState = Optional.empty();
    }

    /**
     * The gestures Tamarin always works out itself (no runtime provides them).
     */
    private void addTamarinOnlyGestures(HandGestureState.Builder builder, Map<HandJoint, BonePose> joints){
        for(Finger finger : Finger.values()){
            builder.fingerCurl(finger, JointGestureCalculations.fingerCurl(joints, finger));
        }
        builder.fist(JointGestureCalculations.fistStrength(joints), fistHysteresis.isActive());
        builder.point(JointGestureCalculations.pointStrength(joints), pointHysteresis.isActive());
        builder.thumbsUp(JointGestureCalculations.thumbsUpStrength(joints), thumbsUpHysteresis.isActive());
        builder.palm(joints.get(HandJoint.PALM_EXT).position(), JointGestureCalculations.palmNormal(joints));
        builder.joints(joints);
    }

    /**
     * Like the runtime's, Tamarin's pinches are exclusive: a finger is only pinching if no other finger is pinching or
     * nearly pinching. If two are, neither counts (rather than the strongest winning). The strengths are not exclusive
     * (they are just how close each finger is to the thumb).
     */
    private void addTamarinPinches(HandGestureState.Builder builder, Map<HandJoint, BonePose> joints){
        Map<Finger, Float> strengths = new EnumMap<>(Finger.class);
        for(Finger finger : Finger.PINCHING_FINGERS){
            strengths.put(finger, JointGestureCalculations.pinchStrength(joints, finger));
        }

        if (activePinch != null){
            boolean stillStrong = strengths.get(activePinch) >= PINCH_DEACTIVATE;
            boolean othersStillWeak = strongestOtherFinger(strengths, activePinch) <= PINCH_OTHER_FINGERS_MAX_TO_CONTINUE;
            if (!stillStrong || !othersStillWeak){
                activePinch = null;
            }
        } else{
            for(Finger finger : Finger.PINCHING_FINGERS){
                boolean strong = strengths.get(finger) >= PINCH_ACTIVATE;
                boolean othersWeak = strongestOtherFinger(strengths, finger) <= PINCH_OTHER_FINGERS_MAX_TO_START;
                if (strong && othersWeak){
                    activePinch = finger;
                }
            }
        }

        for(Finger finger : Finger.PINCHING_FINGERS){
            builder.pinch(finger, strengths.get(finger), finger == activePinch);
        }
    }

    /**
     * The largest pinch strength of the fingers other than the given one.
     */
    private static float strongestOtherFinger(Map<Finger, Float> strengths, Finger finger){
        float strongest = 0;
        for(Finger other : Finger.PINCHING_FINGERS){
            if (other != finger){
                strongest = Math.max(strongest, strengths.get(other));
            }
        }
        return strongest;
    }

    private static void addRuntimePinches(HandGestureState.Builder builder, HandTrackingAimState runtimeAimState){
        for(Finger finger : Finger.PINCHING_FINGERS){
            builder.pinch(finger, runtimeAimState.getPinchStrength(finger), runtimeAimState.isPinching(finger));
        }
        builder.runtimeFlags(runtimeAimState.isSystemGestureActive(), runtimeAimState.isMenuPressed(), runtimeAimState.isDominantHand());
    }
}

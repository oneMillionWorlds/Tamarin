package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks the gestures of one hand over time (which is needed for hysteresis and smoothing). Call
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
     * To start a pinch, the pinching finger's tip must be at least this much closer to the thumb (at the reference
     * hand size) than any other finger's. Otherwise it is ambiguous which finger is pinching (e.g. all the fingers
     * bunched together against the thumb) and it isn't a pinch at all.
     */
    static final float PINCH_START_SEPARATION = 0.007f;
    /**
     * A pinch is released if another finger's tip comes within this distance (at the reference hand size) of being as
     * close to the thumb as the pinching finger's.
     */
    static final float PINCH_RELEASE_SEPARATION = 0.002f;
    static final float GESTURE_ACTIVATE = 0.75f;
    static final float GESTURE_DEACTIVATE = 0.55f;

    /**
     * The time constant (in seconds) of the smoothing applied to Tamarin's estimated aim ray. Larger is smoother but
     * lags more.
     */
    static final float AIM_SMOOTHING_TIME = 0.04f;

    private final HandSide handSide;

    /**
     * Tamarin's pinches are exclusive (like the runtime's), at most one finger is pinching at a time. Null if none
     */
    private Finger activePinch = null;
    private final Hysteresis fistHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis pointHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);
    private final Hysteresis thumbsUpHysteresis = new Hysteresis(GESTURE_ACTIVATE, GESTURE_DEACTIVATE);

    /**
     * Null when there is no previous value to smooth from
     */
    private Vector3f smoothedAimOrigin;
    private Vector3f smoothedAimDirection;

    private Optional<HandGestureState> tamarinState = Optional.empty();
    private Optional<HandGestureState> runtimeState = Optional.empty();

    public HandGestureTracker(HandSide handSide){
        this.handSide = handSide;
    }

    /**
     * @param trackedHand the hand's data this frame, or empty if the hand isn't currently tracked
     * @param headPosition the head position in world coordinates (used to estimate an aim ray)
     * @param headRotation the head rotation in world coordinates (used to estimate an aim ray)
     * @param tpf time since the last update, in seconds
     */
    public void update(Optional<TrackedHand> trackedHand, Vector3f headPosition, Quaternion headRotation, float tpf){
        if (!trackedHand.isPresent()){
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
        Transform aim = smoothAim(JointGestureCalculations.approximateAim(joints, handSide, headPosition, headRotation), tpf);
        tamarinBuilder.aim(aim.getTranslation(), aim.getRotation());
        tamarinState = Optional.of(tamarinBuilder.build());

        runtimeState = trackedHand.get().getRuntimeAimState().map(runtimeAimState -> {
            HandGestureState.Builder runtimeBuilder = HandGestureState.builder(handSide, HandGestureSource.RUNTIME);
            addTamarinOnlyGestures(runtimeBuilder, joints);
            addRuntimePinchesAndAim(runtimeBuilder, runtimeAimState);
            return runtimeBuilder.build();
        });
    }

    /**
     * @param preference which source to use for the pinches and aim
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
        smoothedAimOrigin = null;
        smoothedAimDirection = null;
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
     * Like the runtime's, Tamarin's pinches are exclusive: at most one finger is pinching, and if it's ambiguous which
     * finger the thumb is pinching (e.g. all the fingers bunched together) then none are. The strengths are not
     * exclusive (they are just how close each finger is).
     */
    private void addTamarinPinches(HandGestureState.Builder builder, Map<HandJoint, BonePose> joints){
        float handScale = JointGestureCalculations.handScale(joints);
        Map<Finger, Float> scaledGaps = new EnumMap<>(Finger.class);
        Map<Finger, Float> strengths = new EnumMap<>(Finger.class);
        for(Finger finger : Finger.PINCHING_FINGERS){
            scaledGaps.put(finger, JointGestureCalculations.pinchGap(joints, finger) / handScale);
            strengths.put(finger, JointGestureCalculations.pinchStrength(joints, finger));
        }

        if (activePinch != null){
            boolean stillStrong = strengths.get(activePinch) >= PINCH_DEACTIVATE;
            boolean stillDistinct = separationFromOtherFingers(scaledGaps, activePinch) >= PINCH_RELEASE_SEPARATION;
            if (!stillStrong || !stillDistinct){
                activePinch = null;
            }
        } else{
            Finger closestFinger = Finger.INDEX;
            for(Finger finger : Finger.PINCHING_FINGERS){
                if (scaledGaps.get(finger) < scaledGaps.get(closestFinger)){
                    closestFinger = finger;
                }
            }
            boolean strong = strengths.get(closestFinger) >= PINCH_ACTIVATE;
            boolean distinct = separationFromOtherFingers(scaledGaps, closestFinger) >= PINCH_START_SEPARATION;
            if (strong && distinct){
                activePinch = closestFinger;
            }
        }

        for(Finger finger : Finger.PINCHING_FINGERS){
            builder.pinch(finger, strengths.get(finger), finger == activePinch);
        }
    }

    /**
     * How much closer the finger's tip is to the thumb than the next closest finger's tip (negative if another finger
     * is closer).
     */
    private static float separationFromOtherFingers(Map<Finger, Float> scaledGaps, Finger finger){
        float closestOtherGap = Float.MAX_VALUE;
        for(Finger other : Finger.PINCHING_FINGERS){
            if (other != finger){
                closestOtherGap = Math.min(closestOtherGap, scaledGaps.get(other));
            }
        }
        return closestOtherGap - scaledGaps.get(finger);
    }

    private static void addRuntimePinchesAndAim(HandGestureState.Builder builder, HandTrackingAimState runtimeAimState){
        for(Finger finger : Finger.PINCHING_FINGERS){
            builder.pinch(finger, runtimeAimState.getPinchStrength(finger), runtimeAimState.isPinching(finger));
        }
        if (runtimeAimState.isAimValid()){
            builder.aim(runtimeAimState.getAimPosition(), runtimeAimState.getAimOrientation());
        }
        builder.runtimeFlags(runtimeAimState.isSystemGestureActive(), runtimeAimState.isMenuPressed(), runtimeAimState.isDominantHand());
    }

    /**
     * Exponentially smooths the estimated aim ray, as joint positions jitter slightly frame to frame.
     */
    private Transform smoothAim(Transform rawAim, float tpf){
        Vector3f rawOrigin = rawAim.getTranslation();
        Vector3f rawDirection = rawAim.getRotation().mult(Vector3f.UNIT_Z.negate());
        if (smoothedAimOrigin == null || smoothedAimDirection == null){
            smoothedAimOrigin = rawOrigin.clone();
            smoothedAimDirection = rawDirection.clone();
        } else{
            float blend = 1 - FastMath.exp(-tpf / AIM_SMOOTHING_TIME);
            smoothedAimOrigin.interpolateLocal(rawOrigin, blend);
            smoothedAimDirection.interpolateLocal(rawDirection, blend).normalizeLocal();
        }
        return new Transform(smoothedAimOrigin.clone(), JointGestureCalculations.aimOrientationFromDirection(smoothedAimDirection));
    }
}

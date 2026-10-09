package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.EnumMap;
import java.util.Map;

/**
 * The raw facts about a hand's shape, measured from its joint positions (e.g. how curled each finger is, how far the
 * thumb is from each fingertip). No gesture rules are applied here, that is {@link HandGestureRules}'s job.
 * <p>
 *     All directions are relative to the stage, so "up" is the user's real up (regardless of how the observer is
 *     placed in the game world).
 * </p>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class HandJointMeasurements{

    /**
     * The total bend (summed over the finger's knuckles) of a fully curled finger, as in a fist. Bending more than
     * this still counts as a curl of 1.
     */
    static final float FULL_FINGER_BEND = 220 * FastMath.DEG_TO_RAD;
    /**
     * As {@link #FULL_FINGER_BEND} but for the thumb, which has one fewer knuckle and bends less.
     */
    static final float FULL_THUMB_BEND = 120 * FastMath.DEG_TO_RAD;
    /**
     * Thumb tip to fingertip distance (between joint centres, in metres) at or below which the pinch strength is 1.
     */
    static final float PINCH_CLOSED_DISTANCE = 0.02f;
    /**
     * Thumb tip to fingertip distance (in metres) at or above which the pinch strength is 0.
     */
    static final float PINCH_OPEN_DISTANCE = 0.07f;

    private final Map<HandJoint, BonePose> joints;
    private final EnumMap<Finger, Float> curls = new EnumMap<>(Finger.class);
    private final EnumMap<Finger, Float> pinchStrengths = new EnumMap<>(Finger.class);
    private final Vector3f thumbDirection;

    /**
     * @param stageJoints every hand joint, relative to the stage
     */
    public HandJointMeasurements(Map<HandJoint, BonePose> stageJoints){
        this.joints = stageJoints;
        for(Finger finger : Finger.values()){
            curls.put(finger, measureCurl(finger));
        }
        Vector3f thumbTip = position(HandJoint.THUMB_TIP_EXT);
        for(Finger finger : Finger.PINCHING_FINGERS){
            float distance = thumbTip.distance(position(finger.getTip()));
            pinchStrengths.put(finger, FastMath.clamp((PINCH_OPEN_DISTANCE - distance) / (PINCH_OPEN_DISTANCE - PINCH_CLOSED_DISTANCE), 0, 1));
        }
        thumbDirection = thumbTip.subtract(position(HandJoint.THUMB_PROXIMAL_EXT)).normalizeLocal();
    }

    /**
     * How curled the finger (or thumb) is. 0 is straight, 1 is fully curled (as in a fist). Measured from the angles
     * at the knuckles, so it doesn't depend on hand size.
     */
    public float getCurl(Finger finger){
        return curls.get(finger);
    }

    /**
     * How close the thumb tip is to the finger's tip. 0 is far apart, 1 is touching. This is just distance; it says
     * nothing about whether other fingers are also near the thumb.
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public float getPinchStrength(Finger finger){
        Finger.assertCanPinch(finger);
        return pinchStrengths.get(finger);
    }

    /**
     * A unit vector along the thumb, from its base towards its tip.
     */
    public Vector3f getThumbDirection(){
        return thumbDirection;
    }

    /**
     * Sums the angle at each knuckle (between consecutive bones) and scales so a full curl is 1.
     */
    private float measureCurl(Finger finger){
        HandJoint[] fingerJoints = finger.getJoints();
        float totalBend = 0;
        Vector3f previousBone = null;
        for(int i = 1; i < fingerJoints.length; i++){
            Vector3f bone = position(fingerJoints[i]).subtract(position(fingerJoints[i - 1])).normalizeLocal();
            if (previousBone != null){
                // FastMath.acos clamps, so float error on a straight finger can't produce NaN
                totalBend += FastMath.acos(previousBone.dot(bone));
            }
            previousBone = bone;
        }
        float fullBend = finger == Finger.THUMB ? FULL_THUMB_BEND : FULL_FINGER_BEND;
        return FastMath.clamp(totalBend / fullBend, 0, 1);
    }

    private Vector3f position(HandJoint joint){
        return joints.get(joint).position();
    }
}

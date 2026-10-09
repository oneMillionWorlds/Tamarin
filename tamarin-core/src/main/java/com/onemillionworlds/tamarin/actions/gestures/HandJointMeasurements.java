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
     * The distance from the wrist to the middle finger knuckle on an average adult hand. Distances are scaled by the
     * actual hand's size relative to this, so gestures work the same for large and small hands.
     */
    static final float REFERENCE_HAND_SIZE = 0.095f;
    /**
     * The gap between the skin of the thumb tip and the fingertip (at the reference hand size) that counts as a full
     * pinch. Not zero because tracking is rarely good enough to see the skin actually touching.
     */
    static final float PINCH_FULL_GAP = 0.005f;
    /**
     * The gap between the skin of the thumb tip and the fingertip (at the reference hand size) at which the pinch
     * strength drops to zero.
     */
    static final float PINCH_ZERO_GAP = 0.045f;
    /**
     * How much closer (at the reference hand size) the thumb tip must be to the finger's side (its proximal and
     * intermediate bones) than to its tip for the contact to count as entirely "side" (no pinch at all).
     */
    static final float TIP_CONTACT_ZERO = -0.012f;
    /**
     * The thumb tip being at least as close to the fingertip as to the finger's side counts as entirely "tip" (a
     * normal pinch).
     */
    static final float TIP_CONTACT_FULL = 0f;

    /**
     * How close (at the reference hand size) the thumb tip must be to the middle of the index or middle finger to
     * count as fully tucked around them (as in a fist).
     */
    static final float THUMB_TUCKED_FULL_DISTANCE = 0.03f;
    /**
     * How far (at the reference hand size) the thumb tip must be from the middle of the index and middle fingers to
     * count as not tucked at all.
     */
    static final float THUMB_TUCKED_ZERO_DISTANCE = 0.05f;

    private static final HandJoint[] THUMB_TUCK_TARGETS = new HandJoint[]{
            HandJoint.INDEX_INTERMEDIATE_EXT, HandJoint.INDEX_DISTAL_EXT, HandJoint.MIDDLE_INTERMEDIATE_EXT, HandJoint.MIDDLE_DISTAL_EXT
    };

    private final Map<HandJoint, BonePose> joints;
    private final float handScale;
    private final float thumbTucked;
    private final EnumMap<Finger, Float> curls = new EnumMap<>(Finger.class);
    private final EnumMap<Finger, Float> pinchStrengths = new EnumMap<>(Finger.class);
    private final Vector3f thumbDirection;

    /**
     * @param stageJoints every hand joint, relative to the stage
     */
    public HandJointMeasurements(Map<HandJoint, BonePose> stageJoints){
        this.joints = stageJoints;
        float handSize = position(HandJoint.WRIST_EXT).distance(position(HandJoint.MIDDLE_PROXIMAL_EXT));
        // clamped so that a tracking glitch can't make the hand absurdly big or small
        this.handScale = FastMath.clamp(handSize / REFERENCE_HAND_SIZE, 0.5f, 2f);
        for(Finger finger : Finger.values()){
            curls.put(finger, measureCurl(finger));
        }
        for(Finger finger : Finger.PINCHING_FINGERS){
            pinchStrengths.put(finger, measurePinch(finger));
        }
        thumbDirection = position(HandJoint.THUMB_TIP_EXT).subtract(position(HandJoint.THUMB_PROXIMAL_EXT)).normalizeLocal();
        thumbTucked = measureThumbTucked();
    }

    /**
     * How much the thumb tip is tucked against the middle of the index and middle fingers (as it is in a fist). 0 is
     * not at all, 1 is fully.
     */
    public float getThumbTucked(){
        return thumbTucked;
    }

    /**
     * How big this hand is relative to an average adult hand (1 is average).
     */
    public float getHandScale(){
        return handScale;
    }

    /**
     * How curled the finger (or thumb) is. 0 is straight, 1 is fully curled (as in a fist). Measured from the angles
     * at the knuckles, so it doesn't depend on hand size.
     */
    public float getCurl(Finger finger){
        return curls.get(finger);
    }

    /**
     * How close the thumb tip is to the finger's tip. 0 is far apart, 1 is touching. This says nothing about whether
     * other fingers are also near the thumb.
     * <p>
     *     Only the thumb meeting the tip end of the finger counts. The thumb touching the side of the finger further
     *     down (as it does in Meta's thumb microgestures, which slide the thumb along the side of the index finger) is
     *     not a pinch, even if the thumb tip happens to be close to the fingertip.
     * </p>
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

    /**
     * The skin to skin gap between the thumb tip and fingertip (scaled by hand size), times how much the thumb is at
     * the tip of the finger rather than its side.
     */
    private float measurePinch(Finger finger){
        BonePose thumbTip = joints.get(HandJoint.THUMB_TIP_EXT);
        BonePose fingerTip = joints.get(finger.getTip());
        float gap = thumbTip.position().distance(fingerTip.position()) - thumbTip.radius() - fingerTip.radius();
        float closeness = 1 - inverseLerpClamped(gap / handScale, PINCH_FULL_GAP, PINCH_ZERO_GAP);
        return closeness * measureTipContact(finger);
    }

    /**
     * Whether the thumb tip is nearer the tip of the finger (1) or the side of the finger further down, i.e. its
     * proximal and intermediate bones (0).
     */
    private float measureTipContact(Finger finger){
        HandJoint[] fingerJoints = finger.getJoints();
        Vector3f thumbTip = position(HandJoint.THUMB_TIP_EXT);
        float tipDistance = thumbTip.distance(position(finger.getTip()));

        // the proximal and intermediate bones: knuckle -> intermediate joint -> distal joint
        float sideDistance = Math.min(
                distanceToSegment(thumbTip, position(fingerJoints[1]), position(fingerJoints[2])),
                distanceToSegment(thumbTip, position(fingerJoints[2]), position(fingerJoints[3])));

        // positive when the thumb is nearer the tip than the side of the finger
        float tipCloserBy = (sideDistance - tipDistance) / handScale;
        return inverseLerpClamped(tipCloserBy, TIP_CONTACT_ZERO, TIP_CONTACT_FULL);
    }

    private float measureThumbTucked(){
        Vector3f thumbTip = position(HandJoint.THUMB_TIP_EXT);
        float closestDistance = Float.MAX_VALUE;
        for(HandJoint target : THUMB_TUCK_TARGETS){
            closestDistance = Math.min(closestDistance, thumbTip.distance(position(target)));
        }
        return 1 - inverseLerpClamped(closestDistance / handScale, THUMB_TUCKED_FULL_DISTANCE, THUMB_TUCKED_ZERO_DISTANCE);
    }

    /**
     * The shortest distance from the point to the line segment from a to b.
     */
    static float distanceToSegment(Vector3f point, Vector3f a, Vector3f b){
        Vector3f ab = b.subtract(a);
        float lengthSquared = ab.lengthSquared();
        if (lengthSquared == 0){
            return point.distance(a);
        }
        float t = FastMath.clamp(point.subtract(a).dot(ab) / lengthSquared, 0, 1);
        return point.distance(a.add(ab.multLocal(t)));
    }

    /**
     * 0 at (or before) from, 1 at (or after) to. From may be greater than to.
     */
    static float inverseLerpClamped(float value, float from, float to){
        return FastMath.clamp((value - from) / (to - from), 0, 1);
    }

    private Vector3f position(HandJoint joint){
        return joints.get(joint).position();
    }
}

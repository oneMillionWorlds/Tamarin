package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Map;

/**
 * Tamarin's own gesture recognition, worked out purely from the positions of the hand's joints (bones). Every method
 * returns a "strength" from 0 (not doing the gesture at all) to 1 (definitely doing it).
 * <p>
 *     All the joint maps must contain every {@link HandJoint}, and all the positions must be in the same coordinate
 *     system. Most calculations only use distances and angles between joints so any coordinate system works, but
 *     {@link #thumbsUpStrength}, {@link #palmNormal} and {@link #approximateAim} need world coordinates (because "up"
 *     and the head position matter).
 * </p>
 * <p>
 *     Distances are scaled by the size of the hand, so they work the same for large and small hands.
 * </p>
 * <p>
 *     Most applications should use {@link HandGestureState} (via
 *     {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getHandGestureState}) rather than calling
 *     these directly, it adds hysteresis so the gestures don't flicker on and off.
 * </p>
 */
public final class JointGestureCalculations{

    /**
     * The distance from the wrist to the middle finger knuckle on an average adult hand. Distances are scaled by
     * the actual hand's size relative to this.
     */
    static final float REFERENCE_HAND_SIZE = 0.095f;

    /**
     * The gap between the skin of the thumb tip and the finger tip (at the reference hand size) that counts as a full pinch.
     * Not zero because tracking is rarely good enough to see the skin actually touching.
     */
    static final float PINCH_FULL_GAP = 0.005f;
    /**
     * The gap between the skin of the thumb tip and the finger tip (at the reference hand size) at which the pinch
     * strength drops to zero.
     */
    static final float PINCH_ZERO_GAP = 0.045f;

    /**
     * How much closer (at the reference hand size) the thumb tip must be to the finger's side (its proximal and
     * intermediate bones) than to its tip for the contact to count as entirely "side" (no pinch at all).
     */
    static final float TIP_CONTACT_ZERO = -0.012f;
    /**
     * The thumb tip being at least as close to the finger tip as to the finger's side counts as entirely "tip" (a
     * normal pinch).
     */
    static final float TIP_CONTACT_FULL = 0f;

    /**
     * The total bend (across the 3 knuckles) of a finger that still counts as fully straight
     */
    static final float FINGER_STRAIGHT_DEGREES = 25;
    /**
     * The total bend (across the 3 knuckles) of a finger that counts as fully curled
     */
    static final float FINGER_CURLED_DEGREES = 200;

    static final float THUMB_STRAIGHT_DEGREES = 15;
    static final float THUMB_CURLED_DEGREES = 100;

    /**
     * How close (at the reference hand size) the thumb tip must be to the middle of the index or middle finger to
     * count as fully wrapped around them (as in a fist)
     */
    static final float THUMB_WRAPPED_FULL_DISTANCE = 0.03f;
    static final float THUMB_WRAPPED_ZERO_DISTANCE = 0.05f;

    /**
     * The cosine of the angle between the thumb and straight up at which the thumb counts as fully up (about 37 degrees)
     */
    static final float THUMB_UP_FULL_COSINE = 0.8f;
    /**
     * The cosine of the angle between the thumb and straight up at which the thumb no longer counts as up at all (about 66 degrees)
     */
    static final float THUMB_UP_ZERO_COSINE = 0.4f;

    /**
     * Roughly how far below the eyes the shoulders are, used to estimate an aim ray
     */
    static final float SHOULDER_DROP = 0.15f;
    /**
     * Roughly how far to the side of the centre of the head the shoulders are, used to estimate an aim ray
     */
    static final float SHOULDER_HALF_WIDTH = 0.15f;

    private static final HandJoint[] THUMB_WRAP_TARGETS = new HandJoint[]{
            HandJoint.INDEX_INTERMEDIATE_EXT, HandJoint.INDEX_DISTAL_EXT, HandJoint.MIDDLE_INTERMEDIATE_EXT, HandJoint.MIDDLE_DISTAL_EXT
    };

    private JointGestureCalculations(){}

    /**
     * How big this hand is relative to an average adult hand (1 is average).
     */
    public static float handScale(Map<HandJoint, BonePose> joints){
        float size = position(joints, HandJoint.WRIST_EXT).distance(position(joints, HandJoint.MIDDLE_PROXIMAL_EXT));
        // clamped so that a tracking glitch can't make the hand absurdly big or small
        return FastMath.clamp(size / REFERENCE_HAND_SIZE, 0.5f, 2f);
    }

    /**
     * The gap between the skin of the thumb tip and the skin of the finger tip (in metres, not scaled by hand size).
     * May be negative if the tracked joints overlap.
     */
    public static float pinchGap(Map<HandJoint, BonePose> joints, Finger finger){
        Finger.assertCanPinch(finger);
        BonePose thumbTip = joints.get(HandJoint.THUMB_TIP_EXT);
        BonePose fingerTip = joints.get(finger.getTip());
        return thumbTip.position().distance(fingerTip.position()) - thumbTip.radius() - fingerTip.radius();
    }

    /**
     * How strongly the thumb is pinching the given finger. 1 is touching (or near enough), 0 is apart.
     * <p>
     *     Only the thumb meeting the tip end of the finger counts. The thumb touching the side of the finger further
     *     down (as it does in Meta's thumb microgestures, which slide the thumb along the side of the index finger)
     *     is not a pinch, even if the thumb tip happens to be close to the finger tip. See {@link #tipContact}.
     * </p>
     */
    public static float pinchStrength(Map<HandJoint, BonePose> joints, Finger finger){
        float scaledGap = pinchGap(joints, finger) / handScale(joints);
        return (1 - inverseLerpClamped(scaledGap, PINCH_FULL_GAP, PINCH_ZERO_GAP)) * tipContact(joints, finger);
    }

    /**
     * Whether the thumb tip is nearer the tip of the finger (1) or the side of the finger further down, i.e. its
     * proximal and intermediate bones (0). Used to tell a pinch (thumb meets finger tip) from the thumb touching or
     * sliding along the side of the finger.
     */
    public static float tipContact(Map<HandJoint, BonePose> joints, Finger finger){
        Finger.assertCanPinch(finger);
        HandJoint[] fingerJoints = finger.getJoints();
        Vector3f thumbTip = position(joints, HandJoint.THUMB_TIP_EXT);
        float tipDistance = thumbTip.distance(position(joints, finger.getTip()));

        // the proximal and intermediate bones: knuckle -> intermediate joint -> distal joint
        float sideDistance = Math.min(
                distanceToSegment(thumbTip, position(joints, fingerJoints[1]), position(joints, fingerJoints[2])),
                distanceToSegment(thumbTip, position(joints, fingerJoints[2]), position(joints, fingerJoints[3])));

        // positive when the thumb is nearer the tip than the side of the finger
        float tipCloserBy = (sideDistance - tipDistance) / handScale(joints);
        return inverseLerpClamped(tipCloserBy, TIP_CONTACT_ZERO, TIP_CONTACT_FULL);
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
     * The finger (not thumb) whose tip is currently closest to the thumb tip.
     */
    public static Finger closestFingerToThumb(Map<HandJoint, BonePose> joints){
        Finger closest = Finger.INDEX;
        float closestGap = Float.MAX_VALUE;
        for(Finger finger : Finger.PINCHING_FINGERS){
            float gap = pinchGap(joints, finger);
            if (gap < closestGap){
                closestGap = gap;
                closest = finger;
            }
        }
        return closest;
    }

    /**
     * How curled the finger is. 0 is straight, 1 is fully curled (as in a fist). This is worked out from the bend at
     * each of the finger's knuckles.
     */
    public static float fingerCurl(Map<HandJoint, BonePose> joints, Finger finger){
        float totalBendDegrees = totalBendDegrees(joints, finger.getJoints());
        if (finger == Finger.THUMB){
            return inverseLerpClamped(totalBendDegrees, THUMB_STRAIGHT_DEGREES, THUMB_CURLED_DEGREES);
        }
        return inverseLerpClamped(totalBendDegrees, FINGER_STRAIGHT_DEGREES, FINGER_CURLED_DEGREES);
    }

    /**
     * How much the thumb tip is tucked against the middle of the index and middle fingers (as it is in a fist).
     */
    public static float thumbWrappedStrength(Map<HandJoint, BonePose> joints){
        Vector3f thumbTip = position(joints, HandJoint.THUMB_TIP_EXT);
        float closestDistance = Float.MAX_VALUE;
        for(HandJoint target : THUMB_WRAP_TARGETS){
            closestDistance = Math.min(closestDistance, thumbTip.distance(position(joints, target)));
        }
        float scaledDistance = closestDistance / handScale(joints);
        return 1 - inverseLerpClamped(scaledDistance, THUMB_WRAPPED_FULL_DISTANCE, THUMB_WRAPPED_ZERO_DISTANCE);
    }

    /**
     * A closed fist: all four fingers curled with the thumb wrapped around them.
     */
    public static float fistStrength(Map<HandJoint, BonePose> joints){
        return Math.min(fourFingersCurled(joints), thumbWrappedStrength(joints));
    }

    /**
     * Pointing: the index finger straight and the middle, ring and little fingers curled. The thumb is ignored.
     */
    public static float pointStrength(Map<HandJoint, BonePose> joints){
        float indexStraight = 1 - fingerCurl(joints, Finger.INDEX);
        float othersCurled = Math.min(fingerCurl(joints, Finger.MIDDLE), Math.min(fingerCurl(joints, Finger.RING), fingerCurl(joints, Finger.LITTLE)));
        return Math.min(indexStraight, othersCurled);
    }

    /**
     * A thumbs up: all four fingers curled, the thumb straight and not tucked in, and the thumb pointing roughly up (in
     * world coordinates, so this needs world coordinate joints).
     */
    public static float thumbsUpStrength(Map<HandJoint, BonePose> joints){
        float thumbStraight = 1 - fingerCurl(joints, Finger.THUMB);
        float thumbNotWrapped = 1 - thumbWrappedStrength(joints);

        Vector3f thumbDirection = position(joints, HandJoint.THUMB_TIP_EXT).subtract(position(joints, HandJoint.THUMB_PROXIMAL_EXT));
        float thumbUp = 0;
        if (thumbDirection.lengthSquared() > 0){
            thumbUp = inverseLerpClamped(thumbDirection.normalizeLocal().dot(Vector3f.UNIT_Y), THUMB_UP_ZERO_COSINE, THUMB_UP_FULL_COSINE);
        }
        return Math.min(Math.min(fourFingersCurled(joints), thumbStraight), Math.min(thumbNotWrapped, thumbUp));
    }

    /**
     * The direction the palm is facing (a unit vector pointing out of the palm, away from the back of the hand).
     * <p>
     *     This uses the OpenXR convention that a hand joint's +Y axis points out of the back of the hand.
     * </p>
     */
    public static Vector3f palmNormal(Map<HandJoint, BonePose> joints){
        return joints.get(HandJoint.PALM_EXT).orientation().mult(Vector3f.UNIT_Y.negate()).normalizeLocal();
    }

    /**
     * Estimates a pointing (aim) ray for the hand, in the same way that runtimes typically do: a ray from (an
     * estimate of) the shoulder through the base of the index finger. This is much more stable than using the
     * direction of the finger itself (which moves when the user pinches to "click").
     *
     * @param joints the hand's joints, in world coordinates
     * @param handSide which hand it is (to estimate which shoulder to use)
     * @param headPosition the position of the head (in world coordinates)
     * @param headRotation the rotation of the head (in world coordinates, using JME's camera convention that
     *                     rotation * UNIT_X is to the head's left)
     * @return the origin of the ray (translation) and its orientation (rotation, with the ray along its -Z axis, the
     *         same convention as OpenXR aim poses)
     */
    public static Transform approximateAim(Map<HandJoint, BonePose> joints, HandSide handSide, Vector3f headPosition, Quaternion headRotation){
        Vector3f headLeft = headRotation.mult(Vector3f.UNIT_X);
        headLeft.y = 0;
        if (headLeft.lengthSquared() < 0.0001f){
            // looking straight up or down, any sideways direction will do
            headLeft.set(Vector3f.UNIT_X);
        }
        headLeft.normalizeLocal();
        Vector3f outwards = handSide == HandSide.LEFT ? headLeft : headLeft.negate();
        Vector3f shoulder = headPosition.add(0, -SHOULDER_DROP, 0).addLocal(outwards.multLocal(SHOULDER_HALF_WIDTH));

        Vector3f origin = position(joints, HandJoint.INDEX_PROXIMAL_EXT).add(position(joints, HandJoint.THUMB_PROXIMAL_EXT)).multLocal(0.5f);
        Vector3f direction = origin.subtract(shoulder);
        if (direction.lengthSquared() < 0.000001f){
            direction.set(Vector3f.UNIT_Z.negate());
        }
        direction.normalizeLocal();
        return new Transform(origin, aimOrientationFromDirection(direction));
    }

    /**
     * An orientation whose -Z axis points along the direction (the OpenXR aim pose convention), with its Y axis as
     * close to world up as possible.
     */
    static Quaternion aimOrientationFromDirection(Vector3f direction){
        Vector3f up = Math.abs(direction.dot(Vector3f.UNIT_Y)) > 0.99f ? Vector3f.UNIT_Z : Vector3f.UNIT_Y;
        Quaternion orientation = new Quaternion();
        // lookAt points +Z along the given direction, so give it the opposite direction to put -Z along the ray
        orientation.lookAt(direction.negate(), up);
        return orientation;
    }

    private static float fourFingersCurled(Map<HandJoint, BonePose> joints){
        float minimumCurl = 1;
        for(Finger finger : Finger.PINCHING_FINGERS){
            minimumCurl = Math.min(minimumCurl, fingerCurl(joints, finger));
        }
        return minimumCurl;
    }

    /**
     * The sum of the angles between each consecutive pair of bones in the chain of joints.
     */
    private static float totalBendDegrees(Map<HandJoint, BonePose> joints, HandJoint[] chain){
        float totalRadians = 0;
        Vector3f previousBone = null;
        for(int i = 0; i < chain.length - 1; i++){
            Vector3f bone = position(joints, chain[i + 1]).subtract(position(joints, chain[i]));
            if (bone.lengthSquared() == 0){
                continue;
            }
            bone.normalizeLocal();
            if (previousBone != null){
                totalRadians += FastMath.acos(FastMath.clamp(previousBone.dot(bone), -1, 1));
            }
            previousBone = bone;
        }
        return totalRadians * FastMath.RAD_TO_DEG;
    }

    private static Vector3f position(Map<HandJoint, BonePose> joints, HandJoint joint){
        return joints.get(joint).position();
    }

    /**
     * 0 at (or before) from, 1 at (or after) to. From may be greater than to.
     */
    static float inverseLerpClamped(float value, float from, float to){
        if (from == to){
            return value >= to ? 1 : 0;
        }
        return FastMath.clamp((value - from) / (to - from), 0, 1);
    }
}

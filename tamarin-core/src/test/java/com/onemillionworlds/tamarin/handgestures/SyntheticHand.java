package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds simple, roughly realistic hand skeletons for testing gesture recognition.
 * <p>
 *     The hand is a right hand, palm facing down (-Y), fingers pointing forward (-Z), thumb on the -X side. The wrist
 *     is at the origin and the hand is exactly the reference size (wrist to middle knuckle is 9.5cm).
 * </p>
 */
class SyntheticHand{

    static final float TIP_RADIUS = 0.007f;
    static final float JOINT_RADIUS = 0.01f;

    private static final float[] SEGMENT_LENGTHS = new float[]{0.04f, 0.025f, 0.02f};

    private final Map<HandJoint, Vector3f> positions = new EnumMap<>(HandJoint.class);

    private SyntheticHand(){
        positions.put(HandJoint.WRIST_EXT, new Vector3f(0, 0, 0));
        positions.put(HandJoint.PALM_EXT, new Vector3f(0.01f, 0, -0.05f));

        placeFinger(Finger.INDEX, new Vector3f(-0.02f, 0, -0.01f), new Vector3f(-0.022f, 0, -0.09f));
        placeFinger(Finger.MIDDLE, new Vector3f(0, 0, -0.01f), new Vector3f(0, 0, -0.095f));
        placeFinger(Finger.RING, new Vector3f(0.02f, 0, -0.01f), new Vector3f(0.02f, 0, -0.088f));
        placeFinger(Finger.LITTLE, new Vector3f(0.035f, 0, -0.01f), new Vector3f(0.038f, 0, -0.08f));
        for(Finger finger : Finger.PINCHING_FINGERS){
            curlFinger(finger, 0);
        }
        pointThumb(new Vector3f(-0.5f, 0, -1));
    }

    private void placeFinger(Finger finger, Vector3f metacarpal, Vector3f knuckle){
        HandJoint[] joints = finger.getJoints();
        positions.put(joints[0], metacarpal);
        positions.put(joints[1], knuckle);
    }

    static SyntheticHand openRightHand(){
        return new SyntheticHand();
    }

    /**
     * Bends each of the finger's 3 knuckles by the given angle (towards the palm).
     */
    SyntheticHand curlFinger(Finger finger, float degreesPerKnuckle){
        HandJoint[] joints = finger.getJoints();
        Vector3f position = positions.get(joints[1]).clone(); // the knuckle (proximal joint)
        Vector3f direction = new Vector3f(0, 0, -1);
        Quaternion bend = new Quaternion().fromAngleAxis(-degreesPerKnuckle * FastMath.DEG_TO_RAD, Vector3f.UNIT_X);
        for(int i = 0; i < 3; i++){
            direction = bend.mult(direction);
            position = position.add(direction.mult(SEGMENT_LENGTHS[i]));
            positions.put(joints[i + 2], position);
        }
        return this;
    }

    SyntheticHand curlAllFingers(float degreesPerKnuckle){
        for(Finger finger : Finger.PINCHING_FINGERS){
            curlFinger(finger, degreesPerKnuckle);
        }
        return this;
    }

    /**
     * Makes the thumb a straight line, from its metacarpal, in the given direction.
     */
    SyntheticHand pointThumb(Vector3f direction){
        Vector3f unitDirection = direction.normalize();
        Vector3f position = new Vector3f(-0.025f, 0, -0.02f);
        positions.put(HandJoint.THUMB_METACARPAL_EXT, position);
        position = position.add(unitDirection.mult(0.03f));
        positions.put(HandJoint.THUMB_PROXIMAL_EXT, position);
        position = position.add(unitDirection.mult(0.03f));
        positions.put(HandJoint.THUMB_DISTAL_EXT, position);
        position = position.add(unitDirection.mult(0.025f));
        positions.put(HandJoint.THUMB_TIP_EXT, position);
        return this;
    }

    /**
     * Moves just the thumb tip so that the gap between its skin and the finger tip's skin is the given amount.
     */
    SyntheticHand thumbTipGapToFingerTip(Finger finger, float skinGap){
        Vector3f fingerTip = positions.get(finger.getTip());
        positions.put(HandJoint.THUMB_TIP_EXT, fingerTip.add(0, -(skinGap + 2 * TIP_RADIUS), 0));
        return this;
    }

    SyntheticHand thumbTipAt(Vector3f position){
        positions.put(HandJoint.THUMB_TIP_EXT, position.clone());
        return this;
    }

    /**
     * Moves just the thumb tip to the given distance below a joint.
     */
    SyntheticHand thumbTipNear(HandJoint joint, float distance){
        positions.put(HandJoint.THUMB_TIP_EXT, positions.get(joint).add(0, -distance, 0));
        return this;
    }

    Map<HandJoint, BonePose> build(){
        Map<HandJoint, BonePose> joints = new EnumMap<>(HandJoint.class);
        for(HandJoint joint : HandJoint.values()){
            boolean isTip = joint.name().endsWith("_TIP_EXT");
            joints.put(joint, new BonePose(positions.get(joint).clone(), new Quaternion(), isTip ? TIP_RADIUS : JOINT_RADIUS));
        }
        return joints;
    }
}

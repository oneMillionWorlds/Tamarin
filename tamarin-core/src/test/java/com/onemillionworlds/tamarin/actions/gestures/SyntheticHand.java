package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds hand joints for tests. The palm is at the origin facing down (-Y), with the fingers pointing forward (-Z).
 * Each finger is a chain of 4cm bones that bends by the same angle at every knuckle.
 */
class SyntheticHand{

    private static final float BONE_LENGTH = 0.04f;

    private final Map<HandJoint, BonePose> joints = new EnumMap<>(HandJoint.class);

    private SyntheticHand(){
        joints.put(HandJoint.PALM_EXT, pose(new Vector3f()));
        joints.put(HandJoint.WRIST_EXT, pose(new Vector3f(0, 0, 0.05f)));
        for(Finger finger : Finger.PINCHING_FINGERS){
            curlFinger(finger, 0);
        }
        thumb(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, 0);
    }

    /**
     * All fingers and the thumb straight, the thumb pointing out sideways.
     */
    static SyntheticHand openHand(){
        return new SyntheticHand();
    }

    /**
     * @param bendPerKnuckle degrees, towards the palm
     */
    SyntheticHand curlFinger(Finger finger, float bendPerKnuckle){
        Vector3f base = new Vector3f(-0.03f + 0.02f * (finger.ordinal() - 1), 0, 0);
        return chain(finger, base, new Vector3f(0, 0, -1), Vector3f.UNIT_X.negate(), bendPerKnuckle);
    }

    SyntheticHand curlAllFingers(float bendPerKnuckle){
        for(Finger finger : Finger.PINCHING_FINGERS){
            curlFinger(finger, bendPerKnuckle);
        }
        return this;
    }

    /**
     * @param direction the direction of the thumb's first bone
     * @param bendAxis the axis the thumb bends around
     * @param bendPerKnuckle degrees
     */
    SyntheticHand thumb(Vector3f direction, Vector3f bendAxis, float bendPerKnuckle){
        return chain(Finger.THUMB, new Vector3f(-0.05f, 0, 0.02f), direction, bendAxis, bendPerKnuckle);
    }

    /**
     * Moves just the fingertip so it is the given distance from the thumb tip.
     */
    SyntheticHand fingerTipNearThumbTip(Finger finger, float distance){
        Vector3f thumbTip = joints.get(HandJoint.THUMB_TIP_EXT).position();
        joints.put(finger.getTip(), pose(thumbTip.add(distance, 0, 0)));
        return this;
    }

    Map<HandJoint, BonePose> joints(){
        return new EnumMap<>(joints);
    }

    private SyntheticHand chain(Finger finger, Vector3f base, Vector3f direction, Vector3f bendAxis, float bendPerKnuckle){
        Quaternion bend = new Quaternion().fromAngleAxis(bendPerKnuckle * FastMath.DEG_TO_RAD, bendAxis);
        Vector3f position = base.clone();
        Vector3f boneDirection = direction.normalize();
        HandJoint[] fingerJoints = finger.getJoints();
        for(int i = 0; i < fingerJoints.length; i++){
            joints.put(fingerJoints[i], pose(position.clone()));
            if (i > 0){
                boneDirection = bend.mult(boneDirection);
            }
            position.addLocal(boneDirection.mult(BONE_LENGTH));
        }
        return this;
    }

    private static BonePose pose(Vector3f position){
        return new BonePose(position, new Quaternion(), 0.01f);
    }
}

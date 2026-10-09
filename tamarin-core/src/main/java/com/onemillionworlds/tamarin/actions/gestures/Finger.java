package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.handskeleton.HandJoint;

/**
 * The fingers (and thumb) of a hand.
 */
public enum Finger {
    THUMB(HandJoint.THUMB_METACARPAL_EXT, HandJoint.THUMB_PROXIMAL_EXT, HandJoint.THUMB_DISTAL_EXT, HandJoint.THUMB_TIP_EXT),
    INDEX(HandJoint.INDEX_METACARPAL_EXT, HandJoint.INDEX_PROXIMAL_EXT, HandJoint.INDEX_INTERMEDIATE_EXT, HandJoint.INDEX_DISTAL_EXT, HandJoint.INDEX_TIP_EXT),
    MIDDLE(HandJoint.MIDDLE_METACARPAL_EXT, HandJoint.MIDDLE_PROXIMAL_EXT, HandJoint.MIDDLE_INTERMEDIATE_EXT, HandJoint.MIDDLE_DISTAL_EXT, HandJoint.MIDDLE_TIP_EXT),
    RING(HandJoint.RING_METACARPAL_EXT, HandJoint.RING_PROXIMAL_EXT, HandJoint.RING_INTERMEDIATE_EXT, HandJoint.RING_DISTAL_EXT, HandJoint.RING_TIP_EXT),
    LITTLE(HandJoint.LITTLE_METACARPAL_EXT, HandJoint.LITTLE_PROXIMAL_EXT, HandJoint.LITTLE_INTERMEDIATE_EXT, HandJoint.LITTLE_DISTAL_EXT, HandJoint.LITTLE_TIP_EXT);

    /**
     * The fingers that can pinch against the thumb (i.e. all of them except the thumb itself).
     */
    public static final Finger[] PINCHING_FINGERS = new Finger[]{INDEX, MIDDLE, RING, LITTLE};

    private final HandJoint[] joints;

    Finger(HandJoint... joints){
        this.joints = joints;
    }

    /**
     * The joints of this finger, in order from the one nearest the wrist to the tip. Note that the thumb has one
     * fewer joint than the other fingers (it has no intermediate phalanx).
     */
    public HandJoint[] getJoints(){
        return joints.clone();
    }

    public HandJoint getTip(){
        return joints[joints.length - 1];
    }

    /**
     * Throws if the finger is the thumb (the thumb can't pinch against itself).
     */
    static void assertCanPinch(Finger finger){
        if (finger == THUMB){
            throw new IllegalArgumentException("The thumb can't pinch against itself, use INDEX, MIDDLE, RING or LITTLE");
        }
    }
}

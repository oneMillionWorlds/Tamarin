package com.onemillionworlds.tamarin.handskeleton;


import org.lwjgl.openxr.EXTHandTracking;

public enum HandJoint {
    /**
     * The palm is the central area of the hand, between the fingers and the wrist. This is the root of the hand skeleton.
     */
    PALM_EXT(EXTHandTracking.XR_HAND_JOINT_PALM_EXT),

    /**
     * The wrist is the joint connecting the hand with the forearm.
     */
    WRIST_EXT(EXTHandTracking.XR_HAND_JOINT_WRIST_EXT),

    /**
     * The metacarpal of the thumb is the bone within the thumb, closest to the wrist.
     */
    THUMB_METACARPAL_EXT(EXTHandTracking.XR_HAND_JOINT_THUMB_METACARPAL_EXT),

    /**
     * The proximal phalanx of the thumb is the first bone segment from the metacarpal towards the tip of the thumb.
     */
    THUMB_PROXIMAL_EXT(EXTHandTracking.XR_HAND_JOINT_THUMB_PROXIMAL_EXT),

    /**
     * The distal phalanx of the thumb is the bone segment closest to the tip of the thumb.
     */
    THUMB_DISTAL_EXT(EXTHandTracking.XR_HAND_JOINT_THUMB_DISTAL_EXT),

    /**
     * The tip of the thumb is the very end of the thumb, beyond the distal phalanx.
     */
    THUMB_TIP_EXT(EXTHandTracking.XR_HAND_JOINT_THUMB_TIP_EXT),

    /**
     * The metacarpal of the index finger is the bone within the index finger, closest to the wrist.
     */
    INDEX_METACARPAL_EXT(EXTHandTracking.XR_HAND_JOINT_INDEX_METACARPAL_EXT),

    /**
     * The proximal phalanx of the index finger is the first bone segment from the metacarpal towards the tip of the index finger.
     */
    INDEX_PROXIMAL_EXT(EXTHandTracking.XR_HAND_JOINT_INDEX_PROXIMAL_EXT),

    /**
     * The intermediate phalanx of the index finger is the bone segment in the middle of the index finger, between the proximal and distal phalanges.
     */
    INDEX_INTERMEDIATE_EXT(EXTHandTracking.XR_HAND_JOINT_INDEX_INTERMEDIATE_EXT),

    /**
     * The distal phalanx of the index finger is the bone segment closest to the tip of the index finger.
     */
    INDEX_DISTAL_EXT(EXTHandTracking.XR_HAND_JOINT_INDEX_DISTAL_EXT),

    /**
     * The tip of the index finger is the very end of the finger, beyond the distal phalanx.
     */
    INDEX_TIP_EXT(EXTHandTracking.XR_HAND_JOINT_INDEX_TIP_EXT),

    /**
     * The metacarpal of the middle finger is the bone within the middle finger, closest to the wrist.
     */
    MIDDLE_METACARPAL_EXT(EXTHandTracking.XR_HAND_JOINT_MIDDLE_METACARPAL_EXT),

    /**
     * The proximal phalanx of the middle finger is the first bone segment from the metacarpal towards the tip of the middle finger.
     */
    MIDDLE_PROXIMAL_EXT(EXTHandTracking.XR_HAND_JOINT_MIDDLE_PROXIMAL_EXT),

    /**
     * The intermediate phalanx of the middle finger is the bone segment in the middle of the middle finger, between the proximal and distal phalanges.
     */
    MIDDLE_INTERMEDIATE_EXT(EXTHandTracking.XR_HAND_JOINT_MIDDLE_INTERMEDIATE_EXT),

    /**
     * The distal phalanx of the middle finger is the bone segment closest to the tip of the middle finger.
     */
    MIDDLE_DISTAL_EXT(EXTHandTracking.XR_HAND_JOINT_MIDDLE_DISTAL_EXT),

    /**
     * The tip of the middle finger is the very end of the finger, beyond the distal phalanx.
     */
    MIDDLE_TIP_EXT(EXTHandTracking.XR_HAND_JOINT_MIDDLE_TIP_EXT),

    /**
     * The metacarpal of the ring finger is the bone within the ring finger, closest to the wrist.
     */
    RING_METACARPAL_EXT(EXTHandTracking.XR_HAND_JOINT_RING_METACARPAL_EXT),

    /**
     * The proximal phalanx of the ring finger is the first bone segment from the metacarpal towards the tip of the ring finger.
     */
    RING_PROXIMAL_EXT(EXTHandTracking.XR_HAND_JOINT_RING_PROXIMAL_EXT),

    /**
     * The intermediate phalanx of the ring finger is the bone segment in the middle of the ring finger, between the proximal and distal phalanges.
     */
    RING_INTERMEDIATE_EXT(EXTHandTracking.XR_HAND_JOINT_RING_INTERMEDIATE_EXT),

    /**
     * The distal phalanx of the ring finger is the bone segment closest to the tip of the ring finger.
     */
    RING_DISTAL_EXT(EXTHandTracking.XR_HAND_JOINT_RING_DISTAL_EXT),

    /**
     * The tip of the ring finger is the very end of the finger, beyond the distal phalanx.
     */
    RING_TIP_EXT(EXTHandTracking.XR_HAND_JOINT_RING_TIP_EXT),

    /**
     * The metacarpal of the little finger, also known as the pinky, is the bone within the finger closest to the wrist.
     */
    LITTLE_METACARPAL_EXT(EXTHandTracking.XR_HAND_JOINT_LITTLE_METACARPAL_EXT),

    /**
     * The proximal phalanx of the little finger is the first bone segment from the metacarpal towards the tip of the little finger.
     */
    LITTLE_PROXIMAL_EXT(EXTHandTracking.XR_HAND_JOINT_LITTLE_PROXIMAL_EXT),

    /**
     * The intermediate phalanx of the little finger is the bone segment in the middle of the little finger, between the proximal and distal phalanges.
     */
    LITTLE_INTERMEDIATE_EXT(EXTHandTracking.XR_HAND_JOINT_LITTLE_INTERMEDIATE_EXT),

    /**
     * The distal phalanx of the little finger is the bone segment closest to the tip of the little finger.
     */
    LITTLE_DISTAL_EXT(EXTHandTracking.XR_HAND_JOINT_LITTLE_DISTAL_EXT),

    /**
     * The tip of the little finger is the very end of the finger, beyond the distal phalanx.
     */
    LITTLE_TIP_EXT(EXTHandTracking.XR_HAND_JOINT_LITTLE_TIP_EXT);

    private final int jointIndex;

    HandJoint(int jointIndex) {
        this.jointIndex = jointIndex;
    }

    public int getJointIndex(){
        return jointIndex;
    }
}
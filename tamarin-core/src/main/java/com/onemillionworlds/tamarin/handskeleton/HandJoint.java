package com.onemillionworlds.tamarin.handskeleton;

public enum HandJoint {
    /**
     * The palm is the central area of the hand, between the fingers and the wrist. This is the root of the hand skeleton.
     */
    PALM_EXT(0), // EXTHandTracking.XR_HAND_JOINT_PALM_EXT

    /**
     * The wrist is the joint connecting the hand with the forearm.
     */
    WRIST_EXT(1), // EXTHandTracking.XR_HAND_JOINT_WRIST_EXT

    /**
     * The metacarpal of the thumb is the bone within the thumb, closest to the wrist.
     */
    THUMB_METACARPAL_EXT(2), // EXTHandTracking.XR_HAND_JOINT_THUMB_METACARPAL_EXT

    /**
     * The proximal phalanx of the thumb is the first bone segment from the metacarpal towards the tip of the thumb.
     */
    THUMB_PROXIMAL_EXT(3), // EXTHandTracking.XR_HAND_JOINT_THUMB_PROXIMAL_EXT

    /**
     * The distal phalanx of the thumb is the bone segment closest to the tip of the thumb.
     */
    THUMB_DISTAL_EXT(4), // EXTHandTracking.XR_HAND_JOINT_THUMB_DISTAL_EXT

    /**
     * The tip of the thumb is the very end of the thumb, beyond the distal phalanx.
     */
    THUMB_TIP_EXT(5), // EXTHandTracking.XR_HAND_JOINT_THUMB_TIP_EXT

    /**
     * The metacarpal of the index finger is the bone within the index finger, closest to the wrist.
     */
    INDEX_METACARPAL_EXT(6), // EXTHandTracking.XR_HAND_JOINT_INDEX_METACARPAL_EXT

    /**
     * The proximal phalanx of the index finger is the first bone segment from the metacarpal towards the tip of the index finger.
     */
    INDEX_PROXIMAL_EXT(7), // EXTHandTracking.XR_HAND_JOINT_INDEX_PROXIMAL_EXT

    /**
     * The intermediate phalanx of the index finger is the bone segment in the middle of the index finger, between the proximal and distal phalanges.
     */
    INDEX_INTERMEDIATE_EXT(8), // EXTHandTracking.XR_HAND_JOINT_INDEX_INTERMEDIATE_EXT

    /**
     * The distal phalanx of the index finger is the bone segment closest to the tip of the index finger.
     */
    INDEX_DISTAL_EXT(9), // EXTHandTracking.XR_HAND_JOINT_INDEX_DISTAL_EXT

    /**
     * The tip of the index finger is the very end of the finger, beyond the distal phalanx.
     */
    INDEX_TIP_EXT(10), // EXTHandTracking.XR_HAND_JOINT_INDEX_TIP_EXT

    /**
     * The metacarpal of the middle finger is the bone within the middle finger, closest to the wrist.
     */
    MIDDLE_METACARPAL_EXT(11), // EXTHandTracking.XR_HAND_JOINT_MIDDLE_METACARPAL_EXT

    /**
     * The proximal phalanx of the middle finger is the first bone segment from the metacarpal towards the tip of the middle finger.
     */
    MIDDLE_PROXIMAL_EXT(12), // EXTHandTracking.XR_HAND_JOINT_MIDDLE_PROXIMAL_EXT

    /**
     * The intermediate phalanx of the middle finger is the bone segment in the middle of the middle finger, between the proximal and distal phalanges.
     */
    MIDDLE_INTERMEDIATE_EXT(13), // EXTHandTracking.XR_HAND_JOINT_MIDDLE_INTERMEDIATE_EXT

    /**
     * The distal phalanx of the middle finger is the bone segment closest to the tip of the middle finger.
     */
    MIDDLE_DISTAL_EXT(14), // EXTHandTracking.XR_HAND_JOINT_MIDDLE_DISTAL_EXT

    /**
     * The tip of the middle finger is the very end of the finger, beyond the distal phalanx.
     */
    MIDDLE_TIP_EXT(15), // EXTHandTracking.XR_HAND_JOINT_MIDDLE_TIP_EXT

    /**
     * The metacarpal of the ring finger is the bone within the ring finger, closest to the wrist.
     */
    RING_METACARPAL_EXT(16), // EXTHandTracking.XR_HAND_JOINT_RING_METACARPAL_EXT

    /**
     * The proximal phalanx of the ring finger is the first bone segment from the metacarpal towards the tip of the ring finger.
     */
    RING_PROXIMAL_EXT(17), // EXTHandTracking.XR_HAND_JOINT_RING_PROXIMAL_EXT

    /**
     * The intermediate phalanx of the ring finger is the bone segment in the middle of the ring finger, between the proximal and distal phalanges.
     */
    RING_INTERMEDIATE_EXT(18), // EXTHandTracking.XR_HAND_JOINT_RING_INTERMEDIATE_EXT

    /**
     * The distal phalanx of the ring finger is the bone segment closest to the tip of the ring finger.
     */
    RING_DISTAL_EXT(19), // EXTHandTracking.XR_HAND_JOINT_RING_DISTAL_EXT

    /**
     * The tip of the ring finger is the very end of the finger, beyond the distal phalanx.
     */
    RING_TIP_EXT(20), // EXTHandTracking.XR_HAND_JOINT_RING_TIP_EXT

    /**
     * The metacarpal of the little finger, also known as the pinky, is the bone within the finger closest to the wrist.
     */
    LITTLE_METACARPAL_EXT(21), // EXTHandTracking.XR_HAND_JOINT_LITTLE_METACARPAL_EXT

    /**
     * The proximal phalanx of the little finger is the first bone segment from the metacarpal towards the tip of the little finger.
     */
    LITTLE_PROXIMAL_EXT(22), // EXTHandTracking.XR_HAND_JOINT_LITTLE_PROXIMAL_EXT

    /**
     * The intermediate phalanx of the little finger is the bone segment in the middle of the little finger, between the proximal and distal phalanges.
     */
    LITTLE_INTERMEDIATE_EXT(23), // EXTHandTracking.XR_HAND_JOINT_LITTLE_INTERMEDIATE_EXT

    /**
     * The distal phalanx of the little finger is the bone segment closest to the tip of the little finger.
     */
    LITTLE_DISTAL_EXT(24), // EXTHandTracking.XR_HAND_JOINT_LITTLE_DISTAL_EXT

    /**
     * The tip of the little finger is the very end of the finger, beyond the distal phalanx.
     */
    LITTLE_TIP_EXT(25); // EXTHandTracking.XR_HAND_JOINT_LITTLE_TIP_EXT

    private final int jointIndex;

    HandJoint(int jointIndex) {
        this.jointIndex = jointIndex;
    }

    public int getJointIndex(){
        return jointIndex;
    }
}
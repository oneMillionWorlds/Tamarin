package com.onemillionworlds.tamarin.vrhands;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BoneMappings{

    private static Map<HandJoint, String> boneMappingsLeft = Collections.unmodifiableMap(defaultMappings());
    private static Map<HandJoint, String> boneMappingsRight= boneMappingsLeft;

    public static Map<HandJoint, String> getBoneMappings(HandSide handSide) {
        return handSide == HandSide.LEFT ? boneMappingsLeft : boneMappingsRight;
    }

    public static Map<HandJoint, String> getBoneMappingsLeft(){
        return boneMappingsLeft;
    }

    public static Map<HandJoint, String> getBoneMappingsRight(){
        return boneMappingsRight;
    }

    /**
     * Override the default bone mappings for the left hand.
     */
    @SuppressWarnings("unused")
    public static void setBoneMappingsLeft(Map<HandJoint, String> boneMappingsLeft) {
        BoneMappings.boneMappingsLeft = Collections.unmodifiableMap(boneMappingsLeft);
    }

    /**
     * Override the default bone mappings for the right hand.
     */
    @SuppressWarnings("unused")
    public static void setBoneMappingsRight(Map<HandJoint, String> boneMappingsRight) {
        BoneMappings.boneMappingsRight = Collections.unmodifiableMap(boneMappingsRight);
    }

    private static Map<HandJoint, String> defaultMappings(){
        Map<HandJoint, String> boneMappings = new HashMap<>();

        boneMappings.put(HandJoint.WRIST_EXT, "wrist");
        boneMappings.put(HandJoint.PALM_EXT, "palm");

        boneMappings.put(HandJoint.THUMB_METACARPAL_EXT, "finger_thumb_0");
        boneMappings.put(HandJoint.THUMB_PROXIMAL_EXT, "finger_thumb_1");
        boneMappings.put(HandJoint.THUMB_DISTAL_EXT, "finger_thumb_2");
        boneMappings.put(HandJoint.THUMB_TIP_EXT, "finger_thumb_end");

        boneMappings.put(HandJoint.INDEX_METACARPAL_EXT, "finger_index_meta");
        boneMappings.put(HandJoint.INDEX_PROXIMAL_EXT, "finger_index_0");
        boneMappings.put(HandJoint.INDEX_INTERMEDIATE_EXT, "finger_index_1");
        boneMappings.put(HandJoint.INDEX_DISTAL_EXT, "finger_index_2");
        boneMappings.put(HandJoint.INDEX_TIP_EXT, "finger_index_end");

        boneMappings.put(HandJoint.MIDDLE_METACARPAL_EXT, "finger_middle_meta");
        boneMappings.put(HandJoint.MIDDLE_PROXIMAL_EXT, "finger_middle_0");
        boneMappings.put(HandJoint.MIDDLE_INTERMEDIATE_EXT, "finger_middle_1");
        boneMappings.put(HandJoint.MIDDLE_DISTAL_EXT, "finger_middle_2");
        boneMappings.put(HandJoint.MIDDLE_TIP_EXT, "finger_middle_end");

        boneMappings.put(HandJoint.RING_METACARPAL_EXT, "finger_ring_meta");
        boneMappings.put(HandJoint.RING_PROXIMAL_EXT, "finger_ring_0");
        boneMappings.put(HandJoint.RING_INTERMEDIATE_EXT, "finger_ring_1");
        boneMappings.put(HandJoint.RING_DISTAL_EXT, "finger_ring_2");
        boneMappings.put(HandJoint.RING_TIP_EXT, "finger_ring_end");

        boneMappings.put(HandJoint.LITTLE_METACARPAL_EXT, "finger_pinky_meta");
        boneMappings.put(HandJoint.LITTLE_PROXIMAL_EXT, "finger_pinky_0");
        boneMappings.put(HandJoint.LITTLE_INTERMEDIATE_EXT, "finger_pinky_1");
        boneMappings.put(HandJoint.LITTLE_DISTAL_EXT, "finger_pinky_2");
        boneMappings.put(HandJoint.LITTLE_TIP_EXT, "finger_pinky_end");
        return boneMappings;
    }

    /**
     * Tamarin 1.x had openVR style bone names which (annoyingly) had _r and _l in the bone names. If you have
     * existing models that use these names, you can use this method to get the old mappings.
     */
    @SuppressWarnings("unused")
    public static Map<HandJoint, String> legacyOpenVrLeftHandMappings() {
        Map<HandJoint, String> boneMappings = defaultMappings();
        Map<HandJoint, String> mutatedMappings = new HashMap<>();
        for (Map.Entry<HandJoint, String> entry : boneMappings.entrySet()) {
            String updatedValue = entry.getValue() + "_l";
            mutatedMappings.put(entry.getKey(), updatedValue);
        }

        return mutatedMappings;
    }

    /**
     * Tamarin 1.x had openVR style bone names which (annoyingly) had _r and _l in the bone names. If you have
     * existing models that use these names, you can use this method to get the old mappings.
     */
    @SuppressWarnings("unused")
    public static Map<HandJoint, String> legacyOpenVrRightHandMappings() {
        Map<HandJoint, String> boneMappings = defaultMappings();
        Map<HandJoint, String> mutatedMappings = new HashMap<>();
        for (Map.Entry<HandJoint, String> entry : boneMappings.entrySet()) {
            String updatedValue = entry.getValue() + "_r";
            mutatedMappings.put(entry.getKey(), updatedValue);
        }

        return mutatedMappings;
    }
}


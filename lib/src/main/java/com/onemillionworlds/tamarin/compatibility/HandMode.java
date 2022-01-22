package com.onemillionworlds.tamarin.compatibility;

import org.lwjgl.openvr.VR;

public enum HandMode{

    /**
     * Hands in this mode look like they are holding an invisible controller. This is the systems best guess at
     * where the hands truly are.
     *
     * If in doubt; use this mode when the player is holding something.
     */
    WITH_CONTROLLER(VR.EVRSkeletalMotionRange_VRSkeletalMotionRange_WithController),
    /**
     * Hands in this mode do not look like they are holding an invisible controller. This is the systems best guess at
     * where the user INTENDS their hands to be. In this mode making a fist or fully stretching out the fingers is
     * possible.
     *
     * If in doubt; use this mode when the player has empty hands.
     */
    WITHOUT_CONTROLLER(VR.EVRSkeletalMotionRange_VRSkeletalMotionRange_WithoutController);

    public final int openVrHandle;

    HandMode(int openVrHandle){
        this.openVrHandle = openVrHandle;
    }
}

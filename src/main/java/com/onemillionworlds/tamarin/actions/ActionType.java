package com.onemillionworlds.tamarin.actions;

import lombok.Getter;
import org.lwjgl.openxr.XR10;

@Getter
public enum ActionType{
    BOOLEAN(XR10.XR_ACTION_TYPE_BOOLEAN_INPUT),
    FLOAT(XR10.XR_ACTION_TYPE_FLOAT_INPUT),
    /**
     * Note that while it may appear like all the component paths are 1 dimentional
     * (e.g. <code>OculusTouchController.pathBuilder().leftHand().thumbStickX()</code> it is possible
     * to add both an X and a Y to the suggested binding and it "all just sorts itself out".
     */
    VECTOR2F(XR10.XR_ACTION_TYPE_VECTOR2F_INPUT),
    /**
     * A hand position (may also include bones)
     */
    POSE(XR10.XR_ACTION_TYPE_POSE_INPUT),
    /**
     * Vibrating the controller
     */
    HAPTIC(XR10.XR_ACTION_TYPE_VIBRATION_OUTPUT);

    private final int openXrOption;

    ActionType(int openXrOption){
        this.openXrOption = openXrOption;
    }

}

package com.onemillionworlds.tamarin.actions;

public enum ActionType{
    BOOLEAN(1), // XR10.XR_ACTION_TYPE_BOOLEAN_INPUT
    FLOAT(2), // XR10.XR_ACTION_TYPE_FLOAT_INPUT
    /**
     * Note that while it may appear like all the component paths are 1 dimentional
     * (e.g. <code>OculusTouchController.pathBuilder().leftHand().thumbStickX()</code> it is possible
     * to add both an X and a Y to the suggested binding and it "all just sorts itself out".
     */
    VECTOR2F(3), // XR10.XR_ACTION_TYPE_VECTOR2F_INPUT
    /**
     * A hand position (may also include bones)
     */
    POSE(4), //XR10.XR_ACTION_TYPE_POSE_INPUT
    /**
     * Vibrating the controller
     */
    HAPTIC(100); // XR10.XR_ACTION_TYPE_VIBRATION_OUTPUT

    private final int openXrOption;

    ActionType(int openXrOption){
        this.openXrOption = openXrOption;
    }

    public int getOpenXrOption(){
        return openXrOption;
    }
}

package com.onemillionworlds.tamarin.handgestures;

/**
 * Where the pinch and aim information in a {@link HandGestureState} came from.
 */
public enum HandGestureSource{
    /**
     * The OpenXR runtime recognised the gesture itself. Currently this means a Meta runtime, using the
     * XR_FB_hand_tracking_aim extension. Usually better quality (it is tuned by the runtime and copes better with
     * fingers being hidden from the cameras).
     */
    RUNTIME,
    /**
     * Tamarin worked out the gesture from the positions of the hand's joints (bones). Available on any runtime that
     * supports hand tracking (XR_EXT_hand_tracking).
     */
    TAMARIN
}

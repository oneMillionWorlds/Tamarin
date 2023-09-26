package com.onemillionworlds.tamarin.openxr;

import lombok.AllArgsConstructor;
import org.lwjgl.openxr.XR10;

@AllArgsConstructor
public enum SessionState{
    UNKNOWN(XR10.XR_SESSION_STATE_UNKNOWN),
    IDLE(XR10.XR_SESSION_STATE_IDLE),
    READY(XR10.XR_SESSION_STATE_READY),
    SYNCHRONIZED(XR10.XR_SESSION_STATE_SYNCHRONIZED),
    VISIBLE(XR10.XR_SESSION_STATE_VISIBLE),
    FOCUSED(XR10.XR_SESSION_STATE_FOCUSED),
    STOPPING(XR10.XR_SESSION_STATE_STOPPING),
    LOSS_PENDING(XR10.XR_SESSION_STATE_LOSS_PENDING),
    EXITING(XR10.XR_SESSION_STATE_EXITING);

    private final int xrValue;

    public static SessionState fromXRValue(int xrValue) {
        for (SessionState state : values()) {
            if (state.xrValue == xrValue) {
                return state;
            }
        }
        throw new RuntimeException("Unexpected xrValue: " + xrValue);
    }
}

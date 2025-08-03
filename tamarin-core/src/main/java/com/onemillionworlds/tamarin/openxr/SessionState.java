package com.onemillionworlds.tamarin.openxr;

import org.lwjgl.openxr.XR10;

public enum SessionState{
    /**
     * UNKNOWN: An unknown state. The runtime must not return this value in an XrEventDataSessionStateChanged event.
     * It is defined to avoid 0 being a valid state.
     */
    UNKNOWN(XR10.XR_SESSION_STATE_UNKNOWN),

    /**
     * IDLE: The initial state after calling CreateSession or returned to after calling EndSession.
     * In this state, applications should minimize resource consumption but continue to call PollEvent.
     */
    IDLE(XR10.XR_SESSION_STATE_IDLE),

    /**
     * READY: Indicates the application is ready to begin its session and sync its frame loop with the runtime.
     * This state is reached after the runtime desires the application to prepare rendering resources and begin its session.
     */
    READY(XR10.XR_SESSION_STATE_READY),

    /**
     * SYNCHRONIZED: The application has synced its frame loop with the runtime but is not visible to the user.
     * The application should continue its frame loop with minimal GPU usage, allowing higher precedence to visible applications.
     */
    SYNCHRONIZED(XR10.XR_SESSION_STATE_SYNCHRONIZED),

    /**
     * VISIBLE: The session’s frames are visible to the user, but the session cannot receive XR input.
     * Applications should continue rendering to remain visible underneath potential modal pop-ups.
     */
    VISIBLE(XR10.XR_SESSION_STATE_VISIBLE),

    /**
     * FOCUSED: The session is visible to the user and can receive XR input.
     * The runtime should give XR input focus to only one session at a time, and the application should render all active input actions.
     */
    FOCUSED(XR10.XR_SESSION_STATE_FOCUSED),

    /**
     * STOPPING: The runtime has determined that the application should halt its rendering loop.
     * Applications should exit their rendering loop and call EndSession when in this state.
     */
    STOPPING(XR10.XR_SESSION_STATE_STOPPING),

    /**
     * LOSS_PENDING: The session is in the process of being lost, e.g., due to a loss of display hardware connection.
     * Applications should destroy the current session and may opt to recreate it or poll for a new XrSystemId.
     */
    LOSS_PENDING(XR10.XR_SESSION_STATE_LOSS_PENDING),

    /**
     * EXITING: The runtime wishes the application to terminate its XR experience, typically due to a user request.
     * Applications should end their process in this state if they do not have a non-XR user experience.
     */
    EXITING(XR10.XR_SESSION_STATE_EXITING);

    private final int xrValue;

    SessionState(int xrValue){
        this.xrValue = xrValue;
    }

    public static SessionState fromXRValue(int xrValue) {
        for (SessionState state : values()) {
            if (state.xrValue == xrValue) {
                return state;
            }
        }
        throw new RuntimeException("Unexpected xrValue: " + xrValue);
    }

    public boolean isAtLeastReady() {
        return this == READY || this == SYNCHRONIZED || this == VISIBLE || this == FOCUSED;
    }
}

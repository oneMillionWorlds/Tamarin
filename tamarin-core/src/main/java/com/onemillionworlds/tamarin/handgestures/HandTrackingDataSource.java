package com.onemillionworlds.tamarin.handgestures;

/**
 * Where a hand's tracking data (its joints) is coming from. See
 * {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getHandTrackingDataSource}.
 * <p>
 *     This comes from the XR_EXT_hand_tracking_data_source extension (which Tamarin requests by default). On runtimes
 *     without it the source is {@link #UNKNOWN}.
 * </p>
 */
public enum HandTrackingDataSource{
    /**
     * The hand itself is being tracked, with individual fingers (e.g. optical hand tracking, or data gloves). The user
     * is not holding a controller in this hand.
     */
    BARE_HAND,
    /**
     * The joints are being worked out from a controller held in this hand (e.g. SteamVR with Valve Index controllers,
     * which sense the fingers on the controller). Hand gestures are not detected from these joints, as the user is
     * holding a controller (and would mostly be making a fist around it).
     */
    CONTROLLER,
    /**
     * The runtime doesn't say where the data is coming from (it doesn't support XR_EXT_hand_tracking_data_source). It
     * could be either. Note that many runtimes (e.g. Quest) don't provide hand joints at all while a controller is held,
     * so on those the joints are probably from a bare hand.
     */
    UNKNOWN
}

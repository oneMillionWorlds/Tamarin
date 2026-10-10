package com.onemillionworlds.tamarin.actions.gestures;

/**
 * Where the hand joints (see {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getSkeleton}) for a hand
 * are currently coming from. Obtained from
 * {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getHandJointsDataSource}.
 */
public enum HandJointsDataSource {
    /**
     * The hand itself is being tracked, with individual fingers (e.g. optical hand tracking, or data gloves). The user
     * is not holding a controller in this hand.
     */
    BARE_HAND,
    /**
     * The joints are being worked out from a controller held in this hand (e.g. SteamVR with Valve Index controllers,
     * which sense the fingers on the controller). Hand gestures are not detected from these joints, as the user is
     * holding a controller (and would mostly be making a fist around it).
     * <p>
     *     Tamarin only asks for these joints if {@link com.onemillionworlds.tamarin.openxr.XrSettings#setAcceptControllerDerivedHandJoints}
     *     is set, so by default this is not seen.
     * </p>
     */
    CONTROLLER,
    /**
     * Hand joints are being provided, but the runtime doesn't say where the data is coming from (it doesn't support
     * XR_EXT_hand_tracking_data_source). It could be either. Note that many runtimes (e.g. Quest) don't provide hand
     * joints at all while a controller is held, so on those the joints are probably from a bare hand.
     */
    UNKNOWN,
    /**
     * The runtime supports hand joints but isn't providing any for this hand right now (e.g. the hand is out of view
     * of the cameras, or the controller is put down and the runtime can't track bare hands). Also used briefly at
     * start up, before the session is ready.
     */
    NOT_TRACKED,
    /**
     * Hand joints are never available, the runtime doesn't support XR_EXT_hand_tracking. Also always the case in
     * desktop simulation mode.
     */
    HAND_TRACKING_UNSUPPORTED;

    /**
     * XR_HAND_TRACKING_DATA_SOURCE_UNOBSTRUCTED_EXT.
     */
    static final int XR_DATA_SOURCE_UNOBSTRUCTED = 1;
    /**
     * XR_HAND_TRACKING_DATA_SOURCE_CONTROLLER_EXT.
     */
    static final int XR_DATA_SOURCE_CONTROLLER = 2;
    /**
     * XR_HAND_TRACKING_DATA_SOURCE_UNOBSTRUCTED_WIDE_MOTION_META (a bare hand, Tamarin doesn't currently request it
     * but if a runtime gives it anyway it is still a bare hand).
     */
    static final int XR_DATA_SOURCE_UNOBSTRUCTED_WIDE_MOTION_META = 1000695000;

    /**
     * Primarily for internal use. Converts what the runtime reported when locating the hand joints into a data source.
     *
     * @param jointsActive XrHandJointLocationsEXT::isActive
     * @param dataSourceReported if XrHandTrackingDataSourceStateEXT was chained to the locate call (i.e.
     *                           XR_EXT_hand_tracking_data_source is in use). If false the following are ignored
     * @param dataSourceActive XrHandTrackingDataSourceStateEXT::isActive
     * @param dataSource XrHandTrackingDataSourceStateEXT::dataSource (an XrHandTrackingDataSourceEXT value)
     */
    public static HandJointsDataSource fromRuntimeState(boolean jointsActive, boolean dataSourceReported, boolean dataSourceActive, int dataSource){
        if (!jointsActive){
            return NOT_TRACKED;
        }
        if (!dataSourceReported){
            return UNKNOWN;
        }
        if (!dataSourceActive){
            // the spec says that in this case the runtime provides no valid joints
            return NOT_TRACKED;
        }
        switch(dataSource){
            case XR_DATA_SOURCE_UNOBSTRUCTED:
            case XR_DATA_SOURCE_UNOBSTRUCTED_WIDE_MOTION_META:
                return BARE_HAND;
            case XR_DATA_SOURCE_CONTROLLER:
                return CONTROLLER;
            default:
                return UNKNOWN;
        }
    }
}

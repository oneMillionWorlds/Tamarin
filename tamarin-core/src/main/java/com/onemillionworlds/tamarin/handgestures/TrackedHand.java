package com.onemillionworlds.tamarin.handgestures;

import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * The raw hand tracking data for one hand in one frame, as provided by the VR implementation (desktop or Android).
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class TrackedHand{
    private final Map<HandJoint, BonePose> worldJoints;
    private final Optional<HandTrackingAimState> runtimeAimState;
    private final HandTrackingDataSource dataSource;

    /**
     * @param worldJoints every hand joint, in world coordinates
     * @param runtimeAimState the runtime's own gesture recognition (XR_FB_hand_tracking_aim), if available
     * @param dataSource where the joints came from (a bare hand or a held controller), if known
     */
    public TrackedHand(Map<HandJoint, BonePose> worldJoints, Optional<HandTrackingAimState> runtimeAimState, HandTrackingDataSource dataSource){
        if (worldJoints.size() != HandJoint.values().length){
            throw new IllegalArgumentException("All " + HandJoint.values().length + " joints are required, but only " + worldJoints.size() + " were provided");
        }
        this.worldJoints = Collections.unmodifiableMap(worldJoints);
        this.runtimeAimState = runtimeAimState;
        this.dataSource = dataSource;
    }

    /**
     * As {@link #TrackedHand(Map, Optional, HandTrackingDataSource)} with an {@link HandTrackingDataSource#UNKNOWN} data source.
     */
    public TrackedHand(Map<HandJoint, BonePose> worldJoints, Optional<HandTrackingAimState> runtimeAimState){
        this(worldJoints, runtimeAimState, HandTrackingDataSource.UNKNOWN);
    }

    public Map<HandJoint, BonePose> getWorldJoints(){
        return worldJoints;
    }

    public Optional<HandTrackingAimState> getRuntimeAimState(){
        return runtimeAimState;
    }

    public HandTrackingDataSource getDataSource(){
        return dataSource;
    }
}

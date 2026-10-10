package com.onemillionworlds.tamarin.actions.gestures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandJointsDataSourceTest{

    @Test
    void inactiveJointsAreNotTrackedRegardlessOfDataSource(){
        assertEquals(HandJointsDataSource.NOT_TRACKED, HandJointsDataSource.fromRuntimeState(false, true, true, HandJointsDataSource.XR_DATA_SOURCE_UNOBSTRUCTED));
        assertEquals(HandJointsDataSource.NOT_TRACKED, HandJointsDataSource.fromRuntimeState(false, false, false, 0));
    }

    @Test
    void activeJointsWithoutDataSourceExtensionAreUnknown(){
        assertEquals(HandJointsDataSource.UNKNOWN, HandJointsDataSource.fromRuntimeState(true, false, false, 0));
    }

    @Test
    void inactiveDataSourceIsNotTracked(){
        assertEquals(HandJointsDataSource.NOT_TRACKED, HandJointsDataSource.fromRuntimeState(true, true, false, HandJointsDataSource.XR_DATA_SOURCE_CONTROLLER));
    }

    @Test
    void unobstructedDataSourcesAreBareHand(){
        assertEquals(HandJointsDataSource.BARE_HAND, HandJointsDataSource.fromRuntimeState(true, true, true, HandJointsDataSource.XR_DATA_SOURCE_UNOBSTRUCTED));
        assertEquals(HandJointsDataSource.BARE_HAND, HandJointsDataSource.fromRuntimeState(true, true, true, HandJointsDataSource.XR_DATA_SOURCE_UNOBSTRUCTED_WIDE_MOTION_META));
    }

    @Test
    void controllerDataSourceIsController(){
        assertEquals(HandJointsDataSource.CONTROLLER, HandJointsDataSource.fromRuntimeState(true, true, true, HandJointsDataSource.XR_DATA_SOURCE_CONTROLLER));
    }

    @Test
    void unrecognisedDataSourceIsUnknown(){
        assertEquals(HandJointsDataSource.UNKNOWN, HandJointsDataSource.fromRuntimeState(true, true, true, 12345));
    }
}

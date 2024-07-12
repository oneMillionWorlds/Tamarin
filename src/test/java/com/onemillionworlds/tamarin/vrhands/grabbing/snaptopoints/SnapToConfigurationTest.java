package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.mock;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapToConfigurationTest{
    private final SnapToLocalPoint snapToPoint1 = new SnapToLocalPoint(new Vector3f(1, 1, 1), 2.0f);
    private final SnapToLocalPoint snapToPoint2 = new SnapToLocalPoint(new Vector3f(2, 2, 2), 2.0f);
    private final SnapToLocalPoint snapToPoint3 = new SnapToLocalPoint(new Vector3f(3, 3, 3), 2.0f);

    static Vector3f localPositionOrigin = new Vector3f(100, 1000, 10000);

    private final BoundHand mockHand = mock(BoundHand.class);

    private static final RestrictionUtilities restrictionUtilities = new RestrictionUtilities(
            local -> local.add(localPositionOrigin),
            global -> global.subtract(localPositionOrigin)
    );


    @Test
    void testSnap_NoPoints() {
        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(Collections.emptyList());

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToConfiguration.snap(position, restrictionUtilities, mockHand);

        assertFalse(result.isPresent(), "No points to snap to should return empty");
    }

    @Test
    void testSnap_WithinRadius() {
        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToConfiguration.snap(position, restrictionUtilities, mockHand);

        assertTrue(result.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_OutsideRadius() {
        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(5, 5, 5);
        Optional<Vector3f> result = snapToConfiguration.snap(position, restrictionUtilities, mockHand);

        assertFalse(result.isPresent(), "Position outside radius should not snap");
    }

    @Test
    void testSnap_MultiplePointsWithinRadius() {
        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPoint1, snapToPoint2, snapToPoint3));

        Vector3f position = new Vector3f(2.1f, 2.1f, 2.1f);
        Optional<Vector3f> result = snapToConfiguration.snap(position, restrictionUtilities, mockHand);

        assertTrue(result.isPresent(), "Position within radius of multiple points should snap");
        assertEquals(new Vector3f(2, 2, 2), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_GlobalMode() {
        SnapToGlobalPoint snapToPointGlobal = new SnapToGlobalPoint(new Vector3f(100, 1000, 10000), 2.0f);

        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPointGlobal));

        Vector3f position = new Vector3f(0.5f,0.5f,0.5f);
        Optional<Vector3f> result = snapToConfiguration.snap(position, restrictionUtilities, mockHand);

        assertTrue(result.isPresent(), "Position within global radius should snap");
        assertEquals(new Vector3f(0, 0, 0), result.get(), "Position should snap to the transformed closest point (but in local coords)");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testSnap_OnSnapAndOnUnSnapCallbacks() {
        SnapToLocalPoint snapToPoint1 = new SnapToLocalPoint(new Vector3f(1, 1, 1), 2.0f);
        SnapToLocalPoint snapToPoint2 = new SnapToLocalPoint(new Vector3f(2, 2, 2), 2.0f);

        ArgumentRecordingOnSnapCallback onSnapCallbacks = new ArgumentRecordingOnSnapCallback();

        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPoint1, snapToPoint2), onSnapCallbacks);

        Vector3f position1 = new Vector3f(0, 0, 0);
        Vector3f position2 = new Vector3f(2f, 2f, 2f);

        // Snap to first point
        Optional<Vector3f> result1 = snapToConfiguration.snap(position1, restrictionUtilities, mockHand);
        assertTrue(result1.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result1.get(), "Position should snap to the first point");
        onSnapCallbacks.assertAndClearExpectedState(
                new Vector3f(1, 1, 1),
                false,
                null,
                new Vector3f(1, 1, 1)
        );

        Optional<Vector3f> result1_5 = snapToConfiguration.snap(position1, restrictionUtilities, mockHand);
        assertTrue(result1_5.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result1.get(), "Position should continue to snap to the first point");
        onSnapCallbacks.assertAndClearExpectedState(
                null,
                false,
                null,
                new Vector3f(1, 1, 1)
        );

        // Snap to second point
        Optional<Vector3f> result2 = snapToConfiguration.snap(position2, restrictionUtilities, mockHand);
        assertTrue(result2.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(2, 2, 2), result2.get(), "Position should snap to the second point");
        onSnapCallbacks.assertAndClearExpectedState(
                null,
                false,
                new Vector3f(2, 2, 2),
                new Vector3f(2, 2, 2)
        );

        //fail to snap
        Optional<Vector3f> result3 = snapToConfiguration.snap(new Vector3f(100,100,100), restrictionUtilities, mockHand);
        assertFalse(result3.isPresent(), "Position outside radius should not snap");
        onSnapCallbacks.assertAndClearExpectedState(
                null,
                true,
                null,
                null
        );

        //2nd fail to snap
        Optional<Vector3f> result4 = snapToConfiguration.snap(new Vector3f(100,100,100), restrictionUtilities, mockHand);
        assertFalse(result4.isPresent(), "Position outside radius should not snap");
        onSnapCallbacks.assertAndClearExpectedState(
                null,
                false,
                null,
                null
        );

    }

    private static class ArgumentRecordingOnSnapCallback extends SnapChangeCallback{
        private Arguments arguments_onSnapFromUnsnapped;

        private BoundHand arguments_onUnsnapFromSnapped;

        private Arguments arguments_onSnapTransfer;

        private Arguments arguments_onSnapContinues;

        @Override
        public void onSnapFromUnsnapped(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
            arguments_onSnapFromUnsnapped = new Arguments(handGrabbing, snappedToLocal, snappedToLocal);
        }

        @Override
        public void onUnsnapFromSnapped(BoundHand handGrabbing){
            arguments_onUnsnapFromSnapped = handGrabbing;
        }

        @Override
        public void onSnapTransfer(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
            arguments_onSnapTransfer = new Arguments(handGrabbing, snappedToLocal, snappedToLocal);
        }

        @Override
        public void onSnapContinues(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
            arguments_onSnapContinues = new Arguments(handGrabbing, snappedToLocal, snappedToLocal);
        }

        /**
         * Asserts that the expected state is as follows, null means "no call to contructor":
         * @param onSnapFromUnsnapped the local position that was snapped to (on an initial snap)
         * @param onUnsnapFromSnapped whether the object was unsnapped
         * @param onSnapTransfer the local position that was snapped to (on a transfer)
         * @param onSnapContinues the local position that was snapped to (on a continues)
         */
        public void assertAndClearExpectedState(Vector3f onSnapFromUnsnapped, boolean onUnsnapFromSnapped, Vector3f onSnapTransfer, Vector3f onSnapContinues){

            assertEquals(onSnapFromUnsnapped, arguments_onSnapFromUnsnapped, "onSnapFromUnsnapped");

            if(onUnsnapFromSnapped){
                assertNotNull(arguments_onUnsnapFromSnapped, "onUnsnapFromSnapped should be called");
            } else{
                assertNull(arguments_onUnsnapFromSnapped, "onUnsnapFromSnapped should not be called");
            }

            assertEquals(onSnapTransfer, arguments_onSnapTransfer, "onSnapTransfer");
            assertEquals(onSnapContinues, arguments_onSnapContinues, "onSnapContinues");

            arguments_onSnapFromUnsnapped = null;
            arguments_onUnsnapFromSnapped = null;
            arguments_onSnapTransfer = null;
            arguments_onSnapContinues = null;
        }

        private void assertEquals(Vector3f expectedLocalPosition, Arguments actualArguments, String callbackName){
            Vector3f actualLocalPosition = Optional.ofNullable(actualArguments).map(Arguments::localPosition).orElse(null);
            Assertions.assertEquals(expectedLocalPosition, actualLocalPosition, callbackName + " expected to be " + expectedLocalPosition + " but was " + actualLocalPosition);
        }

    }

    private record Arguments(BoundHand handSide, Vector3f localPosition, Vector3f globalPosition) {}

}
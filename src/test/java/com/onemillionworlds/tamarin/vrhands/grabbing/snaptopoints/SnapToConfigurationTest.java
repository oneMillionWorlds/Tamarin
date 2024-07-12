package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
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

        SnapToLocalPoint snapToPointGlobal = new SnapToLocalPoint(new Vector3f(100, 1000, 10000), 2.0f);

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

        ArgumentRecordingOnSnapCallback onSnap = new ArgumentRecordingOnSnapCallback();
        ArgumentRecordingOnUnSnapCallback onUnSnap = new ArgumentRecordingOnUnSnapCallback();

        SnapToConfiguration snapToConfiguration = new SnapToConfiguration(List.of(snapToPoint1, snapToPoint2), onSnap, onUnSnap);

        Vector3f position1 = new Vector3f(0, 0, 0);
        Vector3f position2 = new Vector3f(2f, 2f, 2f);

        // Snap to first point
        Optional<Vector3f> result1 = snapToConfiguration.snap(position1, restrictionUtilities, mockHand);
        assertTrue(result1.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result1.get(), "Position should snap to the first point");
        assertEquals(onSnap.poll().localPosition, new Vector3f(1, 1, 1), "OnSnap should be called with the snapped position");
        assertNull(onUnSnap.poll());

        // Snap to second point
        Optional<Vector3f> result2 = snapToConfiguration.snap(position2, restrictionUtilities, mockHand);
        assertTrue(result2.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(2, 2, 2), result2.get(), "Position should snap to the second point");
        assertEquals(onSnap.poll().localPosition, new Vector3f(2, 2, 2), "OnSnap should be called with the snapped position");
        assertNotNull(onUnSnap.poll(), "OnUnSnap should be called with the previous snapped position");

        //fail to snap
        Optional<Vector3f> result3 = snapToConfiguration.snap(new Vector3f(100,100,100), restrictionUtilities, mockHand);
        assertFalse(result3.isPresent(), "Position outside radius should not snap");
        assertNotNull(onUnSnap.poll(), "OnUnSnap should be called with the previous snapped position");
        assertNull(onSnap.poll());

        //2nd fail to snap
        Optional<Vector3f> result4 = snapToConfiguration.snap(new Vector3f(100,100,100), restrictionUtilities, mockHand);
        assertFalse(result4.isPresent(), "Position outside radius should not snap");
        assertNull(onUnSnap.poll());
        assertNull(onSnap.poll());

    }

    private static class ArgumentRecordingOnSnapCallback implements SnapToConfiguration.OnSnapCallback {
        private final List<Arguments> arguments = new ArrayList<>();

        @Override
        public void onSnap(BoundHand boundHand, Vector3f localPosition, Vector3f globalPosition) {
            arguments.add(new Arguments(boundHand, localPosition, globalPosition));
        }

        public Arguments poll(){
            return arguments.isEmpty() ? null : arguments.remove(0);
        }
    }

    private static class ArgumentRecordingOnUnSnapCallback implements SnapToConfiguration.OnUnSnapCallback {
        private final List<Arguments> arguments = new ArrayList<>();

        @Override
        public void onUnSnap(BoundHand boundHand) {
            arguments.add(new Arguments(boundHand, null, null));
        }

        public Arguments poll(){
            return arguments.isEmpty() ? null : arguments.remove(0);
        }
    }

    private record Arguments(BoundHand handSide, Vector3f localPosition, Vector3f globalPosition) {}

}
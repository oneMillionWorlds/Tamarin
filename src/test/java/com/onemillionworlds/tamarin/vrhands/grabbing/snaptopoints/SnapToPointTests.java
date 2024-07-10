package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapToPointTests{
    private final SnapToPoint snapToPoint1 = new SnapToPoint(new Vector3f(1, 1, 1), 2.0f);
    private final SnapToPoint snapToPoint2 = new SnapToPoint(new Vector3f(2, 2, 2), 2.0f);
    private final SnapToPoint snapToPoint3 = new SnapToPoint(new Vector3f(3, 3, 3), 2.0f);

    static Vector3f localPositionOrigin = new Vector3f(100, 1000, 10000);

    private static final RestrictionUtilities restrictionUtilities = new RestrictionUtilities(
            local -> local.add(localPositionOrigin),
            global -> global.subtract(localPositionOrigin)
    );


    @Test
    void testSnap_NoPoints() {
        SnapToPoints snapToPoints = new SnapToPoints(false, Collections.emptyList());

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities, HandSide.LEFT);

        assertFalse(result.isPresent(), "No points to snap to should return empty");
    }

    @Test
    void testSnap_WithinRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities, HandSide.LEFT);

        assertTrue(result.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_OutsideRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(5, 5, 5);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities, HandSide.LEFT);

        assertFalse(result.isPresent(), "Position outside radius should not snap");
    }

    @Test
    void testSnap_MultiplePointsWithinRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2, snapToPoint3));

        Vector3f position = new Vector3f(2.1f, 2.1f, 2.1f);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities, HandSide.LEFT);

        assertTrue(result.isPresent(), "Position within radius of multiple points should snap");
        assertEquals(new Vector3f(2, 2, 2), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_GlobalMode() {

        SnapToPoint snapToPointGlobal = new SnapToPoint(new Vector3f(100, 1000, 10000), 2.0f);

        SnapToPoints snapToPoints = new SnapToPoints(true, List.of(snapToPointGlobal));

        Vector3f position = new Vector3f(0.5f,0.5f,0.5f);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities, HandSide.LEFT);

        assertTrue(result.isPresent(), "Position within global radius should snap");
        assertEquals(new Vector3f(0, 0, 0), result.get(), "Position should snap to the transformed closest point (but in local coords)");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testSnap_OnSnapAndOnUnSnapCallbacks() {
        SnapToPoint snapToPoint1 = new SnapToPoint(new Vector3f(1, 1, 1), 2.0f);
        SnapToPoint snapToPoint2 = new SnapToPoint(new Vector3f(2, 2, 2), 2.0f);

        ArgumentRecordingOnSnapCallback onSnap = new ArgumentRecordingOnSnapCallback();
        ArgumentRecordingOnUnSnapCallback onUnSnap = new ArgumentRecordingOnUnSnapCallback();

        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2), onSnap, onUnSnap);

        Vector3f position1 = new Vector3f(0, 0, 0);
        Vector3f position2 = new Vector3f(2f, 2f, 2f);

        // Snap to first point
        Optional<Vector3f> result1 = snapToPoints.snap(position1, restrictionUtilities, HandSide.LEFT);
        assertTrue(result1.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result1.get(), "Position should snap to the first point");
        assertEquals(onSnap.poll().localPosition, new Vector3f(1, 1, 1), "OnSnap should be called with the snapped position");
        assertNull(onUnSnap.poll());

        // Snap to second point
        Optional<Vector3f> result2 = snapToPoints.snap(position2, restrictionUtilities, HandSide.LEFT);
        assertTrue(result2.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(2, 2, 2), result2.get(), "Position should snap to the second point");
        assertEquals(onSnap.poll().localPosition, new Vector3f(2, 2, 2), "OnSnap should be called with the snapped position");
        assertEquals(onUnSnap.poll().localPosition, new Vector3f(1, 1, 1), "OnUnSnap should be called with the previous snapped position");

        //fail to snap
        Optional<Vector3f> result3 = snapToPoints.snap(new Vector3f(100,100,100), restrictionUtilities, HandSide.LEFT);
        assertFalse(result3.isPresent(), "Position outside radius should not snap");
        assertEquals(onUnSnap.poll().localPosition, new Vector3f(2, 2, 2), "OnUnSnap should be called with the previous snapped position");
        assertNull(onSnap.poll());
    }

    private static class ArgumentRecordingOnSnapCallback implements SnapToPoints.OnSnapCallback {
        private final List<Arguments> arguments = new ArrayList<>();

        @Override
        public void onSnap(HandSide handSide, Vector3f localPosition, Vector3f globalPosition) {
            arguments.add(new Arguments(handSide, localPosition, globalPosition));
        }

        public Arguments poll(){
            return arguments.isEmpty() ? null : arguments.remove(0);
        }
    }

    private static class ArgumentRecordingOnUnSnapCallback implements SnapToPoints.OnUnSnapCallback {
        private final List<Arguments> arguments = new ArrayList<>();

        @Override
        public void onUnSnap(HandSide handSide, Vector3f localPosition, Vector3f globalPosition) {
            arguments.add(new Arguments(handSide, localPosition, globalPosition));
        }

        public Arguments poll(){
            return arguments.isEmpty() ? null : arguments.remove(0);
        }
    }

    private record Arguments(HandSide handSide, Vector3f localPosition, Vector3f globalPosition) {}

}
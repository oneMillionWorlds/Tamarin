package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SnapToPointTest{
    private SnapToPoint snapToPoint1 = new SnapToPoint(new Vector3f(1, 1, 1), 2.0f);
    private SnapToPoint snapToPoint2 = new SnapToPoint(new Vector3f(2, 2, 2), 2.0f);
    private SnapToPoint snapToPoint3 = new SnapToPoint(new Vector3f(3, 3, 3), 2.0f);

    Vector3f localPositionOrigin = new Vector3f(100, 1000, 10000);

    private RestrictionUtilities restrictionUtilities = new RestrictionUtilities(
            local -> local.add(localPositionOrigin),
            global -> global.subtract(localPositionOrigin)
    );


    @Test
    void testSnap_NoPoints() {
        SnapToPoints snapToPoints = new SnapToPoints(false, Collections.emptyList());

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities);

        assertFalse(result.isPresent(), "No points to snap to should return empty");
    }

    @Test
    void testSnap_WithinRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(0, 0, 0);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities);

        assertTrue(result.isPresent(), "Position within radius should snap");
        assertEquals(new Vector3f(1, 1, 1), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_OutsideRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2));

        Vector3f position = new Vector3f(5, 5, 5);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities);

        assertFalse(result.isPresent(), "Position outside radius should not snap");
    }

    @Test
    void testSnap_MultiplePointsWithinRadius() {
        SnapToPoints snapToPoints = new SnapToPoints(false, List.of(snapToPoint1, snapToPoint2, snapToPoint3));

        Vector3f position = new Vector3f(2.1f, 2.1f, 2.1f);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities);

        assertTrue(result.isPresent(), "Position within radius of multiple points should snap");
        assertEquals(new Vector3f(2, 2, 2), result.get(), "Position should snap to the closest point");
    }

    @Test
    void testSnap_GlobalMode() {

        SnapToPoint snapToPointGlobal = new SnapToPoint(new Vector3f(100, 1000, 10000), 2.0f);

        SnapToPoints snapToPoints = new SnapToPoints(true, List.of(snapToPointGlobal));

        Vector3f position = new Vector3f(0.5f,0.5f,0.5f);
        Optional<Vector3f> result = snapToPoints.snap(position, restrictionUtilities);

        assertTrue(result.isPresent(), "Position within global radius should snap");
        assertEquals(new Vector3f(0, 0, 0), result.get(), "Position should snap to the transformed closest point (but in local coords)");
    }

}
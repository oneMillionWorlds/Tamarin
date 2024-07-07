package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.List;
import java.util.Optional;

public class SnapToPoints{

    public static final SnapToPoints EMPTY = new SnapToPoints(false, List.of());

    boolean global;

    List<SnapToPoint> points;

    /**
     * @param global If true, the points are in global space, otherwise they are relative to the moved object's parent
     * @param points The points to snap to (and the ranges at which they snap)
     */
    public SnapToPoints(boolean global, List<SnapToPoint> points){
        this.global = global;
        this.points = points;
    }

    /**
     * Given a position, this will snap it to the nearest point in the list of points (if any are within range).
     * If multiple points are within range, the closest one will be chosen.
     * <p>
     * In all cases the argument and return values are in local space (only the defined points may be in global space
     * depending on the options).
     * </p>
     */
    public Optional<Vector3f> snap(Vector3f position, RestrictionUtilities restrictionUtilities){
        Vector3f thisClassModeRelativeUnsnappedPoint;
        if (global){
            thisClassModeRelativeUnsnappedPoint = restrictionUtilities.localPositionToGlobalPosition(position);
        } else {
            thisClassModeRelativeUnsnappedPoint = position;
        }

        Optional<Vector3f> snapToPoint = points.stream()
                        .filter(point -> point.shouldSnap(thisClassModeRelativeUnsnappedPoint))
                        .min((point1, point2) -> Float.compare(
                                point1.distanceSquared(thisClassModeRelativeUnsnappedPoint),
                                point2.distanceSquared(thisClassModeRelativeUnsnappedPoint)
                        )).map(SnapToPoint::getPoint);

        return snapToPoint.map(snapPoint -> global ? restrictionUtilities.globalPositionToLocalPosition(snapPoint) : snapPoint);
    }

}

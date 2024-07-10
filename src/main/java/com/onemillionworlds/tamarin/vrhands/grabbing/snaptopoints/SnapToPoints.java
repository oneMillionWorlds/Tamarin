package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.List;
import java.util.Optional;

public class SnapToPoints{

    public static final SnapToPoints EMPTY = new SnapToPoints(false, List.of());

    private final boolean global;

    private final List<SnapToPoint> points;

    private final OnSnapCallback onSnap;

    private final OnUnSnapCallback onUnsnap;

    private Optional<Vector3f> isSnappedTo = Optional.empty();

    /**
     * @param global If true, the points are in global space, otherwise they are relative to the moved object's parent
     * @param points The points to snap to (and the ranges at which they snap)
     */
    public SnapToPoints(boolean global, List<SnapToPoint> points){
        this(global, points, OnSnapCallback.NO_OP, OnUnSnapCallback.NO_OP);
    }

    /**
     * @param global If true, the points are in global space, otherwise they are relative to the moved object's parent
     * @param points The points to snap to (and the ranges at which they snap)
     * @param onSnap Called when a point is snapped to
     *               (both the global and local snapped to positions are provided)
     * @param onUnsnap Called when a point is unsnapped from
     */
    public SnapToPoints(boolean global, List<SnapToPoint> points, OnSnapCallback onSnap, OnUnSnapCallback onUnsnap){
        this.global = global;
        this.points = points;
        this.onSnap = onSnap;
        this.onUnsnap = onUnsnap;
    }

    /**
     * Given a position, this will snap it to the nearest point in the list of points (if any are within range).
     * If multiple points are within range, the closest one will be chosen.
     * <p>
     * In all cases the argument and return values are in local space (only the defined points may be in global space
     * depending on the options).
     * </p>
     * <p>
     *     Will call onSnap and onUnsnap callbacks as appropriate (so this is a stateful operation).
     * </p>
     */
    public Optional<Vector3f> snap(Vector3f position, RestrictionUtilities restrictionUtilities, HandSide handSide){
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

        Optional<Vector3f> newSnap = snapToPoint.map(snapPoint -> global ? restrictionUtilities.globalPositionToLocalPosition(snapPoint) : snapPoint);

        if(isSnappedTo.isPresent() && (newSnap.isEmpty() || newSnap.get() != isSnappedTo.orElse(null))){
            Vector3f oldSnap = isSnappedTo.orElseThrow();
            onUnsnap.onUnSnap(handSide, oldSnap, restrictionUtilities.localPositionToGlobalPosition(oldSnap));
        }
        if (newSnap.isPresent() && (newSnap.get() != isSnappedTo.orElse(null))){
            onSnap.onSnap(handSide, newSnap.orElseThrow(), restrictionUtilities.localPositionToGlobalPosition(newSnap.orElseThrow()));
        }
        isSnappedTo = newSnap;
        return newSnap;
    }

    public interface OnSnapCallback{
        OnSnapCallback NO_OP = (boundHand, snappedToLocal, snappedToGlobal) -> {};

        void onSnap(HandSide handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal);
    }

    public interface OnUnSnapCallback{
        OnUnSnapCallback NO_OP = (boundHand, snappedToLocal, snappedToGlobal) -> {};

        void onUnSnap(HandSide handGrabbing, Vector3f snappedFromLocal, Vector3f snappedFromGlobal);
    }

}

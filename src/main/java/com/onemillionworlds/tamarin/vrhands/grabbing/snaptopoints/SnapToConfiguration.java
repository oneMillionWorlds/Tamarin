package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SnapToConfiguration{

    public static final SnapToConfiguration EMPTY = new SnapToConfiguration(List.of());

    private final Collection<SnapTarget> snapTargets;

    private final SnapChangeCallback snapChangeCallbacks;

    private SnapTarget isSnappedTo = null;

    /**
     * @param snapTargets The points to snap to (and the ranges at which they snap)
     */
    public SnapToConfiguration(Collection<SnapTarget> snapTargets){
        this(snapTargets, SnapChangeCallback.NO_OP);
    }

    /**
     * @param snapTargets The points to snap to (and the ranges at which they snap)
     * @param callbacks Called when a point is snapped to or unsnapped from
     */
    public SnapToConfiguration( Collection<SnapTarget> snapTargets, SnapChangeCallback callbacks){
        this.snapTargets = snapTargets;
        this.snapChangeCallbacks = callbacks;
    }

    /**
     * Given a position, this will snap it to the nearest point in the list of points (if any are within range).
     * If multiple points are within range, the closest one will be chosen.
     * <p>
     * In all cases the argument and return values are in local space.
     * </p>
     * <p>
     *     Will call onSnap and onUnsnap callbacks as appropriate (so this is a stateful operation).
     * </p>
     */
    public Optional<Vector3f> snap(Vector3f position, RestrictionUtilities restrictionUtilities, BoundHand boundHand){
        if(snapTargets.isEmpty()){
            return Optional.empty();
        }

        SnapTarget newSnappedTarget = null;
        Optional<Vector3f> snapToPoint = Optional.empty();
        double snapDistanceSquared = Double.MAX_VALUE;

        for(SnapTarget snapTarget : snapTargets){
            Optional<Vector3f> snapPoint = snapTarget.shouldSnap(position, restrictionUtilities);
            if(snapPoint.isPresent()){
                double distanceSquared = snapPoint.get().distanceSquared(position);
                if(distanceSquared < snapDistanceSquared){
                    snapToPoint = snapPoint;
                    snapDistanceSquared = distanceSquared;
                    newSnappedTarget = snapTarget;
                }
            }
        }

        Vector3f localSnapPoint = snapToPoint.orElse(null);
        Vector3f globalSnapPoint = localSnapPoint == null ? null : restrictionUtilities.localPositionToGlobalPosition(localSnapPoint);

        if(newSnappedTarget != null && isSnappedTo == null){
            snapChangeCallbacks.onSnapFromUnsnapped(boundHand,localSnapPoint,globalSnapPoint);
        }
        if(newSnappedTarget == null && isSnappedTo != null){
            snapChangeCallbacks.onUnsnapFromSnapped(boundHand);
        }
        if(newSnappedTarget != null && isSnappedTo != null && newSnappedTarget!=isSnappedTo){
            snapChangeCallbacks.onSnapTransfer(boundHand,localSnapPoint,globalSnapPoint);
        }
        if(newSnappedTarget !=null){
            snapChangeCallbacks.onSnapContinues(boundHand,localSnapPoint,globalSnapPoint);
        }

        isSnappedTo = newSnappedTarget;
        return snapToPoint;
    }

}

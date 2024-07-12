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

    private final OnSnapCallback onSnap;

    private final OnUnSnapCallback onUnsnap;

    private SnapTarget isSnappedTo = null;

    /**
     * @param snapTargets The points to snap to (and the ranges at which they snap)
     */
    public SnapToConfiguration(Collection<SnapTarget> snapTargets){
        this(snapTargets, OnSnapCallback.NO_OP, OnUnSnapCallback.NO_OP);
    }

    /**
     * @param snapTargets The points to snap to (and the ranges at which they snap)
     * @param onSnap Called when a point is snapped to
     *               (both the global and local snapped to positions are provided)
     * @param onUnsnap Called when a point is unsnapped from
     */
    public SnapToConfiguration( Collection<SnapTarget> snapTargets, OnSnapCallback onSnap, OnUnSnapCallback onUnsnap){
        this.snapTargets = snapTargets;
        this.onSnap = onSnap;
        this.onUnsnap = onUnsnap;
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
                snapToPoint = snapPoint;
                double distanceSquared = snapPoint.get().distanceSquared(position);
                if(distanceSquared < snapDistanceSquared){
                    snapDistanceSquared = distanceSquared;
                    newSnappedTarget = snapTarget;
                }
            }
        }

        boolean newSnap = newSnappedTarget != null && newSnappedTarget!=isSnappedTo;
        boolean releasedSnap = isSnappedTo!=null && (newSnappedTarget == null || newSnappedTarget!=isSnappedTo);

        if(releasedSnap){
            onUnsnap.onUnSnap(boundHand);
        }
        if (newSnap){
            onSnap.onSnap(boundHand, snapToPoint.orElseThrow(), restrictionUtilities.localPositionToGlobalPosition(snapToPoint.orElseThrow()));
        }
        isSnappedTo = newSnappedTarget;
        return snapToPoint;
    }

    public interface OnSnapCallback{
        OnSnapCallback NO_OP = (boundHand, snappedToLocal, snappedToGlobal) -> {};

        void onSnap(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal);
    }

    public interface OnUnSnapCallback{
        OnUnSnapCallback NO_OP = (boundHand) -> {};

        void onUnSnap(BoundHand handGrabbing);
    }

    public static SnapSimpleCallback simpleCallback(Consumer<BoundHand> callback){
        return callback::accept;
    }

    public interface SnapSimpleCallback extends OnSnapCallback, OnUnSnapCallback{
        void callback(BoundHand hand);

        @Override
        default void onSnap(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
            callback(handGrabbing);
        }

        @Override
        default void onUnSnap(BoundHand handGrabbing){
            callback(handGrabbing);
        }
    }

}

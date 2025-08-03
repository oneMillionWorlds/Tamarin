package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;

/**
 * Restricts the move target (often, but not always the spatial this control is attached to) to a box.
 * <p>
 * The move target will remain within that path while grabbing. The box is relative to the move target's parent.
 * I.e. it is in local coordinates not world coordinates.
 * </p>
 */
public class RestrictToGlobalBox implements GrabMoveRestriction{

    private final Vector3f min;
    private final Vector3f max;

    public RestrictToGlobalBox(Vector3f globalMin, Vector3f globalMax){
        this.min = globalMin;
        this.max = globalMax;
    }

    @Override
    public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        Vector3f naturalPositionGlobal = restrictionUtilities.localPositionToGlobalPosition(naturalPositionLocal);

        return restrictionUtilities.globalPositionToLocalPosition(new Vector3f(
            Math.min(max.x, Math.max(min.x, naturalPositionGlobal.x)),
            Math.min(max.y, Math.max(min.y, naturalPositionGlobal.y)),
            Math.min(max.z, Math.max(min.z, naturalPositionGlobal.z))
        ));
    }
}

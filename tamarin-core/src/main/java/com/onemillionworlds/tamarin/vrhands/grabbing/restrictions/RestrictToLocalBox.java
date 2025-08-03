package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.math.Line3f;

/**
 * Restricts the move target (often, but not always the spatial this control is attached to) to a box.
 * <p>
 * The move target will remain within that path while grabbing. The box is relative to the move target's parent.
 * I.e. it is in local coordinates not world coordinates.
 * </p>
 */
public class RestrictToLocalBox implements GrabMoveRestriction{

    private final Vector3f min;
    private final Vector3f max;

    public RestrictToLocalBox(Vector3f localMin, Vector3f localMax){
        this.min = localMin;
        this.max = localMax;
    }

    @Override
    public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        return new Vector3f(
            Math.min(max.x, Math.max(min.x, naturalPositionLocal.x)),
            Math.min(max.y, Math.max(min.y, naturalPositionLocal.y)),
            Math.min(max.z, Math.max(min.z, naturalPositionLocal.z))
        );
    }
}

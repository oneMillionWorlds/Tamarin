package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.math.Line3f;

/**
 * Restricts the move target (often, but not always the spatial this control is attached to) to a line in space.
 * <p>
 * The move target will remain on that path. The path is relative to the move target's parent.
 * I.e. it is in local coordinates not world coordinates.
 * </p>
 */
public class RestrictToLocalLine implements GrabMoveRestriction{

    private final Line3f restrictToLine;

    public RestrictToLocalLine(Line3f restrictToLine){
        this.restrictToLine = restrictToLine;
    }

    @Override
    public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        return restrictToLine.findPointOfClosedApproach(naturalPositionLocal);
    }
}

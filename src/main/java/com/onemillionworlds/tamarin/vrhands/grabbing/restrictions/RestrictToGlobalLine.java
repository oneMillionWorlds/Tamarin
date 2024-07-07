package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.math.Line3f;

/**
 * Restricts the move target (often, but not always the spatial this control is attached to) to a line in space.
 * <p>
 * The move target will remain on that path. The path is relative to world coordinates.
 * </p>
 */
public class RestrictToGlobalLine implements GrabMoveRestriction{

    private final Line3f restrictToLine;

    public RestrictToGlobalLine(Line3f restrictToLine){
        this.restrictToLine = restrictToLine;
    }

    public RestrictToGlobalLine(Vector3f min, Vector3f max){
        this(new Line3f(min, max));
    }

    @Override
    public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        return restrictionUtilities.globalPositionToLocalPosition(restrictToLine.findPointOfClosedApproach(restrictionUtilities.localPositionToGlobalPosition(naturalPositionLocal)));
    }
}

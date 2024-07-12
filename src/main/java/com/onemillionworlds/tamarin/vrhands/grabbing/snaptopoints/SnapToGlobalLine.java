package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.math.Line3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Optional;

public class SnapToGlobalLine implements SnapTarget{
    private final Line3f snapToLine;

    private final float distanceToSnapAtSquared;

    public SnapToGlobalLine(Line3f snapToLine, float distanceToSnapAt){
        this.snapToLine = snapToLine;
        this.distanceToSnapAtSquared = distanceToSnapAt * distanceToSnapAt;

    }

    public SnapToGlobalLine(Vector3f minPosition, Vector3f maxPosition, float distanceToSnapAt){
        this(new Line3f(minPosition, maxPosition), distanceToSnapAt);
    }

    @Override
    public Optional<Vector3f> shouldSnap(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        Line3f snapToLineLocal = new Line3f(restrictionUtilities.globalPositionToLocalPosition(snapToLine.start), restrictionUtilities.globalPositionToLocalPosition(snapToLine.end));
        Vector3f pointOfClosedApproach = snapToLineLocal.findPointOfClosedApproach(naturalPositionLocal);
        if (pointOfClosedApproach.distanceSquared(naturalPositionLocal) < distanceToSnapAtSquared){
            return Optional.of(pointOfClosedApproach);
        }else{
            return Optional.empty();
        }
    }

}

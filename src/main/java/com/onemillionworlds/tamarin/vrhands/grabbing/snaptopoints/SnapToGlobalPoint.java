package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Optional;

public class SnapToGlobalPoint implements SnapTarget{
    private final Vector3f point;
    private final float radiusToSnapAtSquared;

    public SnapToGlobalPoint(Vector3f point, float radiusToSnapAt){
        this.point = point;
        this.radiusToSnapAtSquared = radiusToSnapAt *  radiusToSnapAt;
    }

    @Override
    public Optional<Vector3f> shouldSnap(Vector3f position, RestrictionUtilities restrictionUtilities){
        Vector3f pointLocal = restrictionUtilities.globalPositionToLocalPosition(point);

        if (position.distanceSquared(pointLocal) < radiusToSnapAtSquared){
            return Optional.of(pointLocal);
        }else{
            return Optional.empty();
        }
    }

    private float distanceSquared(Vector3f position){
        return point.distanceSquared(position);
    }
}

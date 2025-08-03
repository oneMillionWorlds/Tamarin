package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Optional;

public class SnapToLocalPoint extends SnapTarget{
    private final Vector3f point;
    private final float radiusToSnapAtSquared;

    public SnapToLocalPoint(Vector3f point, float radiusToSnapAt){
        this.point = point;
        this.radiusToSnapAtSquared = radiusToSnapAt *  radiusToSnapAt;
    }

    @Override
    public Optional<Vector3f> shouldSnap(Vector3f position, RestrictionUtilities restrictionUtilities){
        if (distanceSquared(position) < radiusToSnapAtSquared){
            return Optional.of(point);
        }else{
            return Optional.empty();
        }
    }

    private float distanceSquared(Vector3f position){
        return point.distanceSquared(position);
    }
}

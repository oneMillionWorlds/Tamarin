package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;

public class SnapToPoint{
    private final Vector3f point;
    private final float radiusToSnapAtSquared;

    public SnapToPoint(Vector3f point, float radiusToSnapAt){
        this.point = point;
        this.radiusToSnapAtSquared = radiusToSnapAt *  radiusToSnapAt;
    }

    public boolean shouldSnap(Vector3f position){
        return distanceSquared(position) < radiusToSnapAtSquared;
    }

    public float distanceSquared(Vector3f position){
        return point.distanceSquared(position);
    }

    public Vector3f getPoint(){
        return point;
    }
}

package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;

import java.util.function.Function;

public class RestrictionUtilities{

    private final Function<Vector3f,Vector3f> localPositionToGlobalPosition;

    private final Function<Vector3f,Vector3f> globalPositionToLocalPosition;

    public RestrictionUtilities(Function<Vector3f, Vector3f> localPositionToGlobalPosition, Function<Vector3f, Vector3f> globalPositionToLocalPosition){
        this.localPositionToGlobalPosition = localPositionToGlobalPosition;
        this.globalPositionToLocalPosition = globalPositionToLocalPosition;
    }

    public Vector3f localPositionToGlobalPosition(Vector3f localPosition){
        return localPositionToGlobalPosition.apply(localPosition);
    }

    public Vector3f globalPositionToLocalPosition(Vector3f localPosition){
        return globalPositionToLocalPosition.apply(localPosition);
    }

}

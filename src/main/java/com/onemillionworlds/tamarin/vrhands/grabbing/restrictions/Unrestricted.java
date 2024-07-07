package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;

public class Unrestricted implements GrabMoveRestriction{

    public static Unrestricted INSTANCE = new Unrestricted();

    @Override
    public Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities){
        return naturalPositionLocal;
    }
}

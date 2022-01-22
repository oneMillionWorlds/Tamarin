package com.onemillionworlds.tamarin.compatibility;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class BoneStance{

    public Vector3f position;
    public Quaternion orientation;

    public BoneStance(Vector3f position, Quaternion orientation){
        this.position = position;
        this.orientation = orientation;
    }
}

package com.onemillionworlds.tamarin.math;

import com.jme3.math.Vector3f;

public class RotationalVelocity{

    /**
     * The rotational velocity expressed as an axis the rotation is about with its length proportional to the
     * rate at which it is rotating
     */
    final Vector3f rotationAsSingleVector;

    /**
     * The axis about which the rotation is occurring
     */
    final Vector3f axis;

    /**
     * The rate (in radians per second) at which the rotation is occurring
     */
    final float angularVelocity;

    public RotationalVelocity(Vector3f rotationAsSingleVector){
        this.rotationAsSingleVector = rotationAsSingleVector;
        if (Double.isNaN(rotationAsSingleVector.x) || Double.isNaN(rotationAsSingleVector.y) || Double.isNaN(rotationAsSingleVector.z)){
            throw new RuntimeException("NaN rotation encountered");
        }
        angularVelocity = this.rotationAsSingleVector.length();
        if (angularVelocity == 0){
            axis = new Vector3f(0,0,0);
        }else{
            axis = rotationAsSingleVector.normalize();
        }
    }

    public Vector3f getRotationAsSingleVector(){
        return rotationAsSingleVector;
    }

    public Vector3f getAxis(){
        return axis;
    }

    public float getAngularVelocity(){
        return angularVelocity;
    }
}

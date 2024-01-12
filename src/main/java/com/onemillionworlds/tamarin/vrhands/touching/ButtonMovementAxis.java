package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.math.Vector3f;

public enum ButtonMovementAxis{
    X(new Vector3f(1,0,0), true, Axis.X),
    Y(new Vector3f(0,1,0), true, Axis.Y),
    Z(new Vector3f(0,0,1), true, Axis.Z),
    NEGATIVE_X(new Vector3f(-1,0,0), false, Axis.X),
    NEGATIVE_Y(new Vector3f(0,-1,0), false, Axis.Y),
    NEGATIVE_Z(new Vector3f(0,0,-1), false, Axis.Z);

    public final Vector3f axisVector;
    public final boolean isPositive;

    public final Axis axis;

    public float extract(Vector3f vector){
        return (isPositive?1:-1)*axis.extract(vector);
    }

    ButtonMovementAxis(Vector3f axisVector, boolean isPositive, Axis axis){
        this.axisVector = axisVector;
        this.isPositive = isPositive;
        this.axis = axis;
    }

    public static enum Axis{
        X, Y, Z;

        float extract(Vector3f vector){
            return switch(this){
                case X -> vector.x;
                case Y -> vector.y;
                case Z -> vector.z;
                default -> throw new IllegalStateException();
            };
        }
    }

}

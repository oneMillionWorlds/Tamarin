package com.onemillionworlds.tamarin.actions.state;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class PoseActionState{

    private final Vector3f position;

    private final Quaternion orientation;

    /**
     * velocity (in tracker space) of the pose (hand position)
     */
    private final Vector3f velocity;

    /**
     * angular velocity of the pose (hand position), as euler angles
     */
    private final Vector3f angularVelocity;

    private final boolean velocityAvailable;

    public PoseActionState(Vector3f position, Quaternion orientation){
        this.position = position;
        this.orientation = orientation;
        this.velocity = Vector3f.ZERO;
        this.angularVelocity = Vector3f.ZERO;
        this.velocityAvailable = false;
    }

    public PoseActionState(Vector3f position, Quaternion orientation, Vector3f velocity, Vector3f angularVelocity){
        this.position = position;
        this.orientation = orientation;
        this.velocity = velocity;
        this.angularVelocity = angularVelocity;
        this.velocityAvailable = true;
    }

    public Vector3f position(){
        return position;
    }

    public Quaternion orientation(){
        return orientation;
    }

    public Vector3f velocity(){
        return velocity;
    }

    public Vector3f angularVelocity(){
        return angularVelocity;
    }

    /**
     * Depending on if the device supports velocity or not, this will return true or false. If this is false, the velocity
     * data will be zeroed.
     * @return
     */
    public boolean isVelocityAvailable(){
        return velocityAvailable;
    }
}

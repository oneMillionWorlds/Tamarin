package com.onemillionworlds.tamarin.compatibility;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class PoseActionState{

    /**
     * The raw pose (hand position), this includes both position and orientation.
     *
     * It is relative to the observer!
     */
    private final Matrix4f rawPose;

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

    public PoseActionState(Matrix4f rawPose, Vector3f position, Quaternion orientation, Vector3f velocity, Vector3f angularVelocity){
        this.rawPose = rawPose;
        this.position = position;
        this.orientation = orientation;
        this.velocity = velocity;
        this.angularVelocity = angularVelocity;
    }

    public Vector3f getPosition(){
        return position;
    }

    public Quaternion getOrientation(){
        return orientation;
    }
}

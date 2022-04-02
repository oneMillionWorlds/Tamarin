package com.onemillionworlds.tamarin.compatibility;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import lombok.Getter;

@Getter
public class ObserverRelativePoseActionState extends PoseActionState{

    /**
     * The pose data, but relative to the world origin (rather than the observer origin)
     */
    private final PoseActionState poseActionState_worldRelative;

    public ObserverRelativePoseActionState(Matrix4f rawPose, Vector3f position, Quaternion orientation, Vector3f velocity, Vector3f angularVelocity, PoseActionState poseActionState_worldRelative){
        super(rawPose, position, orientation, velocity, angularVelocity);
        this.poseActionState_worldRelative = poseActionState_worldRelative;
    }

}

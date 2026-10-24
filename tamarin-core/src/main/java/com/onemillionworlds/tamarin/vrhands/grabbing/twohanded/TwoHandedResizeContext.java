package com.onemillionworlds.tamarin.vrhands.grabbing.twohanded;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * What a {@link TwoHandedResizing} is told each frame. All "on target" points are in the target's local coordinates as
 * they were when the second hand grabbed (i.e. before any resizing).
 */
public final class TwoHandedResizeContext{

    private final Spatial target;
    private final float handSeparationRatio;
    private final Vector3f startGraspPointOnTarget;
    private final Vector3f startPrimaryHandPointOnTarget;
    private final Vector3f startSecondaryHandPointOnTarget;
    private final Vector3f startTargetLocalScale;

    public TwoHandedResizeContext(Spatial target, float handSeparationRatio, Vector3f startGraspPointOnTarget, Vector3f startPrimaryHandPointOnTarget, Vector3f startSecondaryHandPointOnTarget, Vector3f startTargetLocalScale){
        this.target = target;
        this.handSeparationRatio = handSeparationRatio;
        this.startGraspPointOnTarget = startGraspPointOnTarget;
        this.startPrimaryHandPointOnTarget = startPrimaryHandPointOnTarget;
        this.startSecondaryHandPointOnTarget = startSecondaryHandPointOnTarget;
        this.startTargetLocalScale = startTargetLocalScale;
    }

    /**
     * The spatial being moved (the move target, which is usually but not always the spatial the control is on).
     */
    public Spatial getTarget(){
        return target;
    }

    /**
     * The distance between the hands now divided by the distance when the second hand grabbed (so greater than 1 as
     * the hands move apart).
     */
    public float getHandSeparationRatio(){
        return handSeparationRatio;
    }

    /**
     * The point that was midway between the hands when the second hand grabbed.
     */
    public Vector3f getStartGraspPointOnTarget(){
        return startGraspPointOnTarget.clone();
    }

    /**
     * Where the hand that grabbed first was when the second hand grabbed.
     */
    public Vector3f getStartPrimaryHandPointOnTarget(){
        return startPrimaryHandPointOnTarget.clone();
    }

    /**
     * Where the hand that grabbed second was when it grabbed.
     */
    public Vector3f getStartSecondaryHandPointOnTarget(){
        return startSecondaryHandPointOnTarget.clone();
    }

    /**
     * The target's local scale when the second hand grabbed.
     */
    public Vector3f getStartTargetLocalScale(){
        return startTargetLocalScale.clone();
    }
}

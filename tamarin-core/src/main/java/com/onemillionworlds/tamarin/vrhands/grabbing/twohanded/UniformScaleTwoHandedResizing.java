package com.onemillionworlds.tamarin.vrhands.grabbing.twohanded;

import com.jme3.math.Vector3f;

/**
 * Scales the target's spatial uniformly by how much the hands have moved apart or together, so the points of the
 * target under each hand stay under that hand.
 */
public class UniformScaleTwoHandedResizing implements TwoHandedResizing{

    public static final UniformScaleTwoHandedResizing INSTANCE = new UniformScaleTwoHandedResizing();

    @Override
    public Vector3f resize(TwoHandedResizeContext context){
        context.getTarget().setLocalScale(context.getStartTargetLocalScale().multLocal(context.getHandSeparationRatio()));
        return context.getStartGraspPointOnTarget();
    }
}

package com.onemillionworlds.tamarin.vrhands.grabbing.twohanded;

import com.jme3.math.Vector3f;

/**
 * The default: the hands moving apart or together does nothing to the target.
 */
public class NoTwoHandedResizing implements TwoHandedResizing{

    public static final NoTwoHandedResizing INSTANCE = new NoTwoHandedResizing();

    @Override
    public Vector3f resize(TwoHandedResizeContext context){
        return context.getStartGraspPointOnTarget();
    }
}

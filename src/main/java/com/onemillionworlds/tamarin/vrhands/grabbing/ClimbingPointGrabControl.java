package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.ClimbSupport;

/**
 * A spatial that has a ClimbingPointGrabControl added to it is a point that can be grabbed by the player
 * and then the player can pull themselves up using it. The rungs of a ladder or hand holds on a cliff might have
 * ClimbingPointGrabControl points added to them
 */
public class ClimbingPointGrabControl extends AbstractGrabControl{

    @Override
    public void onGrab(BoundHand grabbedByHand){
        super.onGrab(grabbedByHand);
        grabbedByHand.getFunctionOpt(ClimbSupport.class).ifPresent(climbSupport -> {
            climbSupport.setGrabStartPosition(new Vector3f(grabbedByHand.getHandNode_xPointing().getWorldTranslation()));
        });
    }

    @Override
    public void onRelease(BoundHand handUnbindingFrom){
        super.onRelease(handUnbindingFrom);
        handUnbindingFrom.getFunctionOpt(ClimbSupport.class).ifPresent(climbSupport -> {
            climbSupport.setGrabStartPosition(null);
        });
    }

    @Override
    protected void controlUpdate(float tpf){}

}

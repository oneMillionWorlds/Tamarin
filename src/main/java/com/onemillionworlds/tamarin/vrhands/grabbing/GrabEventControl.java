package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * This is a grab control that just runs some code when the control is grabbed.
 *
 * Note, no actual "grabbing" takes place, the object is not moved
 *
 * ("Some code" provided as a lambda in the constructor)
 */
public class GrabEventControl extends AbstractGrabControl{

    Runnable onGrabEvent;

    public GrabEventControl(Runnable onGrabEvent){
        this.onGrabEvent = onGrabEvent;
    }

    public void setOnGrabEvent(Runnable onGrabEvent){
        this.onGrabEvent = onGrabEvent;
    }

    @Override
    public void onGrab(BoundHand grabbedByHand){
        super.onGrab(grabbedByHand);
        onGrabEvent.run();
    }

    @Override
    protected void controlUpdate(float tpf){

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){

    }
}

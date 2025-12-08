package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import java.util.Optional;

/**
 * This is a control that allows for simple hand grab actions. If a bound hand has been given a grab action then every
 * update it will (if the grab action is pulled) look for spatials with a grab control (it will search up parent trees)
 * and if it has one it will bind to it until the action is released
 * <p>
 * This control allows for grab interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 * </p>
 * Extend this class to get access to onGrab and onRelease, make sure to call super
 */
@SuppressWarnings("unused")
public abstract class AbstractGrabControl extends AbstractControl{


    Optional<BoundHand> grabbingHand = Optional.empty();

    @OverridingMethodsMustInvokeSuper
    public void onGrab(BoundHand grabbedByHand ){
        grabbingHand = Optional.of(grabbedByHand);
    }

    @OverridingMethodsMustInvokeSuper
    public void onRelease(BoundHand handUnbindingFrom){
        grabbingHand = Optional.empty();
    }

    public boolean isGrabbed(){
        return grabbingHand.isPresent();
    }

    public Optional<BoundHand> getGrabbingHand(){
        return grabbingHand;
    }

    /**
     * If this control is currently accepting grabs from that hand
     * <p>
     *  Gives subclasses the opportunity to filter grabs
     * </p>
     * <p>
     *     An example of when you might use this is if different hands are used for different purposes and only one
     *     hand should be able to grab this.
     * </p>
     */
    public boolean isCurrentlyGrabbable(BoundHand grabbedByHand){
        return true;
    }

    @Override protected void controlRender(RenderManager rm, ViewPort vp){}
}

package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.Optional;

/**
 * This is a control that allows for simple hand grab actions. If a bound hand has been given a grab action then every
 * update it will (if the grab action is pulled) look for spatials with a grab control (it will search up parent trees)
 * and if it has one it will bind to it until the action is released
 *
 * This control allows for grab interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 *
 * Extend this class to get access to onGrab and onRelease, make sure to call super
 */
public abstract class AbstractGrabControl extends AbstractControl{


    Optional<BoundHand> grabbingHand = Optional.empty();


    public void onGrab(BoundHand grabbedByHand ){
        grabbingHand = Optional.of(grabbedByHand);
    }

    public void onRelease(){
        grabbingHand = Optional.empty();
    }

    public boolean isGrabbed(){
        return grabbingHand.isPresent();
    }

    public Optional<BoundHand> getGrabbingHand(){
        return grabbingHand;
    }

}

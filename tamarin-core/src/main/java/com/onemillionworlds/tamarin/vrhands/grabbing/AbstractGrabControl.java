package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This is a control that allows for simple hand grab actions. If a bound hand has been given a grab action then every
 * update it will (if the grab action is pulled) look for spatials with a grab control (it will search up parent trees)
 * and if it has one it will bind to it until the action is released
 * <p>
 * This control allows for grab interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 * </p>
 * <p>
 * More than one hand may hold the control at once. Each hand is tracked independently, so releasing one hand leaves
 * the control held by the other. The hand that grabbed first is the primary hand (see {@link #getGrabbingHand()});
 * if it lets go the next hand to have grabbed becomes the primary hand.
 * </p>
 * Extend this class to get access to onGrab and onRelease, make sure to call super
 */
@SuppressWarnings("unused")
public abstract class AbstractGrabControl extends AbstractControl{

    private final List<BoundHand> grabbingHands = new ArrayList<>(2);

    /**
     * Called each time a hand grabs this control, including when another hand is already holding it.
     */
    @OverridingMethodsMustInvokeSuper
    public void onGrab(BoundHand grabbedByHand ){
        if(!grabbingHands.contains(grabbedByHand)){
            grabbingHands.add(grabbedByHand);
        }
    }

    /**
     * Called each time a hand releases this control. Other hands may still be holding it, see {@link #isGrabbed()}.
     */
    @OverridingMethodsMustInvokeSuper
    public void onRelease(BoundHand handUnbindingFrom){
        grabbingHands.remove(handUnbindingFrom);
    }

    /**
     * @return true if at least one hand is holding this control.
     */
    public boolean isGrabbed(){
        return !grabbingHands.isEmpty();
    }

    /**
     * @return true if the given hand is (one of the hands) holding this control.
     */
    public boolean isGrabbedBy(BoundHand hand){
        return grabbingHands.contains(hand);
    }

    /**
     * The primary grabbing hand: the one that has been holding this control the longest.
     */
    public Optional<BoundHand> getGrabbingHand(){
        return grabbingHands.isEmpty() ? Optional.empty() : Optional.of(grabbingHands.get(0));
    }

    /**
     * Every hand currently holding this control, in the order they grabbed it (so the primary hand first).
     */
    public List<BoundHand> getGrabbingHands(){
        return Collections.unmodifiableList(grabbingHands);
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

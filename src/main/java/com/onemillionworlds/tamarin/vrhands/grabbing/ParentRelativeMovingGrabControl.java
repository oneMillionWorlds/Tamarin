package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
 * <p>
 * HOWEVER, the spatial this is attached to accepts the grab, but it is a moveTarget spatial that is actually moved
 * (The move target should probably be a parent of the spatial this is attached to inorder to get sensible behaviour)
 * <p>
 * This control allows for simple interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 * <p>
 * Unlike {@link SnapToHandGrabControl} this does not "snap to hand" so if more useful for things that feel like
 * "moving UI items" and less like physics objects
 * <p>
 * This class is intended to be attached to handles but move the entire object (handle and all)
 */
public class ParentRelativeMovingGrabControl extends AbstractRelativeMovingGrabControl{

    Spatial moveTarget;

    /**
     * This grab control will move the passed moveTarget but will only accept grabs on the spatial the control is
     * attached to.
     * <p>
     * This control is intended to be attached to handles but move the entire object (handle and all)
     *
     * @param moveTarget the Spatial that will be moved in response to grabs on the spatial this control is attached to
     */
    public ParentRelativeMovingGrabControl(Spatial moveTarget){
        this.moveTarget = moveTarget;
    }

    @Override
    Spatial getMoveTargetSpatial(){
        return moveTarget;
    }
}

package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
 * <p>
 * This control allows for simple interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 * <p>
 * Unlike {@link SnapToHandGrabControl} this does not "snap to hand" so if more useful for things that feel like
 * "moving UI items" and less like physics objects
 * <p>
 * Extend this class to get access to onGrab and onRelease, make sure to call super
 */
public class RelativeMovingGrabControl extends AbstractRelativeMovingGrabControl{

    @Override
    Spatial getMoveTargetSpatial(){
        return spatial;
    }
}

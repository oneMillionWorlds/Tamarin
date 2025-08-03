package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * Callback for changes in snap state
 */
public abstract class SnapChangeCallback{

    public static SnapChangeCallback NO_OP = new SnapChangeCallback(){
    };

    /**
     * Called when the object is snapped to a point while it was previously freely moving.
     *
     * <p>Note that this will not be called on an ongoing basis as the snap point changes, only once as the snap forms</p>
     *
     * @param handGrabbing    The hand that is grabbing the object
     * @param snappedToLocal  The position the object was snapped to (in local space)
     * @param snappedToGlobal The position the object was snapped to (in global space)
     */
    public void onSnapFromUnsnapped(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
    }

    /**
     * Called when the object is unsnapped from a point while it was previously snapped and is now moving freely again
     *
     * @param handGrabbing The hand that is grabbing the object
     */
    public void onUnsnapFromSnapped(BoundHand handGrabbing){
    }

    /**
     * Called when the object is already snapped to a point but moves to a new SnapToTarget
     *
     * @param handGrabbing    The hand that is grabbing the object
     * @param snappedToLocal  The position the object was snapped to (in local space)
     * @param snappedToGlobal The position the object was snapped to (in global space)
     */
    public void onSnapTransfer(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
    }

    /**
     * Called every frame that a snap is ongoing (irrespective of whether the snap point is changing, is new, or is the same as the previous frame)
     *
     * @param handGrabbing    The hand that is grabbing the object
     * @param snappedToLocal  The position the object was snapped to (in local space)
     * @param snappedToGlobal The position the object was snapped to (in global space)
     */
    public void onSnapContinues(BoundHand handGrabbing, Vector3f snappedToLocal, Vector3f snappedToGlobal){
    }
}

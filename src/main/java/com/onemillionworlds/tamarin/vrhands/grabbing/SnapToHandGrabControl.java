package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
 *
 * It should be built such that the +X direction is the direction that faces forwards when held.
 *
 * This control allows for simple interaction but is entirely optional, raw {@link BoundHand#pickPalm} can be used and
 * your own grabbing implementation.
 *
 * Extend this class to get access to onGrab and onRelease, make sure to call super
 *
 * Note that this control expects that the spatial is either directly attached to the root node, or is attached to a node
 * that is at (0,0,0) and unrotated (basically it does all its work with local translations)
 *
 * Note that this grab control has a "snap to hand on grab" behaviour. If you don't want that use the
 * {@link AbstractRelativeMovingGrabControl}
 */
public class SnapToHandGrabControl extends AbstractGrabControl{

    private Vector3f inSpatialHoldCentre;
    private float grabPointDistanceFromSkin;

    /**
     * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
     *
     * Parameters to this constructor control where the object sits when it's in the hand. The parameters are broken up
     * like this so that it can be held correctly in either the left or right hand
     *
     * @param inSpatialHoldCentre This is a point that should be above the centre of the palm. The point should be in the centre of your spatial local coordinate system in the z axis
     * @param grabPointDistanceFromSkin how far the inSpatialGrabPoint is from the skin of the hand.
     */
    public SnapToHandGrabControl(Vector3f inSpatialHoldCentre, float grabPointDistanceFromSkin){
        this.inSpatialHoldCentre = inSpatialHoldCentre;
        this.grabPointDistanceFromSkin = grabPointDistanceFromSkin;
    }

    @Override
    protected void controlUpdate(float tpf){
        getGrabbingHand().ifPresent(grabbingHand -> {
            Spatial spatial = getSpatial();

            spatial.setLocalRotation(grabbingHand.getHoldRotation().clone());

            Vector3f currentPosition = spatial.localToWorld(inSpatialHoldCentre, null);
            Vector3f targetPosition = grabbingHand.getHoldPosition(grabPointDistanceFromSkin);
            spatial.setLocalTranslation(spatial.getWorldTranslation().add(targetPosition.subtract(currentPosition)));
        });
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){

    }
}

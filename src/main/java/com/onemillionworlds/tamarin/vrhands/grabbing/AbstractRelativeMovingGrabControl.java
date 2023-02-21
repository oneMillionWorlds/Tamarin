package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.math.Line3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.Optional;


public abstract class AbstractRelativeMovingGrabControl extends AbstractGrabControl{

    Vector3f startTargetPosition;
    Vector3f startHandPosition;
    Vector3f handToTargetOffset;
    Quaternion startTargetRotation;
    Quaternion startHandRotation;

    Optional<Line3f> restrictToLine = Optional.empty();

    private boolean shouldApplyRotation = true;

    /**
     * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
     * <p>
     * Parameters to this constructor control where the object sits when it's in the hand. The parameters are broken up
     * like this so that it can be held correctly in either the left or right hand
     *
     */
    public AbstractRelativeMovingGrabControl(){

    }

    abstract Spatial getMoveTargetSpatial();

    @Override
    protected void controlUpdate(float tpf){

        Optional<BoundHand> grabbingHandOpt = getGrabbingHand();

        if (grabbingHandOpt.isPresent()){
            BoundHand hand = grabbingHandOpt.get();

            Spatial moveTargetSpatial = getMoveTargetSpatial();
            Node targetParent = moveTargetSpatial.getParent();
            if (startTargetPosition == null){
                startTargetPosition = targetParent.worldToLocal(moveTargetSpatial.getWorldTranslation(), null);
                startTargetRotation = targetParent.getWorldRotation().inverse().mult(new Quaternion(moveTargetSpatial.getWorldRotation()));
                startHandPosition = targetParent.worldToLocal(new Vector3f(hand.getHandNode_xPointing().getWorldTranslation()), null);
                startHandRotation = targetParent.getWorldRotation().inverse().mult(new Quaternion(hand.getHandNode_xPointing().getWorldRotation()));
                handToTargetOffset = startTargetPosition.subtract(startHandPosition);
            }
            Vector3f currentHandPosition = targetParent.worldToLocal(hand.getHandNode_xPointing().getWorldTranslation(), null);
            Quaternion currentHandRotation = targetParent.getWorldRotation().inverse().mult(hand.getHandNode_xPointing().getWorldRotation());

            Vector3f bulkMotion = currentHandPosition.subtract(startHandPosition);

            Quaternion changeInRotation = getQuaternionFromTo(startHandRotation, currentHandRotation);

            System.out.println(bulkMotion);

            if (this.shouldApplyRotation){
                Vector3f rotationInducedMotion = changeInRotation.mult(handToTargetOffset).subtract(handToTargetOffset);
                moveTargetSpatial.setLocalRotation(changeInRotation.mult(startTargetRotation));
                moveTargetSpatial.setLocalTranslation(applyLineRestriction(startTargetPosition.add(bulkMotion).add(rotationInducedMotion)));
            }else{
                moveTargetSpatial.setLocalTranslation(applyLineRestriction(startTargetPosition.add(bulkMotion)));
            }
        }else if (startTargetPosition !=null){
            startTargetPosition = null;
            startHandPosition = null;
            handToTargetOffset = null;
            startTargetRotation = null;
            startHandRotation = null;
        }
    }

    private Vector3f applyLineRestriction(Vector3f unrestrictedPosition){
        return restrictToLine.map(l -> l.findPointOfClosedApproach(unrestrictedPosition)).orElse(unrestrictedPosition);
    }

    /**
     * Restricts the move target (often, but not always the spatial this control is attached to).
     * <p>
     * The move target will remain on that path. The path is relative to the move target's parent
     */
    public void restrictToPath(Vector3f startPoint, Vector3f endPoint){
        restrictToLine = Optional.of(new Line3f(startPoint, endPoint));
    }

    /**
     * @param shouldApplyRotation if the rotation of the hand should rotate the spatial
     */
    public void setShouldApplyRotation( boolean shouldApplyRotation ){
        this.shouldApplyRotation = shouldApplyRotation;
    }

    /**
     *
     * This method returns a QuaternionD that would take you from the initial rotation
     * to the final rotation.
     */
    private static Quaternion getQuaternionFromTo(Quaternion from, Quaternion to){
        //see https://stackoverflow.com/a/22167097/2187042
        return to.mult(from.inverse());

    }
}

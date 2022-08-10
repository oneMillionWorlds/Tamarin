package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.Optional;


public abstract class AbstractRelativeMovingGrabControl extends AbstractGrabControl{

    Vector3f startTargetPosition;
    Vector3f startHandPosition;
    Vector3f handToTargetOffset;
    Quaternion startTargetRotation;
    Quaternion startHandRotation;

    /**
     * This is a control that allows for simple hand grab. When grabbed the object will be moved around with the hand.
     *
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

            if (startTargetPosition == null){
                startTargetPosition = new Vector3f(moveTargetSpatial.getLocalTranslation());
                startTargetRotation = new Quaternion(moveTargetSpatial.getLocalRotation());
                startHandPosition = new Vector3f(hand.getHandNode_xPointing().getWorldTranslation());
                startHandRotation = new Quaternion(hand.getHandNode_xPointing().getWorldRotation());
                handToTargetOffset = startTargetPosition.subtract(startHandPosition);
            }
            Vector3f currentHandPosition = hand.getHandNode_xPointing().getWorldTranslation();
            Quaternion currentHandRotation = hand.getHandNode_xPointing().getWorldRotation();

            Vector3f bulkMotion = currentHandPosition.subtract(startHandPosition);

            Quaternion changeInRotation = getQuaternionFromTo(startHandRotation, currentHandRotation);

            Vector3f rotationInducedMotion = changeInRotation.mult(handToTargetOffset).subtract(handToTargetOffset);
            moveTargetSpatial.setLocalRotation(changeInRotation.mult(startTargetRotation));
            moveTargetSpatial.setLocalTranslation(startTargetPosition.add(bulkMotion).add(rotationInducedMotion));
        }else if (startTargetPosition !=null){
            startTargetPosition = null;
            startHandPosition = null;
            handToTargetOffset = null;
            startTargetRotation = null;
            startHandRotation = null;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){}

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

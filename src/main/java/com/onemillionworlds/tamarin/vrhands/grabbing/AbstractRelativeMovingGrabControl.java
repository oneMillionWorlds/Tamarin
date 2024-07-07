package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.math.Line3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.GrabMoveRestriction;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictToLocalLine;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.Unrestricted;
import com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints.SnapToPoints;

import java.util.Optional;


public abstract class AbstractRelativeMovingGrabControl extends AbstractGrabControl{

    private static final Quaternion UNROTATED_QUATERNION = new Quaternion().fromAngleAxis(0, Vector3f.UNIT_Y);

    private Vector3f startTargetPosition;
    private Vector3f startHandPosition;
    private Vector3f handToTargetOffset;
    private Quaternion startTargetRotation;
    private Quaternion startHandRotation;

    private GrabMoveRestriction grabRestriction = Unrestricted.INSTANCE;

    private boolean shouldApplyRotation = true;

    private SnapToPoints snapToPoints = SnapToPoints.EMPTY;

    private final RestrictionUtilities restrictionUtilities = new RestrictionUtilities(
            (localPosition) -> getMoveTargetSpatial().getParent().localToWorld(localPosition, null),
            (globalPosition) -> getMoveTargetSpatial().getParent().worldToLocal(globalPosition, null)
    );

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

                assert !TamarinUtilities.isNaN(startTargetPosition) : "startTargetPosition is NaN";
                assert !TamarinUtilities.isNaN(startTargetRotation) : "startTargetRotation is NaN";
            }
            Vector3f currentHandPosition = targetParent.worldToLocal(hand.getHandNode_xPointing().getWorldTranslation(), null);
            Quaternion currentHandRotation = targetParent.getWorldRotation().inverse().mult(hand.getHandNode_xPointing().getWorldRotation());

            assert !TamarinUtilities.isNaN(currentHandPosition) : "currentHandPosition is NaN";
            assert !TamarinUtilities.isNaN(currentHandRotation) : "currentHandRotation is NaN";

            Vector3f bulkMotion = currentHandPosition.subtract(startHandPosition);

            Quaternion changeInRotation = getQuaternionFromTo(startHandRotation, currentHandRotation);

            assert !TamarinUtilities.isNaN(changeInRotation) : "changeInRotation is NaN";

            if (this.shouldApplyRotation){
                Vector3f rotationInducedMotion = changeInRotation.mult(handToTargetOffset).subtract(handToTargetOffset);

                Vector3f fullMotionDuringFullGrab = bulkMotion.add(rotationInducedMotion);
                Vector3f newLocalTranslation = applySnapToPoints(applyPositionRestriction(startTargetPosition.add(fullMotionDuringFullGrab)));
                Vector3f changeInLocalTranslationThisTick = newLocalTranslation.subtract(moveTargetSpatial.getLocalTranslation());
                Quaternion newLocalRotation = changeInRotation.mult(startTargetRotation);
                Quaternion changeInLocalRotationThisTick = getQuaternionFromTo(moveTargetSpatial.getLocalRotation(), newLocalRotation);

                assert !TamarinUtilities.isNaN(newLocalTranslation) : "newLocalTranslation is NaN";
                assert !TamarinUtilities.isNaN(newLocalRotation) : "newLocalRotation is NaN";

                moveTargetSpatial.setLocalRotation(newLocalRotation);
                moveTargetSpatial.setLocalTranslation(newLocalTranslation);
                whileGrabbing(hand, changeInLocalTranslationThisTick, changeInLocalRotationThisTick);
            }else{
                Vector3f newLocalTranslation = applySnapToPoints(applyPositionRestriction(startTargetPosition.add(bulkMotion)));
                Vector3f changeInLocalTranslationThisTick = newLocalTranslation.subtract(moveTargetSpatial.getLocalTranslation());
                moveTargetSpatial.setLocalTranslation(newLocalTranslation);
                whileGrabbing(hand, changeInLocalTranslationThisTick, UNROTATED_QUATERNION);
            }
        }else if (startTargetPosition !=null){
            startTargetPosition = null;
            startHandPosition = null;
            handToTargetOffset = null;
            startTargetRotation = null;
            startHandRotation = null;
        }
    }

    /**
     * Called every frame that the hand is grabbing this control and is informed of how much
     * control has moved this frame (after any restrictions have been applied).
     * <p>
     * Intended as a callback that child classes may implement if they want to do something with the change beyond
     * just letting the spatial be moved
     *
     * @param grabbedByHand The hand that is grabbing this control
     * @param bulkMotionThisTick How much control has moved this tick
     * @param rotationChangeThisTick How much the rotation has changed this tick
     */
    @SuppressWarnings("unused")
    public void whileGrabbing(BoundHand grabbedByHand, Vector3f bulkMotionThisTick, Quaternion rotationChangeThisTick){
        //do nothing by default, this is a callback if child classes want to do something with the change
    }


    private Vector3f applyPositionRestriction(Vector3f unrestrictedPosition){
        return grabRestriction.restrictPosition(unrestrictedPosition, restrictionUtilities);
    }

    private Vector3f applySnapToPoints(Vector3f unsnappedPosition){
        return snapToPoints.snap(unsnappedPosition, restrictionUtilities).orElse(unsnappedPosition);
    }

    /**
     * Restricts the move target (often, but not always the spatial this control is attached to).
     * <p>
     * The move target will remain on that path. The path is relative to the move target's parent
     * </p>
     * <p>
     * Deprecated; use {@link #setGrabMoveRestriction(GrabMoveRestriction)} instead
     * </p>
     */
    @Deprecated
    public void restrictToPath(Vector3f startPoint, Vector3f endPoint){
        grabRestriction = new RestrictToLocalLine(new Line3f(startPoint, endPoint));
    }

    /**
     * Sets the restriction that will be applied to the move target's position. This allows for restrictions to be
     * placed on how teh grabbed object can be moved.
     */
    public void setGrabMoveRestriction(GrabMoveRestriction restriction){
        grabRestriction = restriction;
    }

    /**
     * @param shouldApplyRotation if the rotation of the hand should rotate the spatial
     */
    public void setShouldApplyRotation( boolean shouldApplyRotation ){
        this.shouldApplyRotation = shouldApplyRotation;
    }

    /**
     * SnapToPoints are points that if the grabbed object is within a certain distance of, it will snap to that point
     * (rather than exactly following the hand).
     * <p>
     * Be aware snapped points take place after restrictions, ideally make all snapped points within the restricted
     * area to avoid unexpected behaviour.
     * </p>
     * @param snapToPoints object defining the snap behaviour
     */
    public void setSnapToPoints(SnapToPoints snapToPoints){
        this.snapToPoints = snapToPoints;
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

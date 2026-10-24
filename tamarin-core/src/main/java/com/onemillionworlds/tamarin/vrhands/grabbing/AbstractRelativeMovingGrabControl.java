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
import com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints.SnapToConfiguration;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.NoTwoHandedResizing;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.TwoHandedResizeContext;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.TwoHandedResizing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Moves a target spatial so that it keeps its position and rotation relative to the hand(s) holding it.
 * <p>
 * By default only one hand moves the target: if a second hand also grabs it, that hand is ignored until the first
 * hand lets go, at which point the target smoothly carries on following the second hand.
 * <p>
 * With {@link #setTwoHandedGrabEnabled(boolean)} two hands can hold the target at once. It then follows the point
 * midway between the palms, and turns as the line between the palms turns (and as the hands twist, on average, about
 * that line). What the hands moving together or apart does is decided by
 * {@link #setTwoHandedResizing(TwoHandedResizing)} (by default nothing). Changing between one and two hands never
 * makes the target jump.
 */
@SuppressWarnings("unused")
public abstract class AbstractRelativeMovingGrabControl extends AbstractGrabControl{

    private static final Quaternion UNROTATED_QUATERNION = new Quaternion().fromAngleAxis(0, Vector3f.UNIT_Y);

    private static final float MINIMUM_HAND_SEPARATION = 1e-4f;

    /**
     * The hands moving the target, primary hand first.
     */
    private List<AnchoredHand> anchoredHands = Collections.emptyList();
    private Vector3f startTargetPosition;
    private Quaternion startTargetRotation;
    private Vector3f startTargetScale;
    private Vector3f startGraspPosition;
    private Vector3f graspToTargetOffset;
    private Vector3f startHandLine;
    private Vector3f startGraspPointOnTarget;
    private Vector3f startPrimaryHandPointOnTarget;
    private Vector3f startSecondaryHandPointOnTarget;

    private GrabMoveRestriction grabRestriction = Unrestricted.INSTANCE;

    private boolean shouldApplyRotation = true;

    private boolean twoHandedGrabEnabled = false;

    private TwoHandedResizing twoHandedResizing = NoTwoHandedResizing.INSTANCE;

    /**
     * The resizing told that a two-handed grab started (and so is owed an end call), or null if none is in progress.
     */
    private TwoHandedResizing resizingInProgress;

    private SnapToConfiguration snapToConfiguration = SnapToConfiguration.EMPTY;

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
        List<BoundHand> activeHands = activeHands();
        if (activeHands.isEmpty()){
            endTwoHandedResizeIfInProgress();
            anchoredHands = Collections.emptyList();
            return;
        }

        Spatial moveTargetSpatial = getMoveTargetSpatial();
        Node targetParent = moveTargetSpatial.getParent();
        if (!isAnchoredTo(activeHands)){
            endTwoHandedResizeIfInProgress();
            anchor(activeHands, moveTargetSpatial, targetParent);
            if (activeHands.size() == 2){
                resizingInProgress = twoHandedResizing;
                resizingInProgress.twoHandedGrabStarted(moveTargetSpatial);
            }
        }

        AnchoredHand primary = anchoredHands.get(0);
        Vector3f primaryPosition = primary.position(targetParent);

        if (anchoredHands.size() == 1){
            Quaternion changeInRotation = primary.rotationChange(targetParent);
            Vector3f naturalPosition = startTargetPosition.add(primaryPosition.subtract(startGraspPosition));
            if (shouldApplyRotation){
                naturalPosition.addLocal(changeInRotation.mult(graspToTargetOffset).subtractLocal(graspToTargetOffset));
            }
            moveTarget(moveTargetSpatial, primary.hand, naturalPosition, changeInRotation.mult(startTargetRotation));
        }else{
            AnchoredHand secondary = anchoredHands.get(1);
            Vector3f secondaryPosition = secondary.position(targetParent);

            Vector3f midpoint = primaryPosition.add(secondaryPosition).multLocal(0.5f);
            Vector3f handLine = secondaryPosition.subtract(primaryPosition);
            Quaternion newRotation = shouldApplyRotation
                    ? twoHandedRotationChange(handLine, primary.rotationChange(targetParent), secondary.rotationChange(targetParent)).mult(startTargetRotation)
                    : startTargetRotation;

            float startHandSeparation = startHandLine.length();
            float separationRatio = startHandSeparation > MINIMUM_HAND_SEPARATION
                    ? Math.max(handLine.length(), MINIMUM_HAND_SEPARATION) / startHandSeparation
                    : 1;
            Vector3f graspPointOnTarget = resizingInProgress.resize(new TwoHandedResizeContext(
                    moveTargetSpatial, separationRatio, startGraspPointOnTarget, startPrimaryHandPointOnTarget,
                    startSecondaryHandPointOnTarget, startTargetScale));

            Vector3f graspToTarget = newRotation.mult(moveTargetSpatial.getLocalScale().mult(graspPointOnTarget).negateLocal());
            moveTarget(moveTargetSpatial, primary.hand, midpoint.add(graspToTarget), newRotation);
        }
    }

    private boolean isAnchoredTo(List<BoundHand> hands){
        if (hands.size() != anchoredHands.size()){
            return false;
        }
        for (int i = 0; i < hands.size(); i++){
            if (anchoredHands.get(i).hand != hands.get(i)){
                return false;
            }
        }
        return true;
    }

    /**
     * The hands that are currently moving the target, primary hand first.
     */
    private List<BoundHand> activeHands(){
        List<BoundHand> grabbingHands = getGrabbingHands();
        if (grabbingHands.isEmpty()){
            return Collections.emptyList();
        }
        if (twoHandedGrabEnabled && grabbingHands.size() >= 2){
            return List.of(grabbingHands.get(0), grabbingHands.get(1));
        }
        return List.of(grabbingHands.get(0));
    }

    private void endTwoHandedResizeIfInProgress(){
        if (resizingInProgress != null){
            TwoHandedResizing ending = resizingInProgress;
            resizingInProgress = null;
            ending.twoHandedGrabEnded(getMoveTargetSpatial());
        }
    }

    /**
     * Records where the target and the hands are now, so that subsequent movement is relative to this moment. Called
     * whenever the set of hands moving the target changes, which is what stops the target jumping at a hand-over.
     */
    private void anchor(List<BoundHand> activeHands, Spatial moveTargetSpatial, Node targetParent){
        List<AnchoredHand> newlyAnchoredHands = new ArrayList<>(activeHands.size());
        for (BoundHand hand : activeHands){
            newlyAnchoredHands.add(new AnchoredHand(hand, targetParent));
        }
        anchoredHands = newlyAnchoredHands;
        startTargetPosition = targetParent.worldToLocal(moveTargetSpatial.getWorldTranslation(), null);
        startTargetRotation = targetParent.getWorldRotation().inverse().mult(new Quaternion(moveTargetSpatial.getWorldRotation()));
        startTargetScale = moveTargetSpatial.getLocalScale().clone();

        Vector3f primaryPosition = anchoredHands.get(0).position(targetParent);
        if (anchoredHands.size() == 1){
            startGraspPosition = primaryPosition;
            startHandLine = null;
        }else{
            Vector3f secondaryPosition = anchoredHands.get(1).position(targetParent);
            startGraspPosition = primaryPosition.add(secondaryPosition).multLocal(0.5f);
            startHandLine = secondaryPosition.subtract(primaryPosition);
            startGraspPointOnTarget = parentLocalToTargetLocal(startGraspPosition);
            startPrimaryHandPointOnTarget = parentLocalToTargetLocal(primaryPosition);
            startSecondaryHandPointOnTarget = parentLocalToTargetLocal(secondaryPosition);
        }
        graspToTargetOffset = startTargetPosition.subtract(startGraspPosition);

        assert !TamarinUtilities.isNaN(startTargetPosition) : "startTargetPosition is NaN";
        assert !TamarinUtilities.isNaN(startTargetRotation) : "startTargetRotation is NaN";
    }

    /**
     * Converts a point relative to the target's parent into the target's local coordinates, using the anchored target
     * position, rotation and scale.
     */
    private Vector3f parentLocalToTargetLocal(Vector3f parentLocalPoint){
        return startTargetRotation.inverse().mult(parentLocalPoint.subtract(startTargetPosition)).divideLocal(startTargetScale);
    }

    /**
     * The swing of the line between the hands, combined with the average twist of the two hands about that line (the
     * line alone can't say how the target has rolled about itself).
     */
    private Quaternion twoHandedRotationChange(Vector3f handLine, Quaternion primaryRotationChange, Quaternion secondaryRotationChange){
        Quaternion swing = GraspMaths.rotationBetween(startHandLine, handLine);
        Quaternion inverseSwing = swing.inverse();
        Vector3f axis = handLine.normalize();

        float primaryTwist = GraspMaths.twistAngle(primaryRotationChange.mult(inverseSwing), axis);
        float secondaryTwist = GraspMaths.twistAngle(secondaryRotationChange.mult(inverseSwing), axis);

        Quaternion twist = new Quaternion().fromAngleNormalAxis((primaryTwist + secondaryTwist) / 2, axis);
        return twist.mult(swing);
    }

    /**
     * Applies restrictions and snapping to the natural position, then moves (and if rotation is on, rotates) the target.
     */
    private void moveTarget(Spatial moveTargetSpatial, BoundHand primaryHand, Vector3f naturalLocalTranslation, Quaternion newLocalRotation){
        Vector3f newLocalTranslation = applySnapToPoints(applyPositionRestriction(naturalLocalTranslation), primaryHand);
        Vector3f changeInLocalTranslationThisTick = newLocalTranslation.subtract(moveTargetSpatial.getLocalTranslation());

        assert !TamarinUtilities.isNaN(newLocalTranslation) : "newLocalTranslation is NaN";

        moveTargetSpatial.setLocalTranslation(newLocalTranslation);
        if (this.shouldApplyRotation){
            assert !TamarinUtilities.isNaN(newLocalRotation) : "newLocalRotation is NaN";
            Quaternion changeInLocalRotationThisTick = getQuaternionFromTo(moveTargetSpatial.getLocalRotation(), newLocalRotation);
            moveTargetSpatial.setLocalRotation(newLocalRotation);
            whileGrabbing(primaryHand, changeInLocalTranslationThisTick, changeInLocalRotationThisTick);
        }else{
            whileGrabbing(primaryHand, changeInLocalTranslationThisTick, UNROTATED_QUATERNION);
        }
    }

    /**
     * A hand moving the target, along with what was recorded about it when it was anchored.
     */
    private static final class AnchoredHand{
        final BoundHand hand;
        final Quaternion startRotation;
        /**
         * The palm's hold position (where the grab pick happens) in the hand node's coordinates.
         */
        final Vector3f holdPointOnHand;

        AnchoredHand(BoundHand hand, Node targetParent){
            this.hand = hand;
            this.startRotation = rotation(hand, targetParent);
            this.holdPointOnHand = hand.getHandNode_xPointing().worldToLocal(hand.getHoldPosition(0), null);
        }

        /**
         * Where the hand is holding the target: the palm's hold position as it was when the hand was anchored, carried
         * rigidly with the hand node. The palm itself is placed from the hand skeleton, so following it live would make
         * the target drift as the fingers move or tracking jitters.
         */
        Vector3f position(Node targetParent){
            Vector3f worldPosition = hand.getHandNode_xPointing().localToWorld(holdPointOnHand, null);
            Vector3f position = targetParent.worldToLocal(worldPosition, null);
            assert !TamarinUtilities.isNaN(position) : "hand position is NaN";
            return position;
        }

        Quaternion rotationChange(Node targetParent){
            return getQuaternionFromTo(startRotation, rotation(hand, targetParent));
        }

        private static Quaternion rotation(BoundHand hand, Node targetParent){
            Quaternion rotation = targetParent.getWorldRotation().inverse().mult(hand.getHandNode_xPointing().getWorldRotation());
            assert !TamarinUtilities.isNaN(rotation) : "hand rotation is NaN";
            return rotation;
        }
    }

    /**
     * Called every frame that the control is being moved by a hand (or by two hands, see
     * {@link #setTwoHandedGrabEnabled(boolean)}) and is informed of how much control has moved this frame (after any
     * restrictions have been applied).
     * <p>
     * Intended as a callback that child classes may implement if they want to do something with the change beyond
     * just letting the spatial be moved
     *
     * @param grabbedByHand The hand that is grabbing this control (the primary hand if two hands are holding it)
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

    private Vector3f applySnapToPoints(Vector3f unsnappedPosition, BoundHand handSide){
        return snapToConfiguration.snap(unsnappedPosition, restrictionUtilities, handSide).orElse(unsnappedPosition);
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
    @Deprecated(since="2.6", forRemoval = true)
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
     * @param shouldApplyRotation if the rotation of the hand(s) should rotate the spatial
     */
    public void setShouldApplyRotation( boolean shouldApplyRotation ){
        this.shouldApplyRotation = shouldApplyRotation;
    }

    /**
     * Allows two hands to hold the target at once (off by default). While two hands hold it the target follows the
     * point midway between them and rotates as the line between them rotates, and the
     * {@link #setTwoHandedResizing(TwoHandedResizing) two handed resizing} decides what the hands moving apart or
     * together does. While only one hand holds it the behaviour is exactly as with a single hand.
     */
    public void setTwoHandedGrabEnabled(boolean twoHandedGrabEnabled){
        this.twoHandedGrabEnabled = twoHandedGrabEnabled;
    }

    public boolean isTwoHandedGrabEnabled(){
        return twoHandedGrabEnabled;
    }

    /**
     * Decides what happens to the target as two hands holding it move apart or together (only relevant with
     * {@link #setTwoHandedGrabEnabled(boolean)} on). The default, {@link NoTwoHandedResizing}, does nothing.
     * {@link com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.UniformScaleTwoHandedResizing} scales the
     * spatial, or implement {@link TwoHandedResizing} to resize the target some other way (e.g. by rebuilding its mesh).
     * <p>
     * A change takes effect from the next time a second hand grabs.
     * </p>
     */
    public void setTwoHandedResizing(TwoHandedResizing twoHandedResizing){
        this.twoHandedResizing = twoHandedResizing;
    }

    public TwoHandedResizing getTwoHandedResizing(){
        return twoHandedResizing;
    }

    /**
     * SnapToPoints are points that if the grabbed object is within a certain distance of, it will snap to that point
     * (rather than exactly following the hand).
     * <p>
     * Be aware snapped points take place after restrictions, ideally make all snapped points within the restricted
     * area to avoid unexpected behaviour.
     * </p>
     * <p>
     * While two hands are holding the target the primary hand is the one passed to the snap points.
     * </p>
     * @param snapToConfiguration object defining the snap behaviour
     */
    public void setSnapToPoints(SnapToConfiguration snapToConfiguration){
        this.snapToConfiguration = snapToConfiguration;
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

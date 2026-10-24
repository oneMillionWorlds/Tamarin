package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.testhelpers.Vector3fAsserts;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.TwoHandedResizeContext;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.TwoHandedResizing;
import com.onemillionworlds.tamarin.vrhands.grabbing.twohanded.UniformScaleTwoHandedResizing;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelativeMovingGrabControlTest{

    private static final float TOLERANCE = 1e-4f;

    private final Node root = new Node("root");
    private final Node target = new Node("target");
    private final List<Float> separationRatios = new ArrayList<>();
    private final List<String> resizeLifecycle = new ArrayList<>();
    private final TwoHandedResizing recordingResizing = new TwoHandedResizing(){
        @Override
        public void twoHandedGrabStarted(Spatial target){
            resizeLifecycle.add("started");
        }

        @Override
        public Vector3f resize(TwoHandedResizeContext context){
            separationRatios.add(context.getHandSeparationRatio());
            return context.getStartGraspPointOnTarget();
        }

        @Override
        public void twoHandedGrabEnded(Spatial target){
            resizeLifecycle.add("ended");
        }
    };
    private final RelativeMovingGrabControl control = new RelativeMovingGrabControl();

    private final Vector3f palmHoldOffset = new Vector3f();
    private final Node leftHandNode = new Node("leftHand");
    private final Node rightHandNode = new Node("rightHand");
    private final BoundHand leftHand = handOn(leftHandNode);
    private final BoundHand rightHand = handOn(rightHandNode);

    {
        root.attachChild(target);
        target.addControl(control);
    }

    @Test
    void aSingleHandMovesTheTargetWithIt(){
        target.setLocalTranslation(0, 0, 0.5f);
        leftHandNode.setLocalTranslation(1, 0, 0);
        control.onGrab(leftHand);
        update();

        leftHandNode.setLocalTranslation(1, 2, 0);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(0, 2, 0.5f), target.getLocalTranslation(), TOLERANCE);
    }

    @Test
    void aSingleHandRotatesTheTargetAboutTheHand(){
        target.setLocalTranslation(0, 0, 0);
        leftHandNode.setLocalTranslation(1, 0, 0);
        control.onGrab(leftHand);
        update();

        Quaternion quarterTurn = new Quaternion().fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        leftHandNode.setLocalRotation(quarterTurn);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(1, 0, 0).add(quarterTurn.mult(new Vector3f(-1, 0, 0))), target.getLocalTranslation(), TOLERANCE);
        assertSameRotation(quarterTurn, target.getLocalRotation());
    }

    @Test
    void aSecondHandIsIgnoredWhenTwoHandedGrabIsOff(){
        control.setTwoHandedResizing(recordingResizing);
        leftHandNode.setLocalTranslation(-1, 0, 0);
        rightHandNode.setLocalTranslation(1, 0, 0);
        control.onGrab(leftHand);
        control.onGrab(rightHand);
        update();

        rightHandNode.setLocalTranslation(1, 5, 0);
        update();
        Vector3fAsserts.assertEquals(Vector3f.ZERO, target.getLocalTranslation(), TOLERANCE);

        leftHandNode.setLocalTranslation(-1, 1, 0);
        update();
        Vector3fAsserts.assertEquals(new Vector3f(0, 1, 0), target.getLocalTranslation(), TOLERANCE);
        assertTrue(separationRatios.isEmpty());
    }

    @Test
    void theTargetCarriesOnWithTheSecondHandWithoutJumpingWhenTheFirstLetsGo(){
        leftHandNode.setLocalTranslation(-1, 0, 0);
        rightHandNode.setLocalTranslation(1, 0, 0);
        control.onGrab(leftHand);
        update();
        leftHandNode.setLocalTranslation(-1, 1, 0);
        update();
        control.onGrab(rightHand);
        rightHandNode.setLocalTranslation(1, 7, 0);
        update();

        control.onRelease(leftHand);
        update();
        Vector3fAsserts.assertEquals(new Vector3f(0, 1, 0), target.getLocalTranslation(), TOLERANCE);

        rightHandNode.setLocalTranslation(1, 9, 0);
        update();
        Vector3fAsserts.assertEquals(new Vector3f(0, 3, 0), target.getLocalTranslation(), TOLERANCE);
    }

    @Test
    void twoHandsMoveTheTargetWithThePointBetweenThem(){
        control.setTwoHandedGrabEnabled(true);
        target.setLocalTranslation(0, 0, 0.5f);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        leftHandNode.setLocalTranslation(-1, 1, 0);
        rightHandNode.setLocalTranslation(1, 3, 0);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(0, 2, 0.5f), target.getLocalTranslation(), TOLERANCE);
    }

    @Test
    void twoHandsRotateTheTargetAsTheLineBetweenThemTurns(){
        control.setTwoHandedGrabEnabled(true);
        target.setLocalTranslation(0, 0, 0.5f);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        Quaternion quarterTurn = new Quaternion().fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        leftHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(-1, 0, 0)));
        rightHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(1, 0, 0)));
        update();

        assertSameRotation(quarterTurn, target.getLocalRotation());
        Vector3fAsserts.assertEquals(quarterTurn.mult(new Vector3f(0, 0, 0.5f)), target.getLocalTranslation(), TOLERANCE);
    }

    @Test
    void twoHandsTwistingTogetherRollTheTargetAboutTheLineBetweenThem(){
        control.setTwoHandedGrabEnabled(true);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        Quaternion roll = new Quaternion().fromAngleNormalAxis(0.5f, Vector3f.UNIT_X);
        leftHandNode.setLocalRotation(roll);
        rightHandNode.setLocalRotation(roll);
        update();

        assertSameRotation(roll, target.getLocalRotation());
    }

    @Test
    void onlyOneHandTwistingRollsTheTargetByHalfAsMuch(){
        control.setTwoHandedGrabEnabled(true);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        rightHandNode.setLocalRotation(new Quaternion().fromAngleNormalAxis(0.5f, Vector3f.UNIT_X));
        update();

        assertSameRotation(new Quaternion().fromAngleNormalAxis(0.25f, Vector3f.UNIT_X), target.getLocalRotation());
    }

    @Test
    void theResizingIsToldTheHandSeparationRelativeToTheStartOfTheTwoHandedGrab(){
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(recordingResizing);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));
        separationRatios.clear();

        rightHandNode.setLocalTranslation(2, 0, 0);
        update();
        leftHandNode.setLocalTranslation(-4, 0, 0);
        update();

        assertEquals(List.of(1.5f, 3f), separationRatios);
    }

    @Test
    void theResizingIsToldWhenTheTwoHandedGrabStartsAndEnds(){
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(recordingResizing);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));
        update();
        control.onRelease(rightHand);
        update();
        control.onRelease(leftHand);
        update();

        assertEquals(List.of("started", "ended"), resizeLifecycle);
    }

    @Test
    void theTargetIsPositionedSoTheGraspPointReportedByTheResizingIsBetweenTheHands(){
        control.setTwoHandedGrabEnabled(true);
        Quaternion targetRotation = new Quaternion().fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        target.setLocalRotation(targetRotation);
        // e.g. a mesh that has grown so that the grasped point is now 0.5 further along the target's local x
        control.setTwoHandedResizing(context -> context.getStartGraspPointOnTarget().add(0.5f, 0, 0));
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        Vector3fAsserts.assertEquals(targetRotation.mult(new Vector3f(-0.5f, 0, 0)), target.getLocalTranslation(), TOLERANCE);
        Vector3fAsserts.assertEquals(new Vector3f(1, 1, 1), target.getLocalScale(), TOLERANCE);
    }

    @Test
    void theTargetIsNotScaledByTwoHandsUnlessScalingIsEnabled(){
        control.setTwoHandedGrabEnabled(true);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        rightHandNode.setLocalTranslation(3, 0, 0);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(1, 1, 1), target.getLocalScale(), TOLERANCE);
    }

    @Test
    void twoHandedScalingScalesUniformlyWithTheHandSeparation(){
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(UniformScaleTwoHandedResizing.INSTANCE);
        target.setLocalScale(1, 2, 3);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        rightHandNode.setLocalTranslation(3, 0, 0);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(2, 4, 6), target.getLocalScale(), TOLERANCE);
    }

    @Test
    void twoHandedScalingKeepsTheGrabbedPointsUnderTheHands(){
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(UniformScaleTwoHandedResizing.INSTANCE);
        target.setLocalTranslation(0.5f, 0.2f, -0.3f);
        Vector3f leftStart = new Vector3f(-0.2f, 0.4f, 0.1f);
        Vector3f rightStart = new Vector3f(1.3f, 0.1f, 0.6f);
        grabWithBothHands(leftStart, rightStart);
        Vector3f leftPointOnTarget = target.worldToLocal(leftStart, null);
        Vector3f rightPointOnTarget = target.worldToLocal(rightStart, null);

        Vector3f leftEnd = new Vector3f(0.4f, 1.1f, -0.5f);
        Vector3f rightEnd = new Vector3f(-1.5f, 2.0f, 1.2f);
        leftHandNode.setLocalTranslation(leftEnd);
        rightHandNode.setLocalTranslation(rightEnd);
        update();

        Vector3fAsserts.assertEquals(leftEnd, target.localToWorld(leftPointOnTarget, null), TOLERANCE);
        Vector3fAsserts.assertEquals(rightEnd, target.localToWorld(rightPointOnTarget, null), TOLERANCE);
    }

    @Test
    void goingFromTwoScalingHandsToOneKeepsTheScaleWithoutJumping(){
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(UniformScaleTwoHandedResizing.INSTANCE);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(2, 0, 0));
        rightHandNode.setLocalTranslation(5, 0, 0);
        update();
        Vector3f positionBeforeRelease = target.getLocalTranslation().clone();

        control.onRelease(rightHand);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(2, 2, 2), target.getLocalScale(), TOLERANCE);
        Vector3fAsserts.assertEquals(positionBeforeRelease, target.getLocalTranslation(), TOLERANCE);
    }

    @Test
    void goingFromTwoHandsToOneDoesNotMakeTheTargetJump(){
        control.setTwoHandedGrabEnabled(true);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));
        Quaternion quarterTurn = new Quaternion().fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        leftHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(-1, 0, 0)));
        rightHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(1, 0, 0)));
        update();
        Vector3f positionBeforeRelease = target.getLocalTranslation().clone();
        Quaternion rotationBeforeRelease = target.getLocalRotation().clone();

        control.onRelease(leftHand);
        update();

        Vector3fAsserts.assertEquals(positionBeforeRelease, target.getLocalTranslation(), TOLERANCE);
        assertSameRotation(rotationBeforeRelease, target.getLocalRotation());
    }

    @Test
    void goingFromOneHandToTwoDoesNotMakeTheTargetJump(){
        control.setTwoHandedGrabEnabled(true);
        target.setLocalTranslation(0, 0, 0.5f);
        leftHandNode.setLocalTranslation(-1, 0, 0);
        leftHandNode.setLocalRotation(new Quaternion().fromAngleNormalAxis(0.3f, Vector3f.UNIT_Z));
        rightHandNode.setLocalTranslation(1, 0, 0);
        control.onGrab(leftHand);
        update();

        control.onGrab(rightHand);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(0, 0, 0.5f), target.getLocalTranslation(), TOLERANCE);
        assertSameRotation(new Quaternion(), target.getLocalRotation());
    }

    @Test
    void rotationIsNotAppliedWhenTurnedOff(){
        control.setTwoHandedGrabEnabled(true);
        control.setShouldApplyRotation(false);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        Quaternion quarterTurn = new Quaternion().fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        leftHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(-1, 0, 0)));
        rightHandNode.setLocalTranslation(quarterTurn.mult(new Vector3f(1, 0, 0)));
        update();

        assertSameRotation(new Quaternion(), target.getLocalRotation());
    }

    @Test
    void twoHandsAreMeasuredFromThePalmHoldPositionNotTheHandNode(){
        palmHoldOffset.set(0, 0, 0.5f);
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(UniformScaleTwoHandedResizing.INSTANCE);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        // the hand node doesn't move but turning it swings the palm from z=0.5 to z=-0.5, so the palms are further apart
        rightHandNode.setLocalRotation(new Quaternion().fromAngleNormalAxis(FastMath.PI, Vector3f.UNIT_Y));
        update();

        float expectedScale = FastMath.sqrt(5) / 2;
        Vector3fAsserts.assertEquals(new Vector3f(expectedScale, expectedScale, expectedScale), target.getLocalScale(), TOLERANCE);
    }

    @Test
    void thePalmMovingWithinTheHandAfterTheGrabDoesNotMoveTheTarget(){
        palmHoldOffset.set(0, 0, 0.5f);
        control.setTwoHandedGrabEnabled(true);
        control.setTwoHandedResizing(UniformScaleTwoHandedResizing.INSTANCE);
        target.setLocalTranslation(0, 0, 0.5f);
        grabWithBothHands(new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0));

        // e.g. the fingers curling further moving the (skeleton placed) palm relative to the controller pose
        palmHoldOffset.set(0.3f, 0.1f, 0.2f);
        update();

        Vector3fAsserts.assertEquals(new Vector3f(0, 0, 0.5f), target.getLocalTranslation(), TOLERANCE);
        Vector3fAsserts.assertEquals(new Vector3f(1, 1, 1), target.getLocalScale(), TOLERANCE);
    }

    private void grabWithBothHands(Vector3f leftPosition, Vector3f rightPosition){
        leftHandNode.setLocalTranslation(leftPosition);
        rightHandNode.setLocalTranslation(rightPosition);
        control.onGrab(leftHand);
        control.onGrab(rightHand);
        update();
    }

    private void update(){
        root.updateLogicalState(0.016f);
    }

    private BoundHand handOn(Node handNode){
        BoundHand hand = mock(BoundHand.class);
        when(hand.getHandNode_xPointing()).thenReturn(handNode);
        when(hand.getHoldPosition(anyFloat())).thenAnswer(invocation -> handNode.localToWorld(palmHoldOffset, null));
        return hand;
    }

    private static void assertSameRotation(Quaternion expected, Quaternion actual){
        for(Vector3f axis : new Vector3f[]{Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z}){
            Vector3fAsserts.assertEquals(expected.mult(axis), actual.mult(axis), TOLERANCE);
        }
    }
}

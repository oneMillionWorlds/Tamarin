package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import org.junit.jupiter.api.Test;

import java.util.Map;

import com.onemillionworlds.tamarin.testhelpers.Vector3fAsserts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JointGestureCalculationsTest{

    private static final float TOLERANCE = 0.001f;

    @Test
    void anOpenHandIsMakingNoGestures(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().build();

        for(Finger finger : Finger.PINCHING_FINGERS){
            assertEquals(0, JointGestureCalculations.pinchStrength(joints, finger), TOLERANCE, finger.name());
            assertEquals(0, JointGestureCalculations.fingerCurl(joints, finger), TOLERANCE, finger.name());
        }
        assertEquals(0, JointGestureCalculations.fingerCurl(joints, Finger.THUMB), TOLERANCE);
        assertEquals(0, JointGestureCalculations.fistStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.pointStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.thumbsUpStrength(joints), TOLERANCE);
    }

    @Test
    void handScaleIsOneForAReferenceSizedHand(){
        assertEquals(1, JointGestureCalculations.handScale(SyntheticHand.openRightHand().build()), TOLERANCE);
    }

    @Test
    void pinchStrengthIsFullWhenTheSkinIsTouchingAndFallsOffWithTheGap(){
        assertEquals(1, JointGestureCalculations.pinchStrength(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).build(), Finger.MIDDLE), TOLERANCE);
        assertEquals(0.5f, JointGestureCalculations.pinchStrength(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0.025f).build(), Finger.MIDDLE), TOLERANCE);
        assertEquals(0, JointGestureCalculations.pinchStrength(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0.05f).build(), Finger.MIDDLE), TOLERANCE);
    }

    @Test
    void thumbOnTheMiddleOfTheFingerIsNotAPinch(){
        // the start of a microgesture swipe, the thumb on the middle segment of the index finger
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().thumbTipNear(HandJoint.INDEX_INTERMEDIATE_EXT, 0.015f).build();
        assertEquals(0, JointGestureCalculations.pinchStrength(joints, Finger.INDEX), TOLERANCE);
    }

    @Test
    void thumbOnTheSideOfTheFingerNearItsEndIsNotAPinch(){
        // the end of a microgesture swipe towards the finger tip, close to the tip but resting on the last knuckle
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().thumbTipNear(HandJoint.INDEX_DISTAL_EXT, 0.012f).build();
        assertTrue(JointGestureCalculations.pinchGap(joints, Finger.INDEX) < 0.01f, "test setup: the thumb tip is near the finger tip");
        assertTrue(JointGestureCalculations.pinchStrength(joints, Finger.INDEX) < 0.1f);
    }

    @Test
    void tipContactIsFullForATipToTipPinch(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.INDEX, 0).build();
        assertEquals(1, JointGestureCalculations.tipContact(joints, Finger.INDEX), TOLERANCE);
    }

    @Test
    void distanceToSegmentUsesTheNearestPointOnTheSegment(){
        Vector3f a = new Vector3f(0, 0, 0);
        Vector3f b = new Vector3f(1, 0, 0);
        assertEquals(2, JointGestureCalculations.distanceToSegment(new Vector3f(0.5f, 2, 0), a, b), TOLERANCE);
        assertEquals(1, JointGestureCalculations.distanceToSegment(new Vector3f(-1, 0, 0), a, b), TOLERANCE);
        assertEquals(1, JointGestureCalculations.distanceToSegment(new Vector3f(2, 0, 0), a, b), TOLERANCE);
    }

    @Test
    void closestFingerToThumbIsTheOneBeingPinched(){
        for(Finger finger : Finger.PINCHING_FINGERS){
            Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().thumbTipGapToFingerTip(finger, 0.002f).build();
            assertEquals(finger, JointGestureCalculations.closestFingerToThumb(joints));
        }
    }

    @Test
    void fingerCurlIsFullForAFullyCurledFinger(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().curlFinger(Finger.RING, 80).build();
        assertEquals(1, JointGestureCalculations.fingerCurl(joints, Finger.RING), TOLERANCE);
        assertEquals(0, JointGestureCalculations.fingerCurl(joints, Finger.MIDDLE), TOLERANCE);
    }

    @Test
    void fingerCurlIsPartialForAPartlyCurledFinger(){
        // 3 knuckles of 37.5 degrees is 112.5, half way between straight (25) and curled (200)
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().curlFinger(Finger.INDEX, 37.5f).build();
        assertEquals(0.5f, JointGestureCalculations.fingerCurl(joints, Finger.INDEX), 0.01f);
    }

    @Test
    void fistIsRecognised(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand()
                .curlAllFingers(80)
                .thumbTipNear(HandJoint.INDEX_INTERMEDIATE_EXT, 0.015f)
                .build();
        assertEquals(1, JointGestureCalculations.fistStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.pointStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.thumbsUpStrength(joints), TOLERANCE);
    }

    @Test
    void curledFingersWithTheThumbOutIsNotAFist(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand()
                .curlAllFingers(80)
                .pointThumb(new Vector3f(-1, 0, 0))
                .build();
        assertEquals(0, JointGestureCalculations.fistStrength(joints), TOLERANCE);
    }

    @Test
    void pointingIsRecognised(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand()
                .curlFinger(Finger.MIDDLE, 80)
                .curlFinger(Finger.RING, 80)
                .curlFinger(Finger.LITTLE, 80)
                .build();
        assertEquals(1, JointGestureCalculations.pointStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.fistStrength(joints), TOLERANCE);
    }

    @Test
    void thumbsUpIsRecognised(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand()
                .curlAllFingers(80)
                .pointThumb(Vector3f.UNIT_Y)
                .build();
        assertEquals(1, JointGestureCalculations.thumbsUpStrength(joints), TOLERANCE);
        assertEquals(0, JointGestureCalculations.fistStrength(joints), TOLERANCE);
    }

    @Test
    void thumbsDownIsNotThumbsUp(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand()
                .curlAllFingers(80)
                .pointThumb(Vector3f.UNIT_Y.negate())
                .build();
        assertEquals(0, JointGestureCalculations.thumbsUpStrength(joints), TOLERANCE);
    }

    @Test
    void palmNormalPointsAwayFromTheBackOfTheHand(){
        // the synthetic hand's palm joint has an identity rotation, so the back of the hand (+Y) is up
        Vector3fAsserts.assertEquals(new Vector3f(0, -1, 0), JointGestureCalculations.palmNormal(SyntheticHand.openRightHand().build()), TOLERANCE);
    }

    @Test
    void approximateAimIsARayFromTheShoulderThroughTheBaseOfTheIndexFinger(){
        Map<HandJoint, BonePose> joints = SyntheticHand.openRightHand().build();
        Vector3f headPosition = new Vector3f(0.1f, 0.4f, 0.3f);
        Quaternion headRotation = new Quaternion(); // so head left is +X

        Transform aim = JointGestureCalculations.approximateAim(joints, HandSide.RIGHT, headPosition, headRotation);

        Vector3f expectedShoulder = headPosition.add(-JointGestureCalculations.SHOULDER_HALF_WIDTH, -JointGestureCalculations.SHOULDER_DROP, 0);
        Vector3f expectedOrigin = joints.get(HandJoint.INDEX_PROXIMAL_EXT).position().add(joints.get(HandJoint.THUMB_PROXIMAL_EXT).position()).mult(0.5f);
        Vector3f expectedDirection = expectedOrigin.subtract(expectedShoulder).normalize();

        Vector3fAsserts.assertEquals(expectedOrigin, aim.getTranslation(), TOLERANCE);
        Vector3fAsserts.assertEquals(expectedDirection, aim.getRotation().mult(Vector3f.UNIT_Z.negate()), TOLERANCE);
        assertTrue(aim.getRotation().mult(Vector3f.UNIT_Y).y > 0, "the aim should be roughly upright");
    }
}

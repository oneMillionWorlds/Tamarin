package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandJointMeasurementsTest{

    @Test
    void straightFingersHaveNoCurl(){
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().joints());

        for(Finger finger : Finger.values()){
            assertEquals(0, measurements.getCurl(finger), 0.001f, finger.name());
        }
    }

    @Test
    void curlIsTheTotalKnuckleBendOverAFullCurl(){
        // 3 knuckles of 40 degrees is 120 out of the full 220
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().curlFinger(Finger.MIDDLE, 40).joints());

        assertEquals(120f / 220f, measurements.getCurl(Finger.MIDDLE), 0.001f);
        assertEquals(0, measurements.getCurl(Finger.INDEX), 0.001f);
    }

    @Test
    void curlBeyondFullCurlIsClampedToOne(){
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().curlFinger(Finger.RING, 100).joints());

        assertEquals(1, measurements.getCurl(Finger.RING), 0.001f);
    }

    @Test
    void thumbCurlUsesItsOwnFullCurl(){
        // 2 knuckles of 30 degrees is 60 out of the full 120
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().thumb(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, 30).joints());

        assertEquals(0.5f, measurements.getCurl(Finger.THUMB), 0.001f);
    }

    @Test
    void pinchStrengthScalesFromOpenToClosedSkinGap(){
        float handScale = new HandJointMeasurements(SyntheticHand.openHand().joints()).getHandScale();
        // the synthetic joints all have a 1cm radius, so the skin gap is the distance between centres minus 2cm
        float halfwayGap = (HandJointMeasurements.PINCH_FULL_GAP + HandJointMeasurements.PINCH_ZERO_GAP) / 2 * handScale;

        HandJointMeasurements touching = new HandJointMeasurements(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.02f).joints());
        HandJointMeasurements halfway = new HandJointMeasurements(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.02f + halfwayGap).joints());

        assertEquals(1, touching.getPinchStrength(Finger.INDEX), 0.001f);
        assertEquals(0.5f, halfway.getPinchStrength(Finger.INDEX), 0.001f);
        assertEquals(0, touching.getPinchStrength(Finger.MIDDLE), 0.001f);
    }

    @Test
    void handScaleIsTheWristToMiddleKnuckleDistanceOverAnAverageHand(){
        // the synthetic hand's wrist is at (0, 0, 0.05) and its middle knuckle is at (-0.01, 0, -0.04)
        float expected = FastMath.sqrt(0.01f * 0.01f + 0.09f * 0.09f) / HandJointMeasurements.REFERENCE_HAND_SIZE;

        assertEquals(expected, new HandJointMeasurements(SyntheticHand.openHand().joints()).getHandScale(), 0.001f);
    }

    @Test
    void thumbOnTheSideOfTheFingerIsNotAPinch(){
        // like a Meta microgesture, the thumb tip on the side of the index finger, closer to its middle than its tip
        SyntheticHand hand = SyntheticHand.openHand().thumbTipNear(HandJoint.INDEX_INTERMEDIATE_EXT, 0.005f);

        assertEquals(0, new HandJointMeasurements(hand.joints()).getPinchStrength(Finger.INDEX), 0.001f);
    }

    @Test
    void thumbDirectionIsBaseToTip(){
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().thumb(Vector3f.UNIT_Y, Vector3f.UNIT_X, 0).joints());

        assertEquals(1, measurements.getThumbDirection().dot(Vector3f.UNIT_Y), 0.001f);
    }
}

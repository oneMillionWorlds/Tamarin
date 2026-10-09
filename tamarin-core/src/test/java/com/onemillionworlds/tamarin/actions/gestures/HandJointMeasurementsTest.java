package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.Vector3f;
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
    void pinchStrengthScalesFromOpenToClosedDistance(){
        HandJointMeasurements touching = new HandJointMeasurements(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.01f).joints());
        HandJointMeasurements halfway = new HandJointMeasurements(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.045f).joints());

        assertEquals(1, touching.getPinchStrength(Finger.INDEX), 0.001f);
        assertEquals(0.5f, halfway.getPinchStrength(Finger.INDEX), 0.001f);
        assertEquals(0, touching.getPinchStrength(Finger.MIDDLE), 0.001f);
    }

    @Test
    void thumbDirectionIsBaseToTip(){
        HandJointMeasurements measurements = new HandJointMeasurements(SyntheticHand.openHand().thumb(Vector3f.UNIT_Y, Vector3f.UNIT_X, 0).joints());

        assertEquals(1, measurements.getThumbDirection().dot(Vector3f.UNIT_Y), 0.001f);
    }
}

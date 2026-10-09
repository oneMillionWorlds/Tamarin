package com.onemillionworlds.tamarin.actions.gestures;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandGestureRulesTest{

    @Test
    void openHandHasNoGestures(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), SyntheticHand.openHand());

        assertFalse(state.isFist());
        assertFalse(state.isPointing());
        assertFalse(state.isThumbsUp());
        for(Finger finger : Finger.PINCHING_FINGERS){
            assertFalse(state.isPinching(finger));
        }
    }

    @Test
    void curledFingersWithFoldedThumbIsAFist(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), fist());

        assertTrue(state.isFist());
        assertFalse(state.isThumbsUp());
        assertFalse(state.isPointing());
    }

    @Test
    void curledFingersWithStraightThumbPointingUpIsAThumbsUp(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), thumbsUp(Vector3f.UNIT_Y));

        assertTrue(state.isThumbsUp());
        assertFalse(state.isFist());
    }

    @Test
    void thumbsUpRequiresTheThumbToPointUp(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), thumbsUp(new Vector3f(1, 0, 0)));

        assertFalse(state.isThumbsUp());
    }

    @Test
    void straightIndexWithOthersCurledIsPointing(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), fist().curlFinger(Finger.INDEX, 0));

        assertTrue(state.isPointing());
        assertFalse(state.isFist());
    }

    @Test
    void gestureStaysActiveBetweenTheThresholds(){
        HandGestureRules rules = new HandGestureRules();
        // 3 knuckles of 47 degrees is a curl of ~0.64, between the deactivate and activate thresholds
        SyntheticHand partlyCurled = SyntheticHand.openHand().curlAllFingers(47).thumb(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, 30);

        assertFalse(apply(new HandGestureRules(), partlyCurled).isFist(), "Shouldn't activate from scratch");

        apply(rules, fist());
        assertTrue(apply(rules, partlyCurled).isFist(), "Should stay active once activated");
    }

    @Test
    void resetDeactivatesGestures(){
        HandGestureRules rules = new HandGestureRules();
        // 3 knuckles of 47 degrees is a curl of ~0.64, between the deactivate and activate thresholds
        SyntheticHand partlyCurled = SyntheticHand.openHand().curlAllFingers(47).thumb(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, 30);
        apply(rules, fist());

        rules.reset();

        assertFalse(apply(rules, partlyCurled).isFist());
    }

    @Test
    void fingerTouchingThumbIsPinching(){
        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), SyntheticHand.openHand().fingerTipNearThumbTip(Finger.MIDDLE, 0.01f));

        assertTrue(state.isPinching(Finger.MIDDLE));
        assertFalse(state.isPinching(Finger.INDEX));
    }

    @Test
    void twoFingersTouchingThumbIsNotAPinch(){
        SyntheticHand hand = SyntheticHand.openHand()
                .fingerTipNearThumbTip(Finger.INDEX, 0.01f)
                .fingerTipNearThumbTip(Finger.MIDDLE, 0.01f);

        HandGestureTamarinProvidedState state = apply(new HandGestureRules(), hand);

        assertFalse(state.isPinching(Finger.INDEX));
        assertFalse(state.isPinching(Finger.MIDDLE));
    }

    @Test
    void pinchIsReleasedIfAnotherFingerJoinsIt(){
        HandGestureRules rules = new HandGestureRules();
        apply(rules, SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.01f));

        HandGestureTamarinProvidedState state = apply(rules, SyntheticHand.openHand()
                .fingerTipNearThumbTip(Finger.INDEX, 0.01f)
                .fingerTipNearThumbTip(Finger.MIDDLE, 0.01f));

        assertFalse(state.isPinching(Finger.INDEX));
    }

    @Test
    void pinchStaysActiveBetweenTheThresholds(){
        HandGestureRules rules = new HandGestureRules();
        SyntheticHand partlyPinched = SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.0325f);
        float strength = new HandJointMeasurements(partlyPinched.joints()).getPinchStrength(Finger.INDEX);
        assertTrue(strength > HandGestureRules.PINCH_DEACTIVATE && strength < HandGestureRules.PINCH_ACTIVATE, "Test setup: the pinch should be between the thresholds but was " + strength);

        assertFalse(apply(new HandGestureRules(), partlyPinched).isPinching(Finger.INDEX), "Shouldn't activate from scratch");

        apply(rules, SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.01f));
        assertTrue(apply(rules, partlyPinched).isPinching(Finger.INDEX), "Should stay active once activated");
    }

    static SyntheticHand fist(){
        return SyntheticHand.openHand().curlAllFingers(80).thumb(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, 30);
    }

    private static SyntheticHand thumbsUp(Vector3f thumbDirection){
        return SyntheticHand.openHand().curlAllFingers(80).thumb(thumbDirection, Vector3f.UNIT_Z, 0);
    }

    private static HandGestureTamarinProvidedState apply(HandGestureRules rules, SyntheticHand hand){
        return rules.apply(new HandJointMeasurements(hand.joints()));
    }
}

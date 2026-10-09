package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandGestureTrackerTest{

    /**
     * The runtime says the ring finger is pinching (and nothing else)
     */
    private static final HandTrackingAimState RUNTIME_RING_PINCH = new HandTrackingAimState(
            0x10, // XR_HAND_TRACKING_AIM_RING_PINCHING_BIT_FB
            0.1f, 0.2f, 0.95f, 0.4f);

    @Test
    void automaticUsesTheRuntimeWhenAvailable(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().build(), Optional.of(RUNTIME_RING_PINCH)));

        HandGestureState state = tracker.getState(HandGestureSourcePreference.AUTOMATIC).orElseThrow();
        assertEquals(HandGestureSource.RUNTIME, state.getSource());
        assertTrue(state.isPinching(Finger.RING));
        assertEquals(0.95f, state.getPinchStrength(Finger.RING));
    }

    @Test
    void tamarinOnlyIgnoresTheRuntime(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().build(), Optional.of(RUNTIME_RING_PINCH)));

        HandGestureState state = tracker.getState(HandGestureSourcePreference.TAMARIN_ONLY).orElseThrow();
        assertEquals(HandGestureSource.TAMARIN, state.getSource());
        // the synthetic hand is open, so Tamarin sees no pinch even though the runtime does
        assertFalse(state.isPinching(Finger.RING));
    }

    @Test
    void automaticFallsBackToTamarinWithoutTheRuntime(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().build(), Optional.empty()));

        assertEquals(HandGestureSource.TAMARIN, tracker.getState(HandGestureSourcePreference.AUTOMATIC).orElseThrow().getSource());
        assertFalse(tracker.getState(HandGestureSourcePreference.RUNTIME_ONLY).isPresent());
    }

    @Test
    void tamarinOnlyGesturesAreAvailableWhateverTheSource(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().curlAllFingers(80).pointThumb(Vector3f.UNIT_Y).build(), Optional.of(RUNTIME_RING_PINCH)));

        assertTrue(tracker.getState(HandGestureSourcePreference.RUNTIME_ONLY).orElseThrow().isThumbsUp());
        assertTrue(tracker.getState(HandGestureSourcePreference.TAMARIN_ONLY).orElseThrow().isThumbsUp());
    }

    @Test
    void tamarinPinchHasHysteresis(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);

        updateWithMiddlePinchStrength(tracker, 0.8f);
        assertFalse(tamarinState(tracker).isPinching(Finger.MIDDLE), "below the activate threshold");

        updateWithMiddlePinchStrength(tracker, 0.9f);
        assertTrue(tamarinState(tracker).isPinching(Finger.MIDDLE), "above the activate threshold");

        updateWithMiddlePinchStrength(tracker, 0.7f);
        assertTrue(tamarinState(tracker).isPinching(Finger.MIDDLE), "still above the deactivate threshold");

        updateWithMiddlePinchStrength(tracker, 0.5f);
        assertFalse(tamarinState(tracker).isPinching(Finger.MIDDLE), "below the deactivate threshold");
    }

    @Test
    void aClearPinchOnOneFingerIsOnlyThatFinger(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).liftAllFingersExcept(Finger.MIDDLE).build(), Optional.empty()));

        HandGestureState state = tamarinState(tracker);
        assertTrue(state.isPinching(Finger.MIDDLE));
        for(Finger finger : new Finger[]{Finger.INDEX, Finger.RING, Finger.LITTLE}){
            assertFalse(state.isPinching(finger), finger.name());
        }
    }

    @Test
    void thumbEquallyCloseToTwoFingersIsNotAPinch(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        Vector3f between = fingerTip(Finger.INDEX).add(fingerTip(Finger.MIDDLE)).multLocal(0.5f).addLocal(0, -0.01f, 0);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipAt(between).build(), Optional.empty()));

        HandGestureState state = tamarinState(tracker);
        assertTrue(state.getPinchStrength(Finger.INDEX) > HandGestureTracker.PINCH_ACTIVATE, "test setup: the index finger is close");
        assertTrue(state.getPinchStrength(Finger.MIDDLE) > HandGestureTracker.PINCH_ACTIVATE, "test setup: the middle finger is close");
        for(Finger finger : Finger.PINCHING_FINGERS){
            assertFalse(state.isPinching(finger), finger.name());
        }
    }

    @Test
    void anotherFingerNearlyPinchingMeansNeitherIsPinching(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        // the thumb is touching the middle finger, but the index finger (not lifted out of the way) is nearly touching too
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).liftFinger(Finger.RING, 0.04f).liftFinger(Finger.LITTLE, 0.04f).build(), Optional.empty()));

        HandGestureState state = tamarinState(tracker);
        assertTrue(state.getPinchStrength(Finger.MIDDLE) >= HandGestureTracker.PINCH_ACTIVATE, "test setup: the middle finger is pinching");
        assertTrue(state.getPinchStrength(Finger.INDEX) > HandGestureTracker.PINCH_OTHER_FINGERS_MAX_TO_START, "test setup: the index finger is nearly pinching");
        for(Finger finger : Finger.PINCHING_FINGERS){
            assertFalse(state.isPinching(finger), finger.name());
        }
    }

    @Test
    void anotherFingerApproachingReleasesAPinch(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).liftAllFingersExcept(Finger.MIDDLE).build(), Optional.empty()));
        assertTrue(tamarinState(tracker).isPinching(Finger.MIDDLE), "test setup: a clean middle finger pinch");

        // the index finger comes back down next to the middle finger
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).liftFinger(Finger.RING, 0.04f).liftFinger(Finger.LITTLE, 0.04f).build(), Optional.empty()));
        HandGestureState state = tamarinState(tracker);
        assertTrue(state.getPinchStrength(Finger.INDEX) > HandGestureTracker.PINCH_OTHER_FINGERS_MAX_TO_CONTINUE, "test setup: the index finger is nearly pinching");
        for(Finger finger : Finger.PINCHING_FINGERS){
            assertFalse(state.isPinching(finger), finger.name());
        }
    }

    private static Vector3f fingerTip(Finger finger){
        return SyntheticHand.openRightHand().build().get(finger.getTip()).position();
    }

    @Test
    void jointsFromAHeldControllerGiveNoGestures(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        // e.g. SteamVR working out the joints from an Index controller held in a fist
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().curlAllFingers(80).thumbTipNear(HandJoint.INDEX_INTERMEDIATE_EXT, 0.015f).build(), Optional.empty(), HandTrackingDataSource.CONTROLLER));

        for(HandGestureSourcePreference preference : HandGestureSourcePreference.values()){
            assertFalse(tracker.getState(preference).isPresent(), preference.name());
        }
    }

    @Test
    void jointsFromABareHandOrAnUnknownSourceGiveGestures(){
        for(HandTrackingDataSource dataSource : new HandTrackingDataSource[]{HandTrackingDataSource.BARE_HAND, HandTrackingDataSource.UNKNOWN}){
            HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
            update(tracker, new TrackedHand(SyntheticHand.openRightHand().curlAllFingers(80).thumbTipNear(HandJoint.INDEX_INTERMEDIATE_EXT, 0.015f).build(), Optional.empty(), dataSource));
            assertTrue(tamarinState(tracker).isFist(), dataSource.name());
        }
    }

    @Test
    void losingTheHandResetsEverything(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        updateWithMiddlePinchStrength(tracker, 1);
        assertTrue(tamarinState(tracker).isPinching(Finger.MIDDLE));

        tracker.update(Optional.empty());
        assertFalse(tracker.getState(HandGestureSourcePreference.AUTOMATIC).isPresent());

        // a weak pinch when the hand comes back shouldn't count, as the earlier pinch has been forgotten
        updateWithMiddlePinchStrength(tracker, 0.7f);
        assertFalse(tamarinState(tracker).isPinching(Finger.MIDDLE));
    }

    private static void updateWithMiddlePinchStrength(HandGestureTracker tracker, float strength){
        float gap = JointGestureCalculations.PINCH_FULL_GAP + (1 - strength) * (JointGestureCalculations.PINCH_ZERO_GAP - JointGestureCalculations.PINCH_FULL_GAP);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, gap).liftAllFingersExcept(Finger.MIDDLE).build(), Optional.empty()));
    }

    private static HandGestureState tamarinState(HandGestureTracker tracker){
        return tracker.getState(HandGestureSourcePreference.TAMARIN_ONLY).orElseThrow();
    }

    private static void update(HandGestureTracker tracker, TrackedHand trackedHand){
        tracker.update(Optional.of(trackedHand));
    }
}

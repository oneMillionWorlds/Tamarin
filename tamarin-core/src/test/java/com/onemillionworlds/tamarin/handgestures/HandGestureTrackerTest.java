package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandGestureTrackerTest{

    private static final float TPF = 1 / 72f;

    /**
     * The runtime says the ring finger is pinching (and nothing else)
     */
    private static final HandTrackingAimState RUNTIME_RING_PINCH = new HandTrackingAimState(
            0x2 | 0x10, // XR_HAND_TRACKING_AIM_VALID_BIT_FB | XR_HAND_TRACKING_AIM_RING_PINCHING_BIT_FB
            new Vector3f(1, 2, 3), new Quaternion(), 0.1f, 0.2f, 0.95f, 0.4f);

    @Test
    void automaticUsesTheRuntimeWhenAvailable(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().build(), Optional.of(RUNTIME_RING_PINCH)));

        HandGestureState state = tracker.getState(HandGestureSourcePreference.AUTOMATIC).orElseThrow();
        assertEquals(HandGestureSource.RUNTIME, state.getSource());
        assertTrue(state.isPinching(Finger.RING));
        assertEquals(0.95f, state.getPinchStrength(Finger.RING));
        assertEquals(new Vector3f(1, 2, 3), state.getAimPosition());
    }

    @Test
    void tamarinOnlyIgnoresTheRuntime(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().build(), Optional.of(RUNTIME_RING_PINCH)));

        HandGestureState state = tracker.getState(HandGestureSourcePreference.TAMARIN_ONLY).orElseThrow();
        assertEquals(HandGestureSource.TAMARIN, state.getSource());
        // the synthetic hand is open, so Tamarin sees no pinch even though the runtime does
        assertFalse(state.isPinching(Finger.RING));
        assertTrue(state.isAimValid());
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
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, 0).build(), Optional.empty()));

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
    void atMostOneFingerPinchesAtATime(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        Vector3f below = new Vector3f(0, -0.015f, 0);
        Vector3f start = fingerTip(Finger.MIDDLE).add(below);
        Vector3f end = fingerTip(Finger.INDEX).add(below);

        boolean everPinchedMiddle = false;
        for(int step = 0; step <= 20; step++){
            Vector3f thumbTip = start.clone().interpolateLocal(end, step / 20f);
            update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipAt(thumbTip).build(), Optional.empty()));

            HandGestureState state = tamarinState(tracker);
            int pinchingCount = 0;
            for(Finger finger : Finger.PINCHING_FINGERS){
                pinchingCount += state.isPinching(finger) ? 1 : 0;
            }
            assertTrue(pinchingCount <= 1, "step " + step + " has " + pinchingCount + " fingers pinching");
            everPinchedMiddle |= state.isPinching(Finger.MIDDLE);
        }
        assertTrue(everPinchedMiddle, "should have started pinching the middle finger");
        assertTrue(tamarinState(tracker).isPinching(Finger.INDEX), "should have ended up pinching the index finger");
    }

    private static Vector3f fingerTip(Finger finger){
        return SyntheticHand.openRightHand().build().get(finger.getTip()).position();
    }

    @Test
    void losingTheHandResetsEverything(){
        HandGestureTracker tracker = new HandGestureTracker(HandSide.RIGHT);
        updateWithMiddlePinchStrength(tracker, 1);
        assertTrue(tamarinState(tracker).isPinching(Finger.MIDDLE));

        tracker.update(Optional.empty(), new Vector3f(), new Quaternion(), TPF);
        assertFalse(tracker.getState(HandGestureSourcePreference.AUTOMATIC).isPresent());

        // a weak pinch when the hand comes back shouldn't count, as the earlier pinch has been forgotten
        updateWithMiddlePinchStrength(tracker, 0.7f);
        assertFalse(tamarinState(tracker).isPinching(Finger.MIDDLE));
    }

    private static void updateWithMiddlePinchStrength(HandGestureTracker tracker, float strength){
        float gap = JointGestureCalculations.PINCH_FULL_GAP + (1 - strength) * (JointGestureCalculations.PINCH_ZERO_GAP - JointGestureCalculations.PINCH_FULL_GAP);
        update(tracker, new TrackedHand(SyntheticHand.openRightHand().thumbTipGapToFingerTip(Finger.MIDDLE, gap).build(), Optional.empty()));
    }

    private static HandGestureState tamarinState(HandGestureTracker tracker){
        return tracker.getState(HandGestureSourcePreference.TAMARIN_ONLY).orElseThrow();
    }

    private static void update(HandGestureTracker tracker, TrackedHand trackedHand){
        tracker.update(Optional.of(trackedHand), new Vector3f(0, 0.5f, 0.3f), new Quaternion(), TPF);
    }
}

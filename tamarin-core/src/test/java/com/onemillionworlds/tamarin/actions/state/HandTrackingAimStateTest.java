package com.onemillionworlds.tamarin.actions.state;

import com.onemillionworlds.tamarin.handgestures.Finger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandTrackingAimStateTest{

    @Test
    void pinchFlagsAreDecodedPerFinger(){
        HandTrackingAimState state = stateWithStatus(HandTrackingAimState.MIDDLE_PINCHING_BIT | HandTrackingAimState.LITTLE_PINCHING_BIT);

        assertFalse(state.isPinching(Finger.INDEX));
        assertTrue(state.isPinching(Finger.MIDDLE));
        assertFalse(state.isPinching(Finger.RING));
        assertTrue(state.isPinching(Finger.LITTLE));
    }

    @Test
    void otherFlagsAreDecoded(){
        HandTrackingAimState noFlags = stateWithStatus(0);
        assertFalse(noFlags.isSystemGestureActive());
        assertFalse(noFlags.isDominantHand());
        assertFalse(noFlags.isMenuPressed());

        HandTrackingAimState allFlags = stateWithStatus(0x1FF);
        assertTrue(allFlags.isSystemGestureActive());
        assertTrue(allFlags.isDominantHand());
        assertTrue(allFlags.isMenuPressed());
    }

    @Test
    void pinchStrengthsAreReturnedForTheRightFinger(){
        HandTrackingAimState state = new HandTrackingAimState(0, 0.1f, 0.2f, 0.3f, 0.4f);

        assertEquals(0.1f, state.getPinchStrength(Finger.INDEX));
        assertEquals(0.2f, state.getPinchStrength(Finger.MIDDLE));
        assertEquals(0.3f, state.getPinchStrength(Finger.RING));
        assertEquals(0.4f, state.getPinchStrength(Finger.LITTLE));
    }

    private static HandTrackingAimState stateWithStatus(long status){
        return new HandTrackingAimState(status, 0, 0, 0, 0);
    }
}

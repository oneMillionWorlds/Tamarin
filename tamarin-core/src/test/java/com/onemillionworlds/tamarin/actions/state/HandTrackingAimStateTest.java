package com.onemillionworlds.tamarin.actions.state;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandTrackingAimStateTest{

    @Test
    void pinchFlagsAreDecodedPerFinger(){
        HandTrackingAimState state = stateWithStatus(HandTrackingAimState.MIDDLE_PINCHING_BIT | HandTrackingAimState.LITTLE_PINCHING_BIT);

        assertFalse(state.isPinching(HandTrackingAimState.PinchFinger.INDEX));
        assertTrue(state.isPinching(HandTrackingAimState.PinchFinger.MIDDLE));
        assertFalse(state.isPinching(HandTrackingAimState.PinchFinger.RING));
        assertTrue(state.isPinching(HandTrackingAimState.PinchFinger.LITTLE));
    }

    @Test
    void otherFlagsAreDecoded(){
        HandTrackingAimState noFlags = stateWithStatus(0);
        assertFalse(noFlags.isAimValid());
        assertFalse(noFlags.isComputed());
        assertFalse(noFlags.isSystemGestureActive());
        assertFalse(noFlags.isDominantHand());
        assertFalse(noFlags.isMenuPressed());

        HandTrackingAimState allFlags = stateWithStatus(0x1FF);
        assertTrue(allFlags.isAimValid());
        assertTrue(allFlags.isComputed());
        assertTrue(allFlags.isSystemGestureActive());
        assertTrue(allFlags.isDominantHand());
        assertTrue(allFlags.isMenuPressed());
    }

    @Test
    void pinchStrengthsAreReturnedForTheRightFinger(){
        HandTrackingAimState state = new HandTrackingAimState(0, new Vector3f(), new Quaternion(), 0.1f, 0.2f, 0.3f, 0.4f);

        assertEquals(0.1f, state.getPinchStrength(HandTrackingAimState.PinchFinger.INDEX));
        assertEquals(0.2f, state.getPinchStrength(HandTrackingAimState.PinchFinger.MIDDLE));
        assertEquals(0.3f, state.getPinchStrength(HandTrackingAimState.PinchFinger.RING));
        assertEquals(0.4f, state.getPinchStrength(HandTrackingAimState.PinchFinger.LITTLE));
    }

    private static HandTrackingAimState stateWithStatus(long status){
        return new HandTrackingAimState(status, new Vector3f(), new Quaternion(), 0, 0, 0, 0);
    }
}

package com.onemillionworlds.tamarin.actions.compatibility;

import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticDPadTest{
    private SyntheticDPad syntheticDPad;

    @BeforeEach
    public void setup() {
        syntheticDPad = new SyntheticDPad();
    }

    @Test
    public void testNorthDirection() {
        syntheticDPad.updateRawAction(new Vector2fActionState(0, 1, true));
        assertTrue(syntheticDPad.north().getState());
        assertTrue(syntheticDPad.north().hasChanged());
        assertFalse(syntheticDPad.south().getState());
        assertFalse(syntheticDPad.east().getState());
        assertFalse(syntheticDPad.west().getState());

        syntheticDPad.updateRawAction(new Vector2fActionState(0, 0.9f, true));
        assertTrue(syntheticDPad.north().getState());
        assertFalse(syntheticDPad.north().hasChanged());
    }

    @Test
    public void testWestDirection() {
        syntheticDPad.updateRawAction(new Vector2fActionState(-1, 0, true));
        assertTrue(syntheticDPad.west().getState());
        assertTrue(syntheticDPad.west().hasChanged());
        assertFalse(syntheticDPad.south().getState());
        assertFalse(syntheticDPad.east().getState());
        assertFalse(syntheticDPad.north().getState());

        syntheticDPad.updateRawAction(new Vector2fActionState(-0.9f, 0, true));
        assertTrue(syntheticDPad.west().getState());
        assertFalse(syntheticDPad.west().hasChanged());
    }

    @Test
    public void testDeactivation() {
        syntheticDPad.updateRawAction(new Vector2fActionState(0, 1, true));
        syntheticDPad.updateRawAction(new Vector2fActionState(0, 0, true));

        assertFalse(syntheticDPad.north().getState());
        assertFalse(syntheticDPad.south().getState());
        assertFalse(syntheticDPad.east().getState());
        assertFalse(syntheticDPad.west().getState());
    }
}
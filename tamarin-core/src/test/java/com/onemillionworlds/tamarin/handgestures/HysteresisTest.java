package com.onemillionworlds.tamarin.handgestures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HysteresisTest{

    @Test
    void activatesAtTheHigherThresholdAndDeactivatesBelowTheLowerOne(){
        Hysteresis hysteresis = new Hysteresis(0.8f, 0.6f);
        assertFalse(hysteresis.update(0.7f));
        assertTrue(hysteresis.update(0.8f));
        assertTrue(hysteresis.update(0.6f));
        assertFalse(hysteresis.update(0.59f));
        assertFalse(hysteresis.update(0.7f));
    }

    @Test
    void resetDeactivates(){
        Hysteresis hysteresis = new Hysteresis(0.8f, 0.6f);
        hysteresis.update(1);
        hysteresis.reset();
        assertFalse(hysteresis.isActive());
        assertFalse(hysteresis.update(0.7f));
    }

    @Test
    void deactivateThresholdAboveActivateThresholdIsRejected(){
        assertThrows(IllegalArgumentException.class, () -> new Hysteresis(0.5f, 0.6f));
    }
}

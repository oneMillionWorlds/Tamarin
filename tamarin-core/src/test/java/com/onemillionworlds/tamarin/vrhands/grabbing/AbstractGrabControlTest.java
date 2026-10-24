package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.onemillionworlds.tamarin.vrhands.BoundHand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AbstractGrabControlTest{

    private final BoundHand leftHand = mock(BoundHand.class);
    private final BoundHand rightHand = mock(BoundHand.class);
    private final AbstractGrabControl control = new GrabEventControl(() -> {});

    @Test
    void aSecondHandGrabbingDoesNotReplaceTheFirst(){
        control.onGrab(leftHand);
        control.onGrab(rightHand);

        assertEquals(Optional.of(leftHand), control.getGrabbingHand());
        assertEquals(List.of(leftHand, rightHand), control.getGrabbingHands());
        assertTrue(control.isGrabbedBy(leftHand));
        assertTrue(control.isGrabbedBy(rightHand));
    }

    @Test
    void releasingOneHandLeavesTheControlHeldByTheOther(){
        control.onGrab(leftHand);
        control.onGrab(rightHand);

        control.onRelease(leftHand);

        assertTrue(control.isGrabbed());
        assertFalse(control.isGrabbedBy(leftHand));
        assertEquals(Optional.of(rightHand), control.getGrabbingHand());
    }

    @Test
    void releasingAHandThatIsNotHoldingTheControlChangesNothing(){
        control.onGrab(leftHand);

        control.onRelease(rightHand);

        assertEquals(Optional.of(leftHand), control.getGrabbingHand());
    }

    @Test
    void releasingEveryHandLeavesTheControlUngrabbed(){
        control.onGrab(leftHand);
        control.onGrab(rightHand);

        control.onRelease(rightHand);
        control.onRelease(leftHand);

        assertFalse(control.isGrabbed());
        assertEquals(Optional.empty(), control.getGrabbingHand());
    }

    @Test
    void grabbingAgainWithTheSameHandDoesNotCountItTwice(){
        control.onGrab(leftHand);
        control.onGrab(leftHand);

        control.onRelease(leftHand);

        assertFalse(control.isGrabbed());
    }
}

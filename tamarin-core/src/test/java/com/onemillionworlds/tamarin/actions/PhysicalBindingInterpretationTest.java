package com.onemillionworlds.tamarin.actions;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalBindingInterpretationTest{
    @Test
    public void testRightJoystickClick() {
        String rawValue = "/user/hand/right/input/joystick/click";
        PhysicalBindingInterpretation interpretation = PhysicalBindingInterpretation.interpretRawValue(rawValue);
        assertEquals(Optional.of(HandSide.RIGHT), interpretation.handSide());
        assertEquals("joystick", interpretation.fundamentalButton());
        assertEquals("click", interpretation.withinButtonAction());
    }

    @Test
    public void testLeftJoystickY() {
        String rawValue = "/user/hand/left/input/joystick/y";
        PhysicalBindingInterpretation interpretation = PhysicalBindingInterpretation.interpretRawValue(rawValue);
        assertEquals(Optional.of(HandSide.LEFT), interpretation.handSide());
        assertEquals("joystick", interpretation.fundamentalButton());
        assertEquals("y", interpretation.withinButtonAction());
    }

    @Test
    public void testRightATouch() {
        String rawValue = "/user/hand/right/input/a/touch";
        PhysicalBindingInterpretation interpretation = PhysicalBindingInterpretation.interpretRawValue(rawValue);
        assertEquals(Optional.of(HandSide.RIGHT), interpretation.handSide());
        assertEquals("a", interpretation.fundamentalButton());
        assertEquals("touch", interpretation.withinButtonAction());
    }

    @Test
    public void testRightAClick() {
        String rawValue = "/user/hand/right/input/a/click";
        PhysicalBindingInterpretation interpretation = PhysicalBindingInterpretation.interpretRawValue(rawValue);
        assertEquals(Optional.of(HandSide.RIGHT), interpretation.handSide());
        assertEquals("a", interpretation.fundamentalButton());
        assertEquals("click", interpretation.withinButtonAction());
    }

    @Test
    public void testHeadSystemClick() {
        String rawValue = "/user/head/input/system/click";
        PhysicalBindingInterpretation interpretation = PhysicalBindingInterpretation.interpretRawValue(rawValue);
        assertEquals(Optional.empty(), interpretation.handSide());
        assertEquals("system", interpretation.fundamentalButton());
        assertEquals("click", interpretation.withinButtonAction());
    }

    @Test
    public void testInvalidInput() {
        String rawValue = "/user/hand/right/input/";
        assertThrows(RuntimeException.class, () -> PhysicalBindingInterpretation.interpretRawValue(rawValue));
    }

}
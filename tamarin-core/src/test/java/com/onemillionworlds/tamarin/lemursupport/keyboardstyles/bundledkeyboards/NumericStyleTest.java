package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericStyleTest{

    private final NumericStyle style = NumericStyle.INSTANCE;

    @Test
    void numbersAreAllowed(){
        assertTrue(style.isAllowedText("0"));
        assertTrue(style.isAllowedText("123"));
        assertTrue(style.isAllowedText("-12.5"));
        assertTrue(style.isAllowedText(".5"));
    }

    @Test
    void partlyTypedNumbersAreAllowed(){
        assertTrue(style.isAllowedText("-"));
        assertTrue(style.isAllowedText("1."));
        assertTrue(style.isAllowedText("-."));
    }

    @Test
    void negativeSignIsOnlyAllowedFirst(){
        assertFalse(style.isAllowedText("1-"));
        assertFalse(style.isAllowedText("--"));
        assertFalse(style.isAllowedText("1.-"));
    }

    @Test
    void onlyOneDecimalPointIsAllowed(){
        assertFalse(style.isAllowedText("1.2."));
        assertFalse(style.isAllowedText(".."));
    }

    @Test
    void nonNumericCharactersAreRejected(){
        assertFalse(style.isAllowedText("1a"));
        assertFalse(style.isAllowedText("1 "));
    }
}

package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyboardTextEditsTest{

    @Test
    void flipSign_positiveBecomesNegative(){
        assertEquals("-12.5", KeyboardTextEdits.flipSign("12.5"));
    }

    @Test
    void flipSign_negativeBecomesPositive(){
        assertEquals("12.5", KeyboardTextEdits.flipSign("-12.5"));
    }

    @Test
    void flipSign_emptyBecomesJustANegativeSign(){
        assertEquals("-", KeyboardTextEdits.flipSign(""));
    }

    @Test
    void flipSign_justANegativeSignBecomesEmpty(){
        assertEquals("", KeyboardTextEdits.flipSign("-"));
    }

    @Test
    void flipSign_zeroCountsAsPositive_negativeZeroCountsAsNegative(){
        assertEquals("-0", KeyboardTextEdits.flipSign("0"));
        assertEquals("0", KeyboardTextEdits.flipSign("-0"));
    }
}

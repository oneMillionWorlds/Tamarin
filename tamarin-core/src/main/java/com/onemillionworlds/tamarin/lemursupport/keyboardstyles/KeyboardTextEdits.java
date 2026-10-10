package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

/**
 * Text manipulations performed by keyboard events (rather than by simply typing characters).
 */
public class KeyboardTextEdits{

    /**
     * Flips the sign of a (possibly partly typed) number. Text starting with a "-" counts as negative (including "-0")
     * and has it removed, anything else (including empty text) has a "-" added at the start.
     */
    public static String flipSign(String text){
        if (text.startsWith("-")){
            return text.substring(1);
        }
        return "-" + text;
    }
}

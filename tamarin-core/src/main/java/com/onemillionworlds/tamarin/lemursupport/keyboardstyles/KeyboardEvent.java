package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

/**
 * Keyboard events are a way that actions other than typing letters are communicated to the caller.
 *
 */
public enum KeyboardEvent{
    DELETE_CHAR,
    DELETE_ALL,
    CLOSED_KEYBOARD,
    CASE_CHANGE,
    /**
     * The sign of the value should be flipped; a leading "-" removed if present, otherwise one added
     * (see {@link KeyboardTextEdits#flipSign}).
     */
    FLIP_SIGN,
    /**
     * An even defined by the user of the library (that has defined that button
     */
    CUSTOM_EVENT
}

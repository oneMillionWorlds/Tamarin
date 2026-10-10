package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

/**
 * How the rows of a keyboard are laid out when they are different widths.
 */
public enum KeyboardRowAlignment{
    /**
     * Shorter rows have their keys stretched so every row is as wide as the widest row.
     */
    STRETCH,
    /**
     * Keys keep their natural size, with rows aligned to the left.
     */
    LEFT,
    /**
     * Keys keep their natural size, with rows centred.
     */
    CENTRE,
    /**
     * Keys keep their natural size, with rows aligned to the right.
     */
    RIGHT
}

package com.onemillionworlds.tamarin.lemursupport;

public enum SpecialHandlingClickThroughResult{
    /**
     * The special handling did not click on anything (but it might still be a button that is handled elsewhere
     */
    NO_SPECIAL_INTERACTIONS,
    /**
     * The click was on a preexisting lemur keyboard (either background or button)
     * In general ignored (the nornal click handling can deal with this) but the caller is informed as this should
     * not lead to a keyboard closure unlike other touches.
     */
    CLICK_ON_LEMUR_KEYBOARD,
    /**
     * The click triggered a lemur keyboard to open
     */
    OPENED_LEMUR_KEYBOARD,

    /**
     * Opened the special support for drop-downs (as lemur ones only work in 2D)
     */
    OPENED_DROPDOWN,

    /**
     * Largely handled separately but the caller is informed so that clicking on the Up/Down arrows
     * doesn't trigger a closure
     */
    CLICKED_ON_DROPDOWN_POPUP,
}

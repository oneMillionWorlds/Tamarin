package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;

public interface KeyboardStyle{

    /**
     * The keys that the keyboard has
     *
     */
    KeyboardButton[][] getKeyboardKeys();

}

package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.function.BiConsumer;

public interface KeyboardButton{

    /**
     * What is written in game on the key
     */
    String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard);

    /**
     * What pressing the key will generate in the output
     */
    String getStringToAddOnClick(LemurKeyboard.ShiftMode ShiftMode, LemurKeyboard keyboard);

    /**
     * Any special behaviour of the button
     */
    void onClickEvent(BiConsumer<KeyboardEvent,Object> eventConsumer, LemurKeyboard keyboard);

}

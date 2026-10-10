package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.function.BiConsumer;

/**
 * Renders as "-" but rather than typing a "-" it flips the sign of the whole value (removing a leading "-" if there is
 * one, adding one if there isn't). See {@link KeyboardEvent#FLIP_SIGN}.
 */
public class FlipSignKey implements KeyboardButton{

    @Override
    public String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return " - ";
    }

    @Override
    public String getStringToAddOnClick(LemurKeyboard.ShiftMode ShiftMode, LemurKeyboard keyboard){
        return "";
    }

    @Override
    public void onClickEvent(BiConsumer<KeyboardEvent, Object> eventConsumer, LemurKeyboard keyboard){
        eventConsumer.accept(KeyboardEvent.FLIP_SIGN, null);
    }
}

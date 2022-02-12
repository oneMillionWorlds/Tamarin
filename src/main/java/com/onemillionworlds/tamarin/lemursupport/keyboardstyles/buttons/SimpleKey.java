package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * A button that simply has a letter and prints it when its pressed
 */
public class SimpleKey implements KeyboardButton{

    private final String character;

    public SimpleKey(String character){
        this.character = character;
    }

    @Override
    public String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return getStringToAddOnClick(shiftMode, keyboard);
    }

    @Override
    public String getStringToAddOnClick(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return shiftMode == LemurKeyboard.ShiftMode.LOWER?character:character.toUpperCase(Locale.ROOT);

    }

    @Override
    public void onClickEvent(BiConsumer<KeyboardEvent,Object> eventConsumer, LemurKeyboard keyboard){
    }
}


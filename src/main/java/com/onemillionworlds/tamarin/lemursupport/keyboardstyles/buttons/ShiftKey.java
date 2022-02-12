package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.function.BiConsumer;

public class ShiftKey implements KeyboardButton{
    @Override
    public String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return "Shift";
    }

    @Override
    public String getStringToAddOnClick(LemurKeyboard.ShiftMode ShiftMode, LemurKeyboard keyboard){
        return "";
    }

    @Override
    public void onClickEvent(BiConsumer<KeyboardEvent, Object> eventConsumer, LemurKeyboard keyboard){
        LemurKeyboard.ShiftMode currentMode = keyboard.getShiftMode();
        LemurKeyboard.ShiftMode newMode = currentMode == LemurKeyboard.ShiftMode.LOWER ? LemurKeyboard.ShiftMode.UPPER : LemurKeyboard.ShiftMode.LOWER;
        keyboard.setShiftMode( newMode );
    }
}

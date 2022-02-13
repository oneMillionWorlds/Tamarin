package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.function.BiConsumer;

public class SpaceBar implements KeyboardButton{

    @Override
    public String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return "               ";
    }

    @Override
    public String getStringToAddOnClick(LemurKeyboard.ShiftMode ShiftMode, LemurKeyboard keyboard){
        return " ";
    }

    @Override
    public void onClickEvent(BiConsumer<KeyboardEvent, Object> eventConsumer, LemurKeyboard keyboard){

    }
}

package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons;

import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;

import java.util.function.BiConsumer;

public class EnterKey implements KeyboardButton{

    @Override
    public String render(LemurKeyboard.ShiftMode shiftMode, LemurKeyboard keyboard){
        return "Enter";
    }

    @Override
    public String getStringToAddOnClick(LemurKeyboard.ShiftMode ShiftMode, LemurKeyboard keyboard){
        return "";
    }

    @Override
    public void onClickEvent(BiConsumer<KeyboardEvent, Object> eventConsumer, LemurKeyboard keyboard){
        keyboard.getStateManager().detach(keyboard);
    }
}

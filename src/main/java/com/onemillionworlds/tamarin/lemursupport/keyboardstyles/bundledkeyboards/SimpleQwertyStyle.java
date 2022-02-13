package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards;

import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.Backspace;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.EnterKey;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.ShiftKey;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.SimpleKey;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.SpaceBar;

public class SimpleQwertyStyle implements KeyboardStyle{
    @Override
    public KeyboardButton[][] getKeyboardKeys(){
        return new KeyboardButton[][]{
                new KeyboardButton[]{k("1"),k("2"),k("3"),k("4"),k("5"),k("6"),k("7"),k("8"),k("9"),k("0")},
                new KeyboardButton[]{k("q"),k("w"),k("e"),k("r"),k("t"),k("y"),k("u"),k("i"),k("o"),k("p")},
                new KeyboardButton[]{k("a"),k("s"),k("d"),k("f"),k("g"),k("h"),k("j"),k("k"),k("l"), new EnterKey()},
                new KeyboardButton[]{new ShiftKey(), k("z"),k("x"),k("c"),k("v"),k("b"),k("n"),k("m"), new Backspace()},
                new KeyboardButton[]{k(","), k("."), k("("), k(")"),new SpaceBar(), k("?"), k("!"), k("%"), k("&"), k("$"), k(";"), k(":")},
        };
    }

    private static KeyboardButton k(String c){
        return new SimpleKey(c);
    }
}

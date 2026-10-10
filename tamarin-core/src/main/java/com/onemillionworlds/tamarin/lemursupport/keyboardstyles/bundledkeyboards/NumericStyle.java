package com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards;

import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardRowAlignment;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.Backspace;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.EnterKey;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.FlipSignKey;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.SimpleKey;

import java.util.regex.Pattern;

/**
 * A number pad: digits, a sign flip ("-"), a decimal point, backspace and enter.
 * <p>
 * The "-" key flips the sign of the whole value rather than typing a "-" (see {@link FlipSignKey}).
 * Key presses that would make the text not a number are rejected, so there can be at most one decimal point. Partly typed numbers like "-" or "1." are allowed, so the final text may
 * still need checking before it is parsed.
 * <p>
 * Note the keyboard can only control what is typed; text set programmatically on the text field is not checked.
 */
public class NumericStyle implements KeyboardStyle{

    public static final NumericStyle INSTANCE = new NumericStyle();

    private static final Pattern PARTIAL_NUMBER = Pattern.compile("-?[0-9]*\\.?[0-9]*");

    @Override
    public KeyboardButton[][] getKeyboardKeys(){
        return new KeyboardButton[][]{
                new KeyboardButton[]{k("7"),k("8"),k("9"), new Backspace()},
                new KeyboardButton[]{k("4"),k("5"),k("6"), new FlipSignKey()},
                new KeyboardButton[]{k("1"),k("2"),k("3"), k(".")},
                new KeyboardButton[]{k("0"), new EnterKey()},
        };
    }

    @Override
    public KeyboardRowAlignment getRowAlignment(){
        return KeyboardRowAlignment.LEFT;
    }

    @Override
    public boolean isAllowedText(String proposedText){
        return PARTIAL_NUMBER.matcher(proposedText).matches();
    }

    private static KeyboardButton k(String c){
        return new SimpleKey(c);
    }
}

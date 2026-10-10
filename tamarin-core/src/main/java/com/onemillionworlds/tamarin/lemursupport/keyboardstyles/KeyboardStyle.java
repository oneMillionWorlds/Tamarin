package com.onemillionworlds.tamarin.lemursupport.keyboardstyles;

import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;

public interface KeyboardStyle{

    /**
     * The keys that the keyboard has
     *
     */
    KeyboardButton[][] getKeyboardKeys();

    /**
     * How rows of different widths are laid out. Defaults to {@link KeyboardRowAlignment#STRETCH}.
     */
    default KeyboardRowAlignment getRowAlignment(){
        return KeyboardRowAlignment.STRETCH;
    }

    /**
     * Called before typed text is added to the text field. If this returns false the key press is ignored and the text
     * field is left unchanged. Deletions are always allowed.
     * <p>
     * Note that this is called on every key press, so text that is a valid "work in progress" (e.g. a lone "-" when
     * typing a negative number) should be accepted.
     *
     * @param proposedText the full text the text field would have if the key press were accepted
     * @return true if the key press should be accepted
     */
    default boolean isAllowedText(String proposedText){
        return true;
    }

}

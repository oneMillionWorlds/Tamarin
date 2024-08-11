package com.onemillionworlds.tamarin.actions;

import com.jme3.input.KeyInput;

import java.util.HashMap;
import java.util.Map;

public class ReverseKeyMapper{

    private static final Map<Integer, String> keyMap = new HashMap<>();

    static {
        keyMap.put(KeyInput.KEY_UNKNOWN, "UNKNOWN");
        keyMap.put(KeyInput.KEY_ESCAPE, "ESCAPE");
        keyMap.put(KeyInput.KEY_1, "1");
        keyMap.put(KeyInput.KEY_2, "2");
        keyMap.put(KeyInput.KEY_3, "3");
        keyMap.put(KeyInput.KEY_4, "4");
        keyMap.put(KeyInput.KEY_5, "5");
        keyMap.put(KeyInput.KEY_6, "6");
        keyMap.put(KeyInput.KEY_7, "7");
        keyMap.put(KeyInput.KEY_8, "8");
        keyMap.put(KeyInput.KEY_9, "9");
        keyMap.put(KeyInput.KEY_0, "0");
        keyMap.put(KeyInput.KEY_MINUS, "MINUS");
        keyMap.put(KeyInput.KEY_EQUALS, "EQUALS");
        keyMap.put(KeyInput.KEY_BACK, "BACK");
        keyMap.put(KeyInput.KEY_TAB, "TAB");
        keyMap.put(KeyInput.KEY_Q, "Q");
        keyMap.put(KeyInput.KEY_W, "W");
        keyMap.put(KeyInput.KEY_E, "E");
        keyMap.put(KeyInput.KEY_R, "R");
        keyMap.put(KeyInput.KEY_T, "T");
        keyMap.put(KeyInput.KEY_Y, "Y");
        keyMap.put(KeyInput.KEY_U, "U");
        keyMap.put(KeyInput.KEY_I, "I");
        keyMap.put(KeyInput.KEY_O, "O");
        keyMap.put(KeyInput.KEY_P, "P");
        keyMap.put(KeyInput.KEY_LBRACKET, "LBRACKET");
        keyMap.put(KeyInput.KEY_RBRACKET, "RBRACKET");
        keyMap.put(KeyInput.KEY_RETURN, "RETURN");
        keyMap.put(KeyInput.KEY_LCONTROL, "LCONTROL");
        keyMap.put(KeyInput.KEY_A, "A");
        keyMap.put(KeyInput.KEY_S, "S");
        keyMap.put(KeyInput.KEY_D, "D");
        keyMap.put(KeyInput.KEY_F, "F");
        keyMap.put(KeyInput.KEY_G, "G");
        keyMap.put(KeyInput.KEY_H, "H");
        keyMap.put(KeyInput.KEY_J, "J");
        keyMap.put(KeyInput.KEY_K, "K");
        keyMap.put(KeyInput.KEY_L, "L");
        keyMap.put(KeyInput.KEY_SEMICOLON, "SEMICOLON");
        keyMap.put(KeyInput.KEY_APOSTROPHE, "APOSTROPHE");
        keyMap.put(KeyInput.KEY_GRAVE, "GRAVE");
        keyMap.put(KeyInput.KEY_LSHIFT, "LSHIFT");
        keyMap.put(KeyInput.KEY_BACKSLASH, "BACKSLASH");
        keyMap.put(KeyInput.KEY_Z, "Z");
        keyMap.put(KeyInput.KEY_X, "X");
        keyMap.put(KeyInput.KEY_C, "C");
        keyMap.put(KeyInput.KEY_V, "V");
        keyMap.put(KeyInput.KEY_B, "B");
        keyMap.put(KeyInput.KEY_N, "N");
        keyMap.put(KeyInput.KEY_M, "M");
        keyMap.put(KeyInput.KEY_COMMA, "COMMA");
        keyMap.put(KeyInput.KEY_PERIOD, "PERIOD");
        keyMap.put(KeyInput.KEY_SLASH, "SLASH");
        keyMap.put(KeyInput.KEY_RSHIFT, "RSHIFT");
        keyMap.put(KeyInput.KEY_MULTIPLY, "MULTIPLY");
        keyMap.put(KeyInput.KEY_LMENU, "LMENU");
        keyMap.put(KeyInput.KEY_SPACE, "SPACE");
        keyMap.put(KeyInput.KEY_CAPITAL, "CAPITAL");
        keyMap.put(KeyInput.KEY_F1, "F1");
        keyMap.put(KeyInput.KEY_F2, "F2");
        keyMap.put(KeyInput.KEY_F3, "F3");
        keyMap.put(KeyInput.KEY_F4, "F4");
        keyMap.put(KeyInput.KEY_F5, "F5");
        keyMap.put(KeyInput.KEY_F6, "F6");
        keyMap.put(KeyInput.KEY_F7, "F7");
        keyMap.put(KeyInput.KEY_F8, "F8");
        keyMap.put(KeyInput.KEY_F9, "F9");
        keyMap.put(KeyInput.KEY_F10, "F10");
        keyMap.put(KeyInput.KEY_NUMLOCK, "NUMLOCK");
        keyMap.put(KeyInput.KEY_SCROLL, "SCROLL");
        keyMap.put(KeyInput.KEY_NUMPAD7, "NUMPAD7");
        keyMap.put(KeyInput.KEY_NUMPAD8, "NUMPAD8");
        keyMap.put(KeyInput.KEY_NUMPAD9, "NUMPAD9");
        keyMap.put(KeyInput.KEY_SUBTRACT, "SUBTRACT");
        keyMap.put(KeyInput.KEY_NUMPAD4, "NUMPAD4");
        keyMap.put(KeyInput.KEY_NUMPAD5, "NUMPAD5");
        keyMap.put(KeyInput.KEY_NUMPAD6, "NUMPAD6");
        keyMap.put(KeyInput.KEY_ADD, "ADD");
        keyMap.put(KeyInput.KEY_NUMPAD1, "NUMPAD1");
        keyMap.put(KeyInput.KEY_NUMPAD2, "NUMPAD2");
        keyMap.put(KeyInput.KEY_NUMPAD3, "NUMPAD3");
        keyMap.put(KeyInput.KEY_NUMPAD0, "NUMPAD0");
        keyMap.put(KeyInput.KEY_DECIMAL, "DECIMAL");
        keyMap.put(KeyInput.KEY_F11, "F11");
        keyMap.put(KeyInput.KEY_F12, "F12");
        keyMap.put(KeyInput.KEY_F13, "F13");
        keyMap.put(KeyInput.KEY_F14, "F14");
        keyMap.put(KeyInput.KEY_F15, "F15");
        keyMap.put(KeyInput.KEY_KANA, "KANA");
        keyMap.put(KeyInput.KEY_CONVERT, "CONVERT");
        keyMap.put(KeyInput.KEY_NOCONVERT, "NOCONVERT");
        keyMap.put(KeyInput.KEY_YEN, "YEN");
        keyMap.put(KeyInput.KEY_NUMPADEQUALS, "NUMPADEQUALS");
        keyMap.put(KeyInput.KEY_CIRCUMFLEX, "CIRCUMFLEX");
        keyMap.put(KeyInput.KEY_AT, "AT");
        keyMap.put(KeyInput.KEY_COLON, "COLON");
        keyMap.put(KeyInput.KEY_UNDERLINE, "UNDERLINE");
        keyMap.put(KeyInput.KEY_KANJI, "KANJI");
        keyMap.put(KeyInput.KEY_STOP, "STOP");
        keyMap.put(KeyInput.KEY_AX, "AX");
        keyMap.put(KeyInput.KEY_UNLABELED, "UNLABELED");
        keyMap.put(KeyInput.KEY_PRTSCR, "PRTSCR");
        keyMap.put(KeyInput.KEY_NUMPADENTER, "NUMPADENTER");
        keyMap.put(KeyInput.KEY_RCONTROL, "RCONTROL");
        keyMap.put(KeyInput.KEY_NUMPADCOMMA, "NUMPADCOMMA");
        keyMap.put(KeyInput.KEY_DIVIDE, "DIVIDE");
        keyMap.put(KeyInput.KEY_SYSRQ, "SYSRQ");
        keyMap.put(KeyInput.KEY_RMENU, "RMENU");
        keyMap.put(KeyInput.KEY_PAUSE, "PAUSE");
        keyMap.put(KeyInput.KEY_HOME, "HOME");
        keyMap.put(KeyInput.KEY_UP, "UP");
        keyMap.put(KeyInput.KEY_PGUP, "PGUP");
        keyMap.put(KeyInput.KEY_LEFT, "LEFT");
        keyMap.put(KeyInput.KEY_RIGHT, "RIGHT");
        keyMap.put(KeyInput.KEY_END, "END");
        keyMap.put(KeyInput.KEY_DOWN, "DOWN");
        keyMap.put(KeyInput.KEY_PGDN, "PGDN");
        keyMap.put(KeyInput.KEY_INSERT, "INSERT");
        keyMap.put(KeyInput.KEY_DELETE, "DELETE");
        keyMap.put(KeyInput.KEY_LMETA, "LMETA");
        keyMap.put(KeyInput.KEY_RMETA, "RMETA");
        keyMap.put(KeyInput.KEY_APPS, "APPS");
        keyMap.put(KeyInput.KEY_POWER, "POWER");
        keyMap.put(KeyInput.KEY_SLEEP, "SLEEP");
        keyMap.put(KeyInput.KEY_LAST, "LAST");
    }

    public static String getKeyName(int keyCode) {
        return keyMap.getOrDefault(keyCode, "UNKNOWN_KEY");
    }


}

package com.onemillionworlds.tamarin.vrhands;

public enum HandSide{
    LEFT("/user/hand/left"),
    RIGHT("/user/hand/right");

    /**
     * The string that can be passed to get actions to restrict to only that hand
     */
    public final String restrictToInputString;

    HandSide(String restrictToInputString){
        this.restrictToInputString = restrictToInputString;
    }
}

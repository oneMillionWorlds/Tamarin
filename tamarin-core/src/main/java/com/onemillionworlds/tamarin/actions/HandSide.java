package com.onemillionworlds.tamarin.actions;

import org.lwjgl.openxr.EXTHandTracking;

public enum HandSide{
    LEFT("/user/hand/left", 1), // EXTHandTracking.XR_HAND_LEFT_EXT
    RIGHT("/user/hand/right", 2); // EXTHandTracking.XR_HAND_RIGHT_EXT

    /**
     * The string that can be passed to get actions to restrict to only that hand
     */
    public final String restrictToInputString;

    public final int skeletonIndex;

    HandSide(String restrictToInputString, int skeletonIndex){
        this.restrictToInputString = restrictToInputString;
        this.skeletonIndex = skeletonIndex;
    }

    public HandSide getOtherHand(){
        switch(this){
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
        }
        throw new RuntimeException("Unexpected hand side");
    }
}

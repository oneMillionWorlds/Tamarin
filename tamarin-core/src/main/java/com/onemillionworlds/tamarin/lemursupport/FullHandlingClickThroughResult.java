package com.onemillionworlds.tamarin.lemursupport;

public class FullHandlingClickThroughResult{
    /**
     * If the special handling (currently keyboard stuff) did anything
     */
    private final SpecialHandlingClickThroughResult specialHandlingClickThroughResult;

    /**
     * If the Tamarin code that tries to synthesis a lemur click for finger presses did anything
     */
    private final boolean clickRegularHandled;

    public FullHandlingClickThroughResult(SpecialHandlingClickThroughResult specialHandlingClickThroughResult, boolean clickRegularHandled){
        this.specialHandlingClickThroughResult = specialHandlingClickThroughResult;
        this.clickRegularHandled = clickRegularHandled;
    }

    /**
     * If the special handling (currently keyboard stuff) did anything
     */
    public SpecialHandlingClickThroughResult getSpecialHandlingClickThroughResult(){
        return specialHandlingClickThroughResult;
    }

    /**
     * If the Tamarin code that tries to synthesis a lemur click for finger presses did anything
     */
    public boolean isClickRegularHandled(){
        return clickRegularHandled;
    }
}

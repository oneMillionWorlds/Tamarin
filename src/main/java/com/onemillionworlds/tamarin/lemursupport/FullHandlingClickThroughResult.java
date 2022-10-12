package com.onemillionworlds.tamarin.lemursupport;

import lombok.Value;

@Value
public class FullHandlingClickThroughResult{
    /**
     * If the special handling (currently keyboard stuff) did anything
     */
    SpecialHandlingClickThroughResult specialHandlingClickThroughResult;

    /**
     * If the Tamarin code that tries to synthesis a lemur click for finger presses did anything
     */
    boolean clickRegularHandled;

}

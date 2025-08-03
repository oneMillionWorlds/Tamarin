package com.onemillionworlds.tamarin.vrhands.functions;

@FunctionalInterface
public interface FunctionRegistration{
    /**
     * End (deregister) the function, so it stops doing whatever it was doing
     */
    void endFunction();
}

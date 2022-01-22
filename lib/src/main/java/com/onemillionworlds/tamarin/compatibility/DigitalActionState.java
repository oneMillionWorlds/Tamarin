package com.onemillionworlds.tamarin.compatibility;

public class DigitalActionState{

    /**
     * The current value of this action
     */
    public final boolean state;

    /**
     * If since the last loop the value of this action has changed
     */
    public final boolean changed;

    public DigitalActionState(boolean state, boolean changed){
        this.state = state;
        this.changed = changed;
    }
}

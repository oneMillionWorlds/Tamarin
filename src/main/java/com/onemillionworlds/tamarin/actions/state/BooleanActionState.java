package com.onemillionworlds.tamarin.actions.state;

public class BooleanActionState{

    /**
     * The current value of this action
     */
    private final boolean state;

    /**
     * If since the last loop the value of this action has changed
     */
    private final boolean changed;

    public BooleanActionState(boolean state, boolean changed){
        this.state = state;
        this.changed = changed;
    }

    /**
     * The current value of this action
     */
    public boolean getState(){
        return state;
    }

    /**
     * If since the last loop the value of this action has changed
     */
    public boolean hasChanged(){
        return changed;
    }
}

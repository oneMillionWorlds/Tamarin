package com.onemillionworlds.tamarin.actions.state;

public class FloatActionState{

    /**
     * The current value of this action
     */
    public final float state;


    /**
     * If since the last loop the value of this action has changed
     */
    private final boolean changed;



    public FloatActionState(float state, boolean changed){
        this.state = state;
        this.changed = changed;
    }

    /**
     * The current value of this action, will be between 0 and 1.
     */
    public float getState(){
        return state;
    }

    /**
     * If since the last loop the value of this action has changed
     */
    public boolean hasChanged(){
        return changed;
    }
}

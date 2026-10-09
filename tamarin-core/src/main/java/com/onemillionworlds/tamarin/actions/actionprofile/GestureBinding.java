package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.handgestures.HandGesture;

/**
 * A Tamarin side (synthetic) binding of a hand gesture to an action. See
 * {@link Action.ActionBuilder#withGestureBinding(HandSide, HandGesture)}.
 */
public final class GestureBinding{
    private final HandSide handSide;
    private final HandGesture gesture;

    public GestureBinding(HandSide handSide, HandGesture gesture){
        this.handSide = handSide;
        this.gesture = gesture;
    }

    public HandSide getHandSide(){
        return handSide;
    }

    public HandGesture getGesture(){
        return gesture;
    }

    @Override
    public String toString(){
        return "GestureBinding{" + handSide + ", " + gesture.getDescription() + '}';
    }
}

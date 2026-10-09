package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.HandSide;

public class GestureBinding {
    private final HandSide handSide;
    private final Gesture gesture;

    public GestureBinding(HandSide handSide, Gesture gesture){
        this.handSide = handSide;
        this.gesture = gesture;
    }

    public HandSide getHandSide(){
        return handSide;
    }

    public Gesture getGesture(){
        return gesture;
    }

}

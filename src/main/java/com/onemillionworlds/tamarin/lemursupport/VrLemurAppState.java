package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.app.Application;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.simsilica.lemur.event.BasePickState;

public class VrLemurAppState extends BasePickState{

    public VrLemurAppState(){
        setEnabled(true);
    }

    @Override
    protected void dispatchMotion() {
        /*
         * do nothing. Motion is published by the hands themselves.
         *
         * If the usual MouseAppState is used instead of the VrLemurAppState then you get weird flicker
         * on the UI elements as the pc mouse goes invisibly over them
         */
    }

    public void dispatch( MouseButtonEvent evt ) {
        if( getSession().buttonEvent(evt.getButtonIndex(), evt.getX(), evt.getY(), evt.isPressed()) ) {
            evt.setConsumed();
        }
    }
}

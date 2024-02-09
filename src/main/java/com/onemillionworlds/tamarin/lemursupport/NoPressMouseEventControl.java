package com.onemillionworlds.tamarin.lemursupport;

import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;

/**
 * This is a MouseEventControl that doesn't do anything on press. It's useful for when you want to disable the event
 * on finger tip press (probably because a cleverer AbstractTouchControl is being used) but want this basic click
 * handler to work for pick line based picking.
 *
 * (It would be nice if this was done by passing a parameter on the MouseButtonEvent but lemur copies over the event so
 * thats not possible)
 */
public class NoPressMouseEventControl extends MouseEventControl{

    public NoPressMouseEventControl(){
    }

    public NoPressMouseEventControl(MouseListener... listeners){
        super(listeners);
    }
}

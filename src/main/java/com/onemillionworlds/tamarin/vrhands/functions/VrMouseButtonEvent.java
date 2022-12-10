package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.input.event.MouseButtonEvent;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import lombok.Getter;

public class VrMouseButtonEvent extends MouseButtonEvent{
    /**
     * The hand that triggered this click.
     */
    @Getter
    BoundHand handIssuingClick;

    public VrMouseButtonEvent(int btnIndex, boolean pressed, int x, int y, BoundHand handIssuingClick){
        super(btnIndex, pressed, x, y);
        this.handIssuingClick = handIssuingClick;
    }
}

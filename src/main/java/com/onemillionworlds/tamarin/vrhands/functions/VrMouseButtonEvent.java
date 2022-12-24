package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.input.event.MouseButtonEvent;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import lombok.Getter;

public class VrMouseButtonEvent extends MouseButtonEvent{
    /**
     * The hand that triggered this click.
     */
    @Getter
    private final BoundHand handIssuingClick;

    /**
     * The openVR action that triggered this click (useful if multiple actions are bound, and you want different behaviours)
     */
    @Getter
    private final String triggeringAction;

    public VrMouseButtonEvent(int btnIndex, boolean pressed, int x, int y, BoundHand handIssuingClick, String triggeringAction){
        super(btnIndex, pressed, x, y);
        this.handIssuingClick = handIssuingClick;
        this.triggeringAction = triggeringAction;
    }
}

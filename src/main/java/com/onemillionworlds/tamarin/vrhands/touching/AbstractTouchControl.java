package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.Optional;

/**
 * The touch control can be attached to geometries and will be informed when an index finger touches the control
 * <p>
 * This can be used for things like buttons (and has a lot of overlap with Lemur functionality)
 */
public abstract class AbstractTouchControl extends AbstractControl{

    Optional<BoundHand> touchingHand = Optional.empty();

    public void onTouch(BoundHand touchingHand){
        this.touchingHand = Optional.of(touchingHand);
    }

    public void onStopTouch(BoundHand noLongerTouchingHand){
        this.touchingHand = Optional.empty();
    }

    public boolean isBeingTouched(){
        return touchingHand.isPresent();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){
    }

    public Optional<BoundHand> getTouchingHand(){
        return this.touchingHand;
    }
}

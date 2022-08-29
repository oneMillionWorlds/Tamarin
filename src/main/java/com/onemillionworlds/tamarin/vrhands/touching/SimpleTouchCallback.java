package com.onemillionworlds.tamarin.vrhands.touching;

import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.function.Consumer;

/**
 * A simple TouchControl that when touched by a hand the call back in informed (and told which hand, which is the
 * key advantage over lemur controls)
 */
public class SimpleTouchCallback extends AbstractTouchControl{

    Consumer<BoundHand> onTouchDo;

    public SimpleTouchCallback(Consumer<BoundHand> onTouchDo){
        this.onTouchDo = onTouchDo;
    }

    @Override
    public void onTouch(BoundHand touchingHand){
        super.onTouch(touchingHand);
        onTouchDo.accept(touchingHand);
    }

    @Override
    protected void controlUpdate(float tpf){}
}

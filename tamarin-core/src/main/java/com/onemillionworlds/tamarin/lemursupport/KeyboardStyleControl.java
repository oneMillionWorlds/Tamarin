package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;

/**
 * Attached to a lemur TextField to give it a keyboard other than the global default
 * ({@link LemurSupport#keyboardStyle}). Usually added via {@link LemurSupport#setKeyboardStyle}.
 */
public class KeyboardStyleControl extends AbstractControl{

    private final KeyboardStyle keyboardStyle;

    public KeyboardStyleControl(KeyboardStyle keyboardStyle){
        this.keyboardStyle = keyboardStyle;
    }

    public KeyboardStyle getKeyboardStyle(){
        return keyboardStyle;
    }

    @Override
    protected void controlUpdate(float tpf){}

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){}
}

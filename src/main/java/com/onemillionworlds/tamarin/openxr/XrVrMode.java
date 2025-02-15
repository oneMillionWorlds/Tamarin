package com.onemillionworlds.tamarin.openxr;

import org.lwjgl.openxr.XR10;

/**
 * This determines how the composited image will be blended with the real world behind the display (if at all).
 * <p>
 * AKA it controls if the application is VR (virtual reality) or AR (Augmented Reality)
 */
public enum XrVrMode{

    /**
     * The composition layers will be displayed with no view of the physical world behind them.
     * The composited image will be interpreted as an RGB image, ignoring the composited alpha channel.
     * This is the typical mode for VR experiences, although this mode can also be supported on
     * devices that support video passthrough.
     */
    ENVIRONMENT_BLEND_MODE_OPAQUE(XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE),

    /**
     * The composition layers will be additively blended with the real world behind the display.
     * The composited image will be interpreted as an RGB image, ignoring the composited alpha channel
     * during the additive blending. This will cause black composited pixels to appear transparent. This is the typical
     * mode for an AR experience on a see-through headset with an additive display, although this mode can also be
     * supported on devices that support video passthrough.
     */
    ENVIRONMENT_BLEND_MODE_ADDITIVE(XR10.XR_ENVIRONMENT_BLEND_MODE_ADDITIVE),

    /**
     * The composition layers will be alpha-blended with the real world behind the display. The composited image will be
     * interpreted as an RGBA image, with the composited alpha channel determining each pixel’s level of blending with
     * the real world behind the display. This is the typical mode for an AR experience on a phone or headset that
     * supports video passthrough.
     */
    ENVIRONMENT_BLEND_MODE_ALPHA_BLEND(XR10.XR_ENVIRONMENT_BLEND_MODE_ALPHA_BLEND);

    final int xrValue;

    XrVrMode(int xrValue){
        this.xrValue = xrValue;
    }

    public int getXrValue(){
        return xrValue;
    }
}

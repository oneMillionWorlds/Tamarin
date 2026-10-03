package com.onemillionworlds.tamarin.openxr;

/**
 * How passthrough (seeing the real world behind the virtual scene, aka mixed reality) is being provided.
 * See {@link XrBaseAppState#setPassthroughEnabled(boolean)}.
 */
public enum PassthroughMethod{
    /**
     * Passthrough is not available (see {@link XrBaseAppState#getPassthroughUnavailableReason()} for why)
     */
    NONE,
    /**
     * The standard OpenXR approach, the runtime supports the {@link XrVrMode#ENVIRONMENT_BLEND_MODE_ALPHA_BLEND}
     * environment blend mode. Preferred where available as it is cross vendor.
     */
    ENVIRONMENT_BLEND_MODE,
    /**
     * Meta's XR_FB_passthrough extension, a passthrough layer is submitted underneath the application's layer. Used
     * where the runtime doesn't support {@link #ENVIRONMENT_BLEND_MODE}.
     */
    FB_PASSTHROUGH
}

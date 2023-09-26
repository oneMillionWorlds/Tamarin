package com.onemillionworlds.tamarin.openxr;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.openxr.EXTDebugUtils;
import org.lwjgl.openxr.EXTHandTracking;
import org.lwjgl.openxr.KHRBindingModification;
import org.lwjgl.openxr.KHROpenGLEnable;
import org.lwjgl.openxr.MNDXEGLEnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class XrSettings{

    @Getter
    @Setter
    private String applicationName = "";

    /**
     * Extenstions that will be attempted to be bound to the session.
     */
    private final Set<String> requiredXrExtensions = new HashSet<>();

    /**
     * This determines how the composited image will be blended with the real world behind the display (if at all).
     * <p>
     * AKA it controls if the application is VR (virtual reality) or AR (Augmented Reality). Note the background should be
     * black/transparent if you choose one of the AR modes.
     * <p>
     * Note; this feature is currently untested and may not work. Many headsets do not yet support AR
     */
    @Getter
    @Setter
    XrVrMode xrVrMode = XrVrMode.ENVIRONMENT_BLEND_MODE_OPAQUE;

    /**
     * If true, the main camera will follow the VR camera. If false, the main camera continue to be controlled in whatever
     * way the main application has been set up.
     */
    @Getter
    @Setter
    boolean mainCameraFollowsVrCamera = true;

    public XrSettings(){
        requiredXrExtensions.add(KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME); //openGL support
        requiredXrExtensions.add(EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME);
        requiredXrExtensions.add(MNDXEGLEnable.XR_MNDX_EGL_ENABLE_EXTENSION_NAME); //cross platform openGL support (not well supported yet but a good idea)
        requiredXrExtensions.add(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME); //bones
        requiredXrExtensions.add(KHRBindingModification.XR_KHR_BINDING_MODIFICATION_EXTENSION_NAME); //required by XR_EXT_DPAD_BINDING_EXTENSION_NAME
        // below can be used once JME upgrades to LWJGL 3.3.3 or higher
        //requiredXrExtensions.add(EXTDpadBinding.XR_EXT_DPAD_BINDING_EXTENSION_NAME); //treating joysticks as dpads
    }

    /**
     * OpenXR provides much of its functionality through extensions. This method allows you to add extensions that you would
     * like in addition to what Tamarin natively supports. Be aware if you are adding extensions you may need to interact
     * with LWJGL directly to use them.
     */
    @SuppressWarnings("unused")
    public void addRequiredXrExtension(String extensionName){
        requiredXrExtensions.add(extensionName);
    }

    public Set<String> getRequiredXrExtensions(){
        return Collections.unmodifiableSet(requiredXrExtensions);
    }
}

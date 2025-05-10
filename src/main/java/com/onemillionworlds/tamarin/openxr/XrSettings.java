package com.onemillionworlds.tamarin.openxr;

import org.lwjgl.openxr.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class XrSettings{

    DrawMode drawMode = DrawMode.AUTOSELECT;

    private String applicationName = "";

    /**
     * Samples are used in MSAA to provide antialiasing at the boundaries of objects. Should be 1,2, 4, 8 etc
     * <p>
     * -1 means "use the JME settings"
     * </p>
     */
    private int samples = -1;

    /**
     * Extensions that will be attempted to be bound to the session.
     */
    private final Set<String> requiredXrExtensions = new HashSet<>();

    /**
     * This determines how the composited image will be blended with the real world behind the display (if at all).
     * <p>
     * AKA it controls if the application is VR (virtual reality) or AR (Augmented Reality). Note the background should be
     * black/transparent if you choose one of the AR modes.
     * </p>
     * <p>
     *     It is likely you'll need to add a passthrough extension, e.g. FBPassthrough.XR_FB_PASSTHROUGH_EXTENSION_NAME
     * </p>
     * <p>
     *      <b>EXPERIMENTAL</b> Note; this feature is currently untested and may not work. Many headsets do not yet support AR
     * </p>
     *
     */
    XrVrMode initialXrVrMode = XrVrMode.ENVIRONMENT_BLEND_MODE_OPAQUE;

    /**
     * If true the ears will no longer follow the main camera but will instead follow the VR cameras mid positions.
     */
    boolean earsFollowVrCamera = true;

    /**
     * If true, the main camera will follow the VR camera. If false, the main camera continue to be controlled in whatever
     * way the main application has been set up.
     */
    boolean mainCameraFollowsVrCamera = true;

    XRVersion xrApiVersion = new XRVersion(1, 0, 43);

    public XrSettings(){
        requiredXrExtensions.add(KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME); //openGL support
        requiredXrExtensions.add(EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME);
        requiredXrExtensions.add(MNDXEGLEnable.XR_MNDX_EGL_ENABLE_EXTENSION_NAME); //cross platform openGL support (not well supported yet but a good idea)
        requiredXrExtensions.add(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME); //bones
        requiredXrExtensions.add(KHRBindingModification.XR_KHR_BINDING_MODIFICATION_EXTENSION_NAME); //required by XR_EXT_DPAD_BINDING_EXTENSION_NAME
        // below can be replaced with EXTDpadBinding.XR_EXT_DPAD_BINDING_EXTENSION_NAME once JME upgrades to LWJGL 3.3.3 or higher
        requiredXrExtensions.add("XR_EXT_dpad_binding"); //treating joysticks as dpads
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

    /**
     * Primarily for testing purposes, this allows you to remove an extension that would otherwise be requested to be used.
     * @param extensionName the extension to not use
     */
    @SuppressWarnings("unused")
    public void removeRequiredExtension(String extensionName){
        requiredXrExtensions.remove(extensionName);
    }

    public Set<String> getRequiredXrExtensions(){
        return Collections.unmodifiableSet(requiredXrExtensions);
    }

    public DrawMode getDrawMode(){
        return drawMode;
    }

    public void setDrawMode(DrawMode drawMode){
        this.drawMode = drawMode;
    }

    /**
     * The version of the OpenXR Runtime that is being requested to be used
     */
    public void setXrApiVersion(int major, int minor, int patch){
        xrApiVersion = new XRVersion(major, minor, patch);
    }

    /**
     * Retrieves the name of the application.
     *
     * @return the name of the application as a string
     */
    public String getApplicationName(){
        return applicationName;
    }

    /**
     * Sets the name of the application.
     *
     * @param applicationName the name of the application as a string
     */
    public void setApplicationName(String applicationName){
        this.applicationName = applicationName;
    }

    /**
     * Retrieves the number of samples used for anti-aliasing.
     * Samples are used in MSAA to provide antialiasing at the boundaries of objects. Should be 1,2, 4, 8 etc
     * <p>
     * -1 means "use the JME settings"
     * </p>
     * @return the number of samples for anti-aliasing
     */
    public int getSamples(){
        return samples;
    }

    /**
     * Sets the number of samples for multi-sample anti-aliasing (MSAA).
     * Samples are used in MSAA to provide smoother edges at the boundaries of objects.
     * Common values are 1, 2, 4, 8, etc.
     *
     * @param samples the number of samples to use for MSAA. A value of -1 indicates
     *                that the application's default settings should be used.
     */
    public void setSamples(int samples){
        this.samples = samples;
    }

    /**
     * Retrieves the initial XR/VR mode in which the application starts.
     * This can be used to determine how the application blends the composited image
     * with the real world (e.g., virtual reality or augmented reality modes).
     *
     * <p>
     *      <b>EXPERIMENTAL</b> Note; this feature is currently untested and may not work. Many headsets do not yet support AR
     * </p>
     *
     * @return the initial XR/VR mode, represented as an instance of {@link XrVrMode}.
     */
    public XrVrMode getInitialXrVrMode(){
        return initialXrVrMode;
    }

    /**
     * Sets the initial XR/VR mode for the application.
     * The XR/VR mode determines how the composited image will be blended
     * with the real world, such as virtual reality (VR) or augmented reality (AR).
     *
     * <p>
     *      <b>EXPERIMENTAL</b> Note; this feature is currently untested and may not work. Many headsets do not yet support AR
     * </p>
     * <p>
     *     It is likely you'll need to add a passthrough extension, e.g. FBPassthrough.XR_FB_PASSTHROUGH_EXTENSION_NAME
     * </p>
     * @param initialXrVrMode the mode (e.g., XR or VR) in which the application will start.
     *                        Represented as an instance of {@link XrVrMode}.
     */
    public void setInitialXrVrMode(XrVrMode initialXrVrMode){
        this.initialXrVrMode = initialXrVrMode;
    }

    /**
     * Determines whether the virtual reality (VR) camera influences the position of audio listening ears.
     * This can be used to align the audio listener's position with the VR camera for a more immersive audio experience.
     *
     * @return true if the ears follow the VR camera's position, false otherwise
     */
    public boolean isEarsFollowVrCamera(){
        return earsFollowVrCamera;
    }

    /**
     * Sets whether the audio listening ears should follow the virtual reality (VR) camera's position.
     * When enabled, the position of the VR camera will determine the audio listening position,
     * providing a more immersive sound experience in VR.
     *
     * @param earsFollowVrCamera true if the ears should follow the VR camera's position, false otherwise
     */
    public void setEarsFollowVrCamera(boolean earsFollowVrCamera){
        this.earsFollowVrCamera = earsFollowVrCamera;
    }

    /**
     * Determines whether the main camera follows the virtual reality (VR) camera's position.
     * This can be used to align the main (i.e. desktop) camera with the VR camera for a synchronized experience
     * within the application.
     *
     * @return true if the main camera follows the VR camera's position, false otherwise
     */
    public boolean isMainCameraFollowsVrCamera(){
        return mainCameraFollowsVrCamera;
    }

    /**
     * Sets whether the main camera should follow the virtual reality (VR) camera's position.
     * When enabled, the main (i.e. desktop) camera will align with the VR camera to provide a synchronized
     * experience between the VR environment and the main camera view.
     *
     * @param mainCameraFollowsVrCamera true to enable the main camera to follow the VR camera's position,
     *                                  false to disable it.
     */
    public void setMainCameraFollowsVrCamera(boolean mainCameraFollowsVrCamera){
        this.mainCameraFollowsVrCamera = mainCameraFollowsVrCamera;
    }

    /**
     * Deprecated, use {@link XrSettings#setInitialXrVrMode(XrVrMode)}
     * @param initialXrVrMode the mode (i.e. XR or VR) that the application STARTS in. Future updates should
     *                        be made via {@link XrSettings#setInitialXrVrMode(XrVrMode)}
     */
    @Deprecated(forRemoval = true)
    public void setXrVrMode(XrVrMode initialXrVrMode){
        this.initialXrVrMode = initialXrVrMode;
    }

    public static class XRVersion{
        private final int major;
        private final int minor;
        private final int patch;

        public XRVersion(int major, int minor, int patch){
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }
    }
}

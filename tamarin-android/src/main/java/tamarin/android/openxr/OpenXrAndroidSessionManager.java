package tamarin.android.openxr;

import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.onemillionworlds.tamarin.openxr.DrawMode;
import com.onemillionworlds.tamarin.openxr.EyeSide;
import com.onemillionworlds.tamarin.openxr.InProgressXrRender;
import com.onemillionworlds.tamarin.openxr.OpenXrDeviceNotAvailableException;
import com.onemillionworlds.tamarin.openxr.SessionState;
import com.onemillionworlds.tamarin.openxr.SwapchainImage;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.openxr.XrVrMode;
import com.onemillionworlds.tamarin.openxrbindings.XR10;
import com.onemillionworlds.tamarin.openxrbindings.XR10Constants;
import com.onemillionworlds.tamarin.openxrbindings.XR10Utils;
import com.onemillionworlds.tamarin.openxrbindings.XrApiLayerProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrApplicationInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrCompositionLayerProjection;
import com.onemillionworlds.tamarin.openxrbindings.XrCompositionLayerProjectionView;
import com.onemillionworlds.tamarin.openxrbindings.XrDebugUtilsMessengerCallbackDataEXT;
import com.onemillionworlds.tamarin.openxrbindings.XrDebugUtilsMessengerCreateInfoEXT;
import com.onemillionworlds.tamarin.openxrbindings.XrEventDataBaseHeader;
import com.onemillionworlds.tamarin.openxrbindings.XrEventDataBuffer;
import com.onemillionworlds.tamarin.openxrbindings.XrEventDataEventsLost;
import com.onemillionworlds.tamarin.openxrbindings.XrEventDataInstanceLossPending;
import com.onemillionworlds.tamarin.openxrbindings.XrEventDataSessionStateChanged;
import com.onemillionworlds.tamarin.openxrbindings.XrExtensionProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrFovf;
import com.onemillionworlds.tamarin.openxrbindings.XrFrameBeginInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrFrameEndInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrFrameState;
import com.onemillionworlds.tamarin.openxrbindings.XrFrameWaitInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrGraphicsBindingOpenGLESAndroidKHR;
import com.onemillionworlds.tamarin.openxrbindings.XrGraphicsRequirementsOpenGLESKHR;
import com.onemillionworlds.tamarin.openxrbindings.XrInstanceCreateInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrPosef;
import com.onemillionworlds.tamarin.openxrbindings.XrQuaternionf;
import com.onemillionworlds.tamarin.openxrbindings.XrRect2Di;
import com.onemillionworlds.tamarin.openxrbindings.XrReferenceSpaceCreateInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSessionBeginInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSessionCreateInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSwapchainCreateInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSwapchainImageAcquireInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSwapchainImageReleaseInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSwapchainImageWaitInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSwapchainSubImage;
import com.onemillionworlds.tamarin.openxrbindings.XrSystemGetInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrSystemGraphicsProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrSystemProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrSystemTrackingProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrVector3f;
import com.onemillionworlds.tamarin.openxrbindings.XrView;
import com.onemillionworlds.tamarin.openxrbindings.XrViewConfigurationView;
import com.onemillionworlds.tamarin.openxrbindings.XrViewLocateInfo;
import com.onemillionworlds.tamarin.openxrbindings.XrViewState;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrEnvironmentBlendMode;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrFormFactor;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrReferenceSpaceType;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrResult;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrStructureType;
import com.onemillionworlds.tamarin.openxrbindings.handles.XrDebugUtilsMessengerEXT;
import com.onemillionworlds.tamarin.openxrbindings.handles.XrInstance;
import com.onemillionworlds.tamarin.openxrbindings.handles.XrSession;
import com.onemillionworlds.tamarin.openxrbindings.handles.XrSpace;
import com.onemillionworlds.tamarin.openxrbindings.handles.XrSwapchain;
import com.onemillionworlds.tamarin.openxrbindings.memory.ByteBufferView;
import com.onemillionworlds.tamarin.openxrbindings.memory.IntBufferView;
import com.onemillionworlds.tamarin.openxrbindings.memory.LongBufferView;
import com.onemillionworlds.tamarin.openxrbindings.memory.MemoryStack;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrViewConfigurationType;
import com.onemillionworlds.tamarin.openxrbindings.memory.PointerBufferView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.onemillionworlds.tamarin.openxrbindings.XR10.xrEnumerateApiLayerProperties;
import static com.onemillionworlds.tamarin.openxrbindings.XR10.xrEnumerateInstanceExtensionProperties;
import static com.onemillionworlds.tamarin.openxrbindings.XR10Constants.XR_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static com.onemillionworlds.tamarin.openxrbindings.memory.MemoryUtil.NULL;

import android.opengl.GLES20;


public class OpenXrAndroidSessionManager {

    public static final String XR_KHR_OPENGL_ENABLE_EXTENSION_NAME = "XR_KHR_opengl_enable";

    /**
     * In pixels the width of the swapchain (aka the width of the eye-screen)
     */
    int swapchainWidth;
    /**
     * In pixels the height of the swapchain (aka the width of the eye-screen)
     */
    int swapchainHeight;

    private static final Logger LOGGER = Logger.getLogger(OpenXrAndroidSessionManager.class.getName());

    XrInstance xrInstance;
    boolean missingXrDebug;

    boolean missingHandTracking;
    /**
     * the EGL bindings are cross-platform but not well-supported, use if available
     */
    boolean useEglGraphicsBinding;

    /**
    * This is the ID of the form factor, e.g. oculus quest.
    */
    long systemID;
    long window;
    int glColorFormat;

    XrSession xrSession;
    XrDebugUtilsMessengerEXT xrDebugMessenger;
    XrSpace xrAppSpace;
    /**
     * Two views representing the form factor’s two primary displays, which map to a left-eye and right-eye view.
     * This configuration requires two views in XrViewConfigurationProperties and two views in each XrCompositionLayerProjection layer.
     * View index 0 must represent the left eye and view index 1 must represent the right eye.
     */
    XrViewConfigurationType viewConfigType = XrViewConfigurationType.VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;

    XrViewConfigurationView.Buffer viewConfigs;
    XrView.Buffer views; //Each view represents an eye in the headset with views[0] being left and views[1] being right
    Swapchain[] swapchains;  //One swapchain per view

    SessionState sessionState;

    XrEventDataBuffer eventDataBuffer = XrEventDataBuffer.calloc()
            .type$Default();

    private final XrSettings xrSettings;

    private final AppSettings regularSettings;

    private long predictedFrameTime;

    /**
     * The data on the  extensions that were secured by the OpenXR instance. A map of the extension name and if
     * it was successfully loaded. This is provided for library clients to check if the requested extensions were
     * available before using optional features (or giving a user-friendly error message if they aren't optional.
     */
    private Map<String, Boolean> extensionsLoaded = new HashMap<>();

    /**
     * Because of buffering the OpenXR swapchains ask for a series of images to be used to write to, these are
     * the buffers that are used to write to those images.
     */
    private final Map<Integer, FrameBuffer> frameBuffers_direct = new HashMap<>();

    /**
     * These bufferes are used to write to a texture, then copy the texture into the swapchain images. This is an
     * alternative to the direct buffers, and is slower but may support features like MSAA.
     */
    private final EnumMap<EyeSide, FrameBuffer> frameBuffers_copied = new EnumMap<>(EyeSide.class);

    private final static Map<Integer, Image.Format> DESIRED_SWAPCHAIN_FORMATS = new LinkedHashMap<>();

    private final Renderer renderer;

    private XrVrMode xrVrBlendMode = XrVrMode.ENVIRONMENT_BLEND_MODE_OPAQUE;

    public static final int
            GL_RGBA16F = 0x881A,
            GL_RGB10_A2 = 0x8059;

    static {
        DESIRED_SWAPCHAIN_FORMATS.put(GL_RGBA16F, Image.Format.RGBA16F);
        DESIRED_SWAPCHAIN_FORMATS.put(GL_RGB10_A2, Image.Format.RGB10A2);
    }

    public static OpenXrAndroidSessionManager createOpenXrSession(long windowHandle, XrSettings xrSettings, AppSettings regularSettings, Renderer renderer){
        OpenXrAndroidSessionManager openXrSessionManager = new OpenXrAndroidSessionManager(xrSettings, regularSettings, renderer);
        openXrSessionManager.window = windowHandle;
        openXrSessionManager.createOpenXRInstance();
        openXrSessionManager.determineOpenXRSystem();
        openXrSessionManager.initializeAndBindOpenGL();
        openXrSessionManager.createXRReferenceSpace();
        openXrSessionManager.createXRSwapchains();

        openXrSessionManager.pollEvents();
        return openXrSessionManager;
    }


    private OpenXrAndroidSessionManager(XrSettings xrSettings, AppSettings regularSettings, Renderer renderer){
        this.xrSettings = xrSettings;
        this.regularSettings = regularSettings;
        this.renderer = renderer;
    }

    public int getSwapchainWidth(){
        return swapchainWidth;
    }

    public int getSwapchainHeight(){
        return swapchainHeight;
    }

    public SessionState getSessionState(){
        return sessionState;
    }

    public XrSession getXrSession(){
        return xrSession;
    }

    public long getPredictedFrameTime(){
        return predictedFrameTime;
    }

    /**
     * The data on the  extensions that were secured by the OpenXR instance. A map of the extension name and if
     * it was successfully loaded. This is provided for library clients to check if the requested extensions were
     * available before using optional features (or giving a user-friendly error message if they aren't optional.
     */
    public Map<String, Boolean> getExtensionsLoaded(){
        return extensionsLoaded;
    }

    public boolean isSessionRunning(){
        return sessionState.isAtLeastReady();
    }

    public boolean isSessionFocused(){
        return sessionState == SessionState.FOCUSED;
    }

    public void setXrVrBlendMode(XrVrMode xrVrBlendMode){
        this.xrVrBlendMode = xrVrBlendMode;
    }

    private void createOpenXRInstance() {
        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            // skip the layers check as android doesn't provide any debugging layers
            ExtensionsCheckResult extensionsCheckResult = makeExtensionsCheck(stack, xrSettings.getRequiredXrExtensions());
            this.extensionsLoaded = extensionsCheckResult.extensionsLoaded();

            for(String extension : extensionsCheckResult.extensionsLoaded().keySet()){
                if (extensionsCheckResult.extensionsLoaded().get(extension)){
                    LOGGER.fine("OpenXR extension " + extension + " loaded");
                }else{
                    LOGGER.warning("OpenXR extension " + extension + " NOT loaded");
                }
            }

            missingXrDebug = extensionsCheckResult.missingXrDebug();
            useEglGraphicsBinding = extensionsCheckResult.useEglGraphicsBinding();

            if(extensionsCheckResult.missingOpenGL()) {
                throw new IllegalStateException("OpenXR library does not provide required extension: " + XR_KHR_OPENGL_ENABLE_EXTENSION_NAME);
            }
            missingHandTracking = extensionsCheckResult.missingHandTracking();

            XrSettings.XRVersion xrVersion = xrSettings.xrApiVersion;

            XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.calloc(stack)
                    .type$Default()
                    .next(NULL)
                    .createFlags(0)
                    .applicationInfo(XrApplicationInfo.calloc(stack)
                            .applicationName(stack.utf8(xrSettings.getApplicationName()))
                            .apiVersion(XR10Utils.xrMakeVersion(xrVersion.getMajor(), xrVersion.getMinor(), xrVersion.getPatch())))
                    .enabledExtensionNames(extensionsCheckResult.extensionsToLoadBuffer().address());

            XrInstance.HandleBuffer pp = XrInstance.create(1,stack);
            checkResponseCode(XR10.xrCreateInstance(createInfo, pp));
            xrInstance = new XrInstance(pp.get(0));
        }
    }

    /**
     * Determine what the system type (e.g. Oculus quest) of the XR device is
     */
    public void determineOpenXRSystem() {
        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            //Get headset type
            LongBufferView systemIdBuffer = stack.mallocLong(1);

            checkResponseCode(XR10.xrGetSystem(
                    xrInstance,
                    XrSystemGetInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .formFactor(XrFormFactor.FORM_FACTOR_HEAD_MOUNTED_DISPLAY),
                    systemIdBuffer
            ));

            systemID = systemIdBuffer.get(0);
            if (systemID == 0) {
                throw new IllegalStateException("No compatible headset detected");
            }
            LOGGER.fine("Headset found with System ID: " + systemID);
        }
    }

    public String getSystemName(){
        try (MemoryStack stack = MemoryStack.stackGet().push()){
            XrSystemProperties systemProperties = XrSystemProperties.malloc(stack)
                    .type(XrStructureType.XR_TYPE_SYSTEM_PROPERTIES);
            checkResponseCode(XR10.xrGetSystemProperties(xrInstance, systemID, systemProperties));
            return systemProperties.systemNameString();
        }
    }


    public void initializeAndBindOpenGL() {
        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            //Initialize OpenXR's OpenGL compatability
            XrGraphicsRequirementsOpenGLESKHR graphicsRequirements = XrGraphicsRequirementsOpenGLESKHR.malloc(stack)
                    .type(XrStructureType.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_ES_KHR)
                    .next(NULL)
                    .minApiVersionSupported(0)
                    .maxApiVersionSupported(0);
            checkResponseCode(XR10.xrGetOpenGLESGraphicsRequirementsKHR(xrInstance, systemID, graphicsRequirements));

            int minMajorVersion = XR10Utils.xrExtractMajorVersion(graphicsRequirements.minApiVersionSupported());
            int minMinorVersion = XR10Utils.xrExtractMinorVersion(graphicsRequirements.minApiVersionSupported());

            int maxMajorVersion = XR10Utils.xrExtractMajorVersion(graphicsRequirements.maxApiVersionSupported());
            int maxMinorVersion = XR10Utils.xrExtractMinorVersion(graphicsRequirements.maxApiVersionSupported());
            LOGGER.info("The OpenXR runtime supports OpenGL " + minMajorVersion + "." + minMinorVersion
                    + " to OpenGL " + maxMajorVersion + "." + maxMinorVersion);
            // Check if OpenGL version is supported by OpenXR runtime
            // In Android, we need to parse the GL_VERSION string to get the version numbers
            String versionString = GLES20.glGetString(GLES20.GL_VERSION);
            LOGGER.info("OpenGL ES version: " + versionString);

            int actualMajorVersion = -1;
            int actualMinorVersion = -1;

            if (versionString != null) {
                // Extract version numbers from the string
                // The format is typically "OpenGL ES X.Y ..."
                String[] parts = versionString.split(" ");
                if (parts.length >= 3) {
                    String[] versionParts = parts[2].split("\\.");
                    if (versionParts.length >= 2) {
                        try {
                            actualMajorVersion = Integer.parseInt(versionParts[0]);
                            actualMinorVersion = Integer.parseInt(versionParts[1]);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Failed to parse OpenGL ES version: " + versionString);
                        }
                    }
                }
            }

            if(actualMajorVersion == -1 || actualMinorVersion == -1){
                throw new IllegalStateException("Failed to parse OpenGL ES version: " + versionString);
            }

            if (minMajorVersion > actualMajorVersion || (minMajorVersion == actualMajorVersion && minMinorVersion > actualMinorVersion)) {
                throw new IllegalStateException(
                        "The OpenXR runtime supports only OpenGL " + minMajorVersion + "." + minMinorVersion +
                                " and later, but we got OpenGL " + actualMajorVersion + "." + actualMinorVersion
                );
            }

            if (actualMajorVersion > maxMajorVersion || (actualMajorVersion == maxMajorVersion && actualMinorVersion > maxMinorVersion)) {
                throw new IllegalStateException(
                        "The OpenXR runtime supports only OpenGL " + maxMajorVersion + "." + minMajorVersion +
                                " and earlier, but we got OpenGL " + actualMajorVersion + "." + actualMinorVersion
                );
            }

            //Bind the OpenGL context to the OpenXR instance and create the session
            XrGraphicsBindingOpenGLESAndroidKHR graphicsBinding = XrAndroidUtils.createGraphicsBindingOpenGL(stack, window, useEglGraphicsBinding);
            XrSession.HandleBuffer sessionPointerBuffer = XrSession.create(1,stack);
            checkResponseCode(XR10.xrCreateSession(
                    xrInstance,
                    XrSessionCreateInfo.malloc(stack)
                            .type$Default()
                            .next(graphicsBinding.address())
                            .createFlags(0)
                            .systemId(systemID),
                    sessionPointerBuffer
            ));
            xrSession = new XrSession(sessionPointerBuffer.get(0));
            if (!missingXrDebug) {
                XrDebugUtilsMessengerCreateInfoEXT ciDebugUtils = XrDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .type$Default()
                        .messageSeverities(
                                XR10Constants.XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                                        XR10Constants.XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                        XR10Constants.XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageTypes(
                                XR10Constants.XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        XR10Constants.XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        XR10Constants.XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT |
                                        XR10Constants.XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
                        )
                        .userCallback((messageSeverity, messageTypes, pCallbackData, userData) -> {
                            XrDebugUtilsMessengerCallbackDataEXT callbackData = XrDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

                            LOGGER.warning("XR Debug Utils: " + callbackData.messageString());
                            return 0;
                        });

                LOGGER.info("OpenXR debug utils enabled");
                checkResponseCode(EXTDebugUtils.xrCreateDebugUtilsMessengerEXT(xrInstance, ciDebugUtils, sessionPointerBuffer));
                xrDebugMessenger = new XrDebugUtilsMessengerEXT(sessionPointerBuffer.get(0), xrInstance);
            }else{
                LOGGER.info("OpenXR debug utils not available");
            }
        }
    }

    /**
     * Creates the main reference space for the application. This is a stage space that is ideal for standing
     * experiences (although not bad for seating experiences either). The origin is on the floor at the center of the
     * bounding rectangle, with +Y up, and the X and Z axes aligned with the rectangle edges
     */
    public void createXRReferenceSpace() {
        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            XrSpace.HandleBuffer pp = XrSpace.create(1, stack);

            checkResponseCode(XR10.xrCreateReferenceSpace(
                    xrSession,
                    XrReferenceSpaceCreateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .referenceSpaceType(XrReferenceSpaceType.REFERENCE_SPACE_TYPE_STAGE)
                            .poseInReferenceSpace(XrPosef.malloc(stack)
                                    .orientation(XrQuaternionf.malloc(stack)
                                            .x(0)
                                            .y(0)
                                            .z(0)
                                            .w(1))
                                    .position$(XrVector3f.calloc(stack))),
                    pp
            ));

            xrAppSpace = new XrSpace(pp.get(0));
        }
    }

    /**
     * Swapchains are for double buffering or triple buffering, techniques used to reduce screen tearing and improve visual performance in XR environments.
     */
    public void createXRSwapchains() {
        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            XrSystemProperties systemProperties = XrSystemProperties.calloc(stack);
            systemProperties.type(XrStructureType.XR_TYPE_SYSTEM_PROPERTIES);
            checkResponseCode(XR10.xrGetSystemProperties(xrInstance, systemID, systemProperties));

            String headsetName = systemProperties.systemNameString();
            int vendor = systemProperties.vendorId();
            LOGGER.info("Headset name:"+ headsetName + " vendor: " + vendor);

            XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
            LOGGER.info("Headset orientationTracking:" + trackingProperties.orientationTracking() + "positionTracking: " + trackingProperties.positionTracking());

            XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
            LOGGER.info("Headset MaxWidth: " + graphicsProperties.maxSwapchainImageWidth()
                    + " MaxHeight: " + graphicsProperties.maxSwapchainImageHeight()
                    + "MaxLayerCount: " + graphicsProperties.maxLayerCount());

            IntBufferView viewCountPointer = stack.mallocInt(1);

            checkResponseCode(XR10.xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType, 0, viewCountPointer, null));

            int numberOfViews = viewCountPointer.get(0);

            viewConfigs = XrViewConfigurationView.calloc(numberOfViews);

            for(int i = 0; i < numberOfViews; i++){
                XrViewConfigurationView xrViewConfigurationView = viewConfigs.get(i);
                xrViewConfigurationView.type(XrStructureType.XR_TYPE_VIEW_CONFIGURATION_VIEW);
                xrViewConfigurationView.next(NULL);
            }


            checkResponseCode(XR10.xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType,0, viewCountPointer, viewConfigs));
            int viewCountNumber = viewCountPointer.get(0);

            views = XrView.calloc(viewCountNumber);

            for(int i=0;i<viewCountNumber;i++){
                xrViewBuffer.get(i).type(XrStructureType.XR_TYPE_VIEW);
            }

            if (viewCountNumber != 2){
                throw new IllegalStateException("Expected 2 views, got " + viewCountNumber);
            }

            IntBufferView formatCountPointer = stack.mallocInt(1);

            checkResponseCode(XR10.xrEnumerateSwapchainFormats(xrSession, 0, formatCountPointer, null));
            LongBufferView swapchainFormats = stack.mallocLong(formatCountPointer.get(0));
            checkResponseCode(XR10.xrEnumerateSwapchainFormats(xrSession, formatCountPointer.get(0),formatCountPointer, swapchainFormats));

            List<Integer> availableFormats = new ArrayList<>();
            for (int i = 0; i < swapchainFormats.getBufferView().capacity(); i++) {
                availableFormats.add((int) swapchainFormats.get(i));
            }

            for (int glFormatIter : DESIRED_SWAPCHAIN_FORMATS.keySet()) {
                if (availableFormats.contains(glFormatIter)){
                    glColorFormat = glFormatIter;
                    break;
                }
            }

            if (glColorFormat == 0) {
                throw new IllegalStateException("No compatable swapchain / framebuffer format available, available formats: " + availableFormats);
            }else{
                LOGGER.info("Selected colour format " + glColorFormat + " from options " + availableFormats);
            }

            swapchains = new Swapchain[viewCountNumber];
            for (int i = 0; i < viewCountNumber; i++) {
                XrViewConfigurationView viewConfig = viewConfigs.get(i);

                XrSwapchainCreateInfo swapchainCreateInfo = XrSwapchainCreateInfo.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .createFlags(0)
                        .usageFlags(XR10Constants.XR_SWAPCHAIN_USAGE_SAMPLED_BIT | XR10Constants.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
                        .format(glColorFormat)
                        .sampleCount(viewConfig.recommendedSwapchainSampleCount())
                        .width(viewConfig.recommendedImageRectWidth())
                        .height(viewConfig.recommendedImageRectHeight())
                        .faceCount(1)
                        .arraySize(1)
                        .mipCount(1);

                this.swapchainHeight = viewConfig.recommendedImageRectHeight();
                this.swapchainWidth = viewConfig.recommendedImageRectWidth();

                XrSwapchain.HandleBuffer swapchainHanglePointerBuffer = XrSwapchain.create(1,stack);
                checkResponseCode(XR10.xrCreateSwapchain(xrSession, swapchainCreateInfo, swapchainHanglePointerBuffer));

                XrSwapchain swapchainHandle = new XrSwapchain(swapchainHanglePointerBuffer.get(0));

                checkResponseCode(XR10.xrEnumerateSwapchainImages(swapchainHandle, 0, viewCountPointer, null));
                int imageCount = viewCountPointer.get(0);

                XrSwapchainImageOpenGLKHR.Buffer swapchainImageBuffer = XrUtils.fill(
                        XrSwapchainImageOpenGLKHR.calloc(imageCount),
                        XrSwapchainImageOpenGLKHR.TYPE,
                        KHROpenGLEnable.XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
                );

                checkResponseCode(XR10.xrEnumerateSwapchainImages(swapchainHandle, imageCount, viewCountPointer, XrSwapchainImageBaseHeader.create(swapchainImageBuffer.address(), swapchainImageBuffer.capacity())));

                swapchains[i] = new Swapchain(swapchainHandle, swapchainCreateInfo.width(), swapchainCreateInfo.height(), swapchainImageBuffer);
            }

        }
    }

    /**
     * Polls events and updates the state of the application.
     * @return if the application should exit
     */
    private boolean pollEvents() {
        GLFW.glfwPollEvents();
        XrEventDataBaseHeader event = readNextOpenXREvent();
        if (event == null) {
            return false;
        }

        do {
            switch (event.type()) {
                case XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING: {
                    XrEventDataInstanceLossPending instanceLossPending = XrEventDataInstanceLossPending.create(event.address());
                    LOGGER.severe("XrEventDataInstanceLossPending by " + instanceLossPending.lossTime());

                    return true;
                }
                case XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED: {
                    XrEventDataSessionStateChanged sessionStateChangedEvent = XrEventDataSessionStateChanged.create(event.address());
                    return handleSessionStateChangedEvent(sessionStateChangedEvent);
                }
                case XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED:
                    break;
                case XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING:
                default: {
                    LOGGER.info("Ignoring event type: " + event.type());
                    break;
                }
            }
            event = readNextOpenXREvent();
        }
        while (event != null);

        return false;
    }

    private XrEventDataBaseHeader readNextOpenXREvent() {
        // It is sufficient to just clear the XrEventDataBuffer header to
        // XR_TYPE_EVENT_DATA_BUFFER rather than recreate it every time
        eventDataBuffer.clear();
        eventDataBuffer.type$Default();
        XrResult result = XR10.xrPollEvent(xrInstance, eventDataBuffer);
        if (result == XrResult.SUCCESS) {
            if (eventDataBuffer.type() == XrStructureType.XR_TYPE_EVENT_DATA_EVENTS_LOST) {
                XrEventDataEventsLost dataEventsLost = XrEventDataEventsLost.create(eventDataBuffer.address());
                LOGGER.info(dataEventsLost.lostEventCount() + " events lost");
            }
            return XrEventDataBaseHeader.create(eventDataBuffer.address());
        }
        if (result == XrResult.EVENT_UNAVAILABLE) {
            return null;
        }
        throw new IllegalStateException("[XrResult failure in xrPollEvent]:" +  result.toString());
    }

    boolean handleSessionStateChangedEvent(XrEventDataSessionStateChanged stateChangedEvent) {
        SessionState oldState = sessionState;
        sessionState = SessionState.fromXRValue(stateChangedEvent.state().getValue());

        LOGGER.info("XrEventDataSessionStateChanged: state " + oldState + "->" + sessionState + " session=" + stateChangedEvent.session() + " time=" + stateChangedEvent.time());
        if ((stateChangedEvent.session().isNullHandle()) && (!stateChangedEvent.session().equals(xrSession))) {
            System.err.println("XrEventDataSessionStateChanged for unknown session");
            return false;
        }

        switch (sessionState) {
            case READY: {
                assert (xrSession != null);
                try (MemoryStack stack = MemoryStack.stackGet().push()) {
                    checkResponseCode(XR10.xrBeginSession(
                            xrSession,
                            XrSessionBeginInfo.malloc(stack)
                                    .type$Default()
                                    .next(NULL)
                                    .primaryViewConfigurationType(viewConfigType)
                    ));
                    return false;
                }
            }
            case FOCUSED: {
                return false;
            }
            case STOPPING: {
                assert (xrSession != null);
                checkResponseCode(XR10.xrEndSession(xrSession));
                return false;
            }
            case EXITING: {
                //this is a user requesting to exit the application
                return true;
            }
            case LOSS_PENDING: {
                //possibly could try to restart?
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * This begins the XR frame and reports where the cameras should be for this frame.
     * The frame is left open, and must be completed within the post render phase.
     */
    public InProgressXrRender startXrFrame(){
        pollEvents();
        if (!isSessionRunning()){
            return InProgressXrRender.NO_XR_FRAME;
        }

        try (MemoryStack stack = MemoryStack.stackGet().push()) {
            XrFrameState frameState = XrFrameState.calloc(stack)
                    .type$Default();

            checkResponseCode(XR10.xrWaitFrame(
                    xrSession,
                    XrFrameWaitInfo.calloc(stack)
                            .type$Default(),
                    frameState
            ));

            checkResponseCode(XR10.xrBeginFrame(
                    xrSession,
                    XrFrameBeginInfo.calloc(stack)
                            .type$Default()
            ));

            XrViewState viewState = XrViewState.calloc(stack)
                    .type$Default();

            IntBufferView pi = stack.mallocInt(1);
            checkResponseCode(XR10.xrLocateViews(
                    xrSession,
                    XrViewLocateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .viewConfigurationType(viewConfigType)
                            .displayTime(frameState.predictedDisplayTime())
                            .space(xrAppSpace),
                    viewState,
                    views.capacity(),
                    pi,
                    views
            ));

            this.predictedFrameTime = frameState.predictedDisplayTime();

            if ((viewState.viewStateFlags() & XR10Constants.XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                    (viewState.viewStateFlags() & XR10Constants.XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
                return InProgressXrRender.NO_XR_FRAME;  // There is no valid tracking poses for the views.
            }

            int viewCountOutput = pi.get(0);
            assert (viewCountOutput == views.capacity());
            assert (viewCountOutput == viewConfigs.capacity());
            assert (viewCountOutput == swapchains.length);

            InProgressXrRender.EyePositionData leftEye = null;
            InProgressXrRender.EyePositionData rightEye = null;
            for(int i = 0; i < 2; i++){
                XrPosef pose = views.get(i).pose();
                XrFovf fov = views.get(i).fov();
                XrVector3f position = pose.position$();
                XrQuaternionf orientation = pose.orientation();
                InProgressXrRender.EyePositionData eyePositionData = new InProgressXrRender.EyePositionData(
                        XrAndroidUtils.convertOpenXRToJme(position),
                        XrAndroidUtils.convertOpenXRQuaternionToJme(orientation),
                        new InProgressXrRender.FieldOfViewData(fov.angleLeft(), fov.angleRight(), fov.angleUp(), fov.angleDown())
                );
                if (i == 0){
                    leftEye = eyePositionData;
                }else{
                    rightEye = eyePositionData;
                }
            }

            FrameBuffer leftFrameBuffer =null;
            FrameBuffer rightFrameBuffer =null;
            int leftSwapchainImageIndex = -1;
            int rightSwapchainImageIndex = -1;

            if (frameState.shouldRender() == XR10Constants.XR_TRUE) {

                // set up to render view to the appropriate part of the swapchain image.
                for (int viewIndex = 0; viewIndex < 2; viewIndex++) {
                    // Each view has a separate swapchain which is acquired, rendered to, and released.
                    Swapchain viewSwapchain = swapchains[viewIndex];

                    checkResponseCode(XR10.xrAcquireSwapchainImage(
                            viewSwapchain.handle,
                            XrSwapchainImageAcquireInfo.calloc(stack)
                                    .type$Default(),
                            pi
                    ));
                    int swapchainImageIndex = pi.get(0);



                    checkResponseCode(XR10.xrWaitSwapchainImage(
                            viewSwapchain.handle,
                            XrSwapchainImageWaitInfo.malloc(stack)
                                    .type$Default()
                                    .next(NULL)
                                    .timeout(XR10Constants.XR_INFINITE_DURATION)
                    ));

                    int image = viewSwapchain.images.get(swapchainImageIndex).image();

                    if(viewIndex == 0){
                        leftSwapchainImageIndex = image;
                    }else{
                        rightSwapchainImageIndex = image;
                    }

                    FrameBuffer frameBuffer;
                    if(xrSettings.getDrawMode() == DrawMode.DIRECT){
                        frameBuffer = getOrCreateDirectFrameBuffer(image);
                    }else if(xrSettings.getDrawMode() == DrawMode.BLITTED){
                        frameBuffer = getOrCreateCopiedFrameBuffer(viewIndex == 0 ? EyeSide.LEFT : EyeSide.RIGHT);
                    } else {
                        throw new IllegalStateException("Unsupported draw mode " + xrSettings.getDrawMode());
                    }

                    if (viewIndex == 0){
                        leftFrameBuffer = frameBuffer;
                    }else{
                        rightFrameBuffer = frameBuffer;
                    }

                }

            }

            return new InProgressXrRender(true, frameState.shouldRender() == XR10Constants.XR_TRUE, frameState.predictedDisplayTime(), leftEye, rightEye, leftFrameBuffer, rightFrameBuffer, leftSwapchainImageIndex, rightSwapchainImageIndex);
        }
    }

    private FrameBuffer getOrCreateDirectFrameBuffer(int swapchainImageId){
        return frameBuffers_direct.computeIfAbsent(swapchainImageId, id -> {
            Image.Format format = DESIRED_SWAPCHAIN_FORMATS.get(glColorFormat);
            Texture2D texture = new Texture2D(new SwapchainImage(id, format, swapchainWidth, swapchainHeight));
            FrameBuffer frameBuffer = new FrameBuffer(swapchainWidth, swapchainHeight, 1);
            frameBuffer.setName("OpenXR direct buffer " + id);
            frameBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
            frameBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
            return frameBuffer;
        });
    }

    public FrameBuffer getOrCreateCopiedFrameBuffer(EyeSide side){
        return frameBuffers_copied.computeIfAbsent(side, eyeSide -> {
            Image.Format format = DESIRED_SWAPCHAIN_FORMATS.get(glColorFormat);
            int samples = xrSettings.getSamples();
            FrameBuffer eyeBuffer = new FrameBuffer(swapchainWidth, swapchainHeight, samples);
            eyeBuffer.setName("OpenXR " + side + " eye buffer copied mode");
            Texture2D texture = new Texture2D(swapchainWidth, swapchainHeight, samples, format);
            Texture2D msDepth = new Texture2D(swapchainWidth, swapchainHeight, samples, Image.Format.Depth);
            eyeBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
            eyeBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(msDepth));
            return eyeBuffer;
        });
    }

    private void glErrorCheck(String message){
        int error = GL11.glGetError();
        if(error != GL11.GL_NO_ERROR){
            System.out.println("OpenGL Error: " + error + " " + message);
        }
    }

    /**
     * Blit from one framebuffer to another, resolving the multisampled buffer to the non-multisampled buffer.
     * (This is usually done for MSAA).
     */
    private void resolveDownMultiSampled(FrameBuffer multisampledFrameBuffer, FrameBuffer nonSampledFrameBuffer){
        glErrorCheck("Pre Resolve");
        renderer.clearClipRect();
        renderer.copyFrameBuffer(multisampledFrameBuffer, nonSampledFrameBuffer, true, false);
        glErrorCheck("Resolve");
    }

    public void presentFrameBuffersToOpenXr(InProgressXrRender continuation){

        if (!continuation.inProgressXr){
            return;
        }

        try (MemoryStack stack = MemoryStack.stackGet().push()) {

            XrCompositionLayerProjection layerProjection = XrCompositionLayerProjection.calloc(stack)
                    .type$Default();

            PointerBufferView layers = stack.callocPointer(1);
            boolean didRender = false;

            if (continuation.isShouldRender()) {

                XrCompositionLayerProjectionView.Buffer projectionLayerViews = XrCompositionLayerProjectionView.calloc(2, stack);
                for (int i = 0; i < projectionLayerViews.capacity(); i++) {
                    projectionLayerViews.get(i).type(XrStructureType.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW);
                }

                for(int viewIndex=0;viewIndex<2;viewIndex++){
                    Swapchain viewSwapchain = swapchains[viewIndex];
                    if(xrSettings.getDrawMode() == DrawMode.BLITTED){
                        // In blitted mode the JME scene has been rendered to an intermediate buffer and must be blitted
                        // into the final swapchain buffer (which resolves samples as it goes)
                        int viewSwapchainImageIndex = viewIndex == 0 ? continuation.getLeftSwapchainImageIndex() : continuation.getRightSwapchainImageIndex();
                        FrameBuffer viewBuffer = viewIndex == 0 ? continuation.getLeftBufferToRenderTo() : continuation.getRightBufferToRenderTo();
                        resolveDownMultiSampled(viewBuffer, getOrCreateDirectFrameBuffer(viewSwapchainImageIndex));
                    }

                    XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex);

                    projectionLayerView.pose(views.get(viewIndex).pose());
                    projectionLayerView.fov(views.get(viewIndex).fov());
                    XrSwapchainSubImage subimage = projectionLayerView.subImage();

                    subimage.swapchain(viewSwapchain.handle);
                    XrRect2Di sumImageREct = subimage.imageRect();
                    sumImageREct.offset().x(0).y(0);
                    sumImageREct.extent().width(viewSwapchain.width).height(viewSwapchain.height);

                    checkResponseCode(XR10.xrReleaseSwapchainImage(
                            viewSwapchain.handle,
                            XrSwapchainImageReleaseInfo.calloc(stack)
                                    .type$Default()
                    ));

                }

                layerProjection.space(xrAppSpace);
                layerProjection.views(projectionLayerViews);
                layers.set(0, layerProjection.address());
                didRender = true;
            } else {
                LOGGER.fine("Shouldn't render");
            }

            checkResponseCode(XR10.xrEndFrame(
                    xrSession,
                    XrFrameEndInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .displayTime(continuation.getPredictedDisplayTime())
                            .environmentBlendMode(XrEnvironmentBlendMode.fromValue(xrVrBlendMode.getXrValue()))
                            .layers(didRender ? layers.address() : NULL)
                            .layerCount(didRender ? layers.capacity() : 0)
            ));
        }
    }


    /**
     * Extensions:
     * - Optional features that extend the core OpenXR API capabilities.
     * - Directly integrated into the OpenXR runtime and API.
     * - Enable support for additional hardware, software features, or APIs like OpenGL.
     * - Must be explicitly enabled when creating an OpenXR instance.
     */
    private ExtensionsCheckResult makeExtensionsCheck(MemoryStack stack, Collection<String> desiredExtensions){
        IntBufferView numberOfExtensionsPointer = stack.mallocInt(1);

        checkResponseCode(xrEnumerateInstanceExtensionProperties((ByteBufferView) null, 0, numberOfExtensionsPointer, null));
        int numExtensions = numberOfExtensionsPointer.get(0);

        XrExtensionProperties.Buffer properties = XrAndroidUtils.createExtensionProperties(stack, numExtensions);

        checkResponseCode(xrEnumerateInstanceExtensionProperties((ByteBufferView)null, numExtensions, numberOfExtensionsPointer, properties));

        PointerBufferView extensions = PointerBufferView.createPointerBufferView(desiredExtensions.size()); //note must have space for the max possible no. of extensions

        Map<String, Boolean> extensionsLoaded = new HashMap<>();
        desiredExtensions.forEach(e -> extensionsLoaded.put(e, false));

        int extensionIndex = 0;
        for (int i = 0; i < numExtensions; i++) {
            XrExtensionProperties prop = properties.get(i);
            String extensionName = prop.extensionNameString();

            if (extensionsLoaded.containsKey(extensionName)) {
                extensionsLoaded.put(extensionName, true);
                extensions.set(extensionIndex, prop.extensionName().address());
                extensionIndex++;
            }
        }
        extensions.getBufferView().flip();

        return new ExtensionsCheckResult(extensions, extensionsLoaded);
    }

    public void checkResponseCode(XrResult result) throws IllegalStateException {
        checkResponseCode(null, result);
    }

    public void checkResponseCode(String context, XrResult result) throws IllegalStateException {
        if (result == XrResult.SUCCESS) {
            return;
        }
        String contextString = context == null ? "" : " Context: " + context;

        if (result == XrResult.ERROR_FORM_FACTOR_UNAVAILABLE){
            throw new OpenXrDeviceNotAvailableException("Open XR returned an error code " + result + " " + contextString, result.getValue());
        } else if (result == XrResult.ERROR_API_VERSION_UNSUPPORTED) {
            throw new RuntimeException("Open XR API version not supported " + result + contextString);
        }
        throw new OpenXrException("Open XR returned an error code " + result + " " + contextString, result.getValue());

    }

    public void destroy(){
        eventDataBuffer.free();
        views.free();
        viewConfigs.free();
        for (Swapchain swapchain : swapchains) {
            XR10.xrDestroySwapchain(swapchain.handle);
            swapchain.images.free();
        }
        XR10.xrDestroySpace(xrAppSpace);
        if (xrDebugMessenger != null) {
            XR10.xrDestroyDebugUtilsMessengerEXT(xrDebugMessenger);
        }
        XR10.xrDestroySession(xrSession);
        XR10.xrDestroyInstance(xrInstance);
    }

    public static class OpenXrException extends RuntimeException {
        private final int errorCode;

        public OpenXrException(String s, int errorCode) {
            super(s);
            this.errorCode = errorCode;
        }

        public int getErrorCode(){
            return errorCode;
        }
    }

    private record ExtensionsCheckResult(
            PointerBufferView extensionsToLoadBuffer,
            Map<String, Boolean> extensionsLoaded){
        public boolean missingOpenGL(){
            return !extensionsLoaded.get(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME);
        }
        public boolean missingXrDebug(){
            return !extensionsLoaded.get(XR_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }
        public boolean missingHandTracking(){
            return !Optional.ofNullable(extensionsLoaded.get(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME)).orElse(false);
        }

        /**
         * the EGL bindings are cross-platform but not well-supported, use if available
         */
        public boolean useEglGraphicsBinding(){
            return extensionsLoaded.get(XR_MNDX_EGL_ENABLE_EXTENSION_NAME);
        }
    }

    private record Swapchain (
        XrSwapchain handle,
        int width,
        int height,
        XrSwapchainImageOpenGLKHR.Buffer images
    ){}

}

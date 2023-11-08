package com.onemillionworlds.tamarin.openxr;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL31;
import org.lwjgl.openxr.EXTDebugUtils;
import org.lwjgl.openxr.EXTHandTracking;
import org.lwjgl.openxr.KHROpenGLEnable;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrApiLayerProperties;
import org.lwjgl.openxr.XrApplicationInfo;
import org.lwjgl.openxr.XrCompositionLayerProjection;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.openxr.XrDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.openxr.XrDebugUtilsMessengerEXT;
import org.lwjgl.openxr.XrEventDataBaseHeader;
import org.lwjgl.openxr.XrEventDataBuffer;
import org.lwjgl.openxr.XrEventDataEventsLost;
import org.lwjgl.openxr.XrEventDataInstanceLossPending;
import org.lwjgl.openxr.XrEventDataSessionStateChanged;
import org.lwjgl.openxr.XrExtensionProperties;
import org.lwjgl.openxr.XrFovf;
import org.lwjgl.openxr.XrFrameBeginInfo;
import org.lwjgl.openxr.XrFrameEndInfo;
import org.lwjgl.openxr.XrFrameState;
import org.lwjgl.openxr.XrFrameWaitInfo;
import org.lwjgl.openxr.XrGraphicsRequirementsOpenGLKHR;
import org.lwjgl.openxr.XrInstance;
import org.lwjgl.openxr.XrInstanceCreateInfo;
import org.lwjgl.openxr.XrPosef;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrReferenceSpaceCreateInfo;
import org.lwjgl.openxr.XrSession;
import org.lwjgl.openxr.XrSessionBeginInfo;
import org.lwjgl.openxr.XrSessionCreateInfo;
import org.lwjgl.openxr.XrSpace;
import org.lwjgl.openxr.XrSwapchain;
import org.lwjgl.openxr.XrSwapchainCreateInfo;
import org.lwjgl.openxr.XrSwapchainImageAcquireInfo;
import org.lwjgl.openxr.XrSwapchainImageBaseHeader;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;
import org.lwjgl.openxr.XrSwapchainImageReleaseInfo;
import org.lwjgl.openxr.XrSwapchainImageWaitInfo;
import org.lwjgl.openxr.XrSystemGetInfo;
import org.lwjgl.openxr.XrSystemGraphicsProperties;
import org.lwjgl.openxr.XrSystemProperties;
import org.lwjgl.openxr.XrSystemTrackingProperties;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.openxr.XrView;
import org.lwjgl.openxr.XrViewConfigurationView;
import org.lwjgl.openxr.XrViewLocateInfo;
import org.lwjgl.openxr.XrViewState;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.lwjgl.openxr.EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.openxr.KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME;
import static org.lwjgl.openxr.MNDXEGLEnable.XR_MNDX_EGL_ENABLE_EXTENSION_NAME;
import static org.lwjgl.openxr.XR10.XR_CURRENT_API_VERSION;
import static org.lwjgl.openxr.XR10.XR_MAX_RESULT_STRING_SIZE;
import static org.lwjgl.openxr.XR10.XR_SUCCEEDED;
import static org.lwjgl.openxr.XR10.XR_VERSION_MAJOR;
import static org.lwjgl.openxr.XR10.XR_VERSION_MINOR;
import static org.lwjgl.openxr.XR10.xrCreateInstance;
import static org.lwjgl.openxr.XR10.xrEnumerateApiLayerProperties;
import static org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties;
import static org.lwjgl.system.MemoryStack.stackMalloc;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memLengthNT1;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.Struct;

import java.nio.LongBuffer;

public class OpenXrSessionManager{

    /**
     * In pixels the width of the swapchain (aka the width of the eye-screen)
     */
    @Getter
    int swapchainWidth;
    /**
     * In pixels the height of the swapchain (aka the width of the eye-screen)
     */
    @Getter
    int swapchainHeight;

    private static final Logger LOGGER = Logger.getLogger(OpenXrSessionManager.class.getName());

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

    @Getter
    XrSession xrSession;
    XrDebugUtilsMessengerEXT xrDebugMessenger;
    XrSpace xrAppSpace;
    /**
     * Two views representing the form factor’s two primary displays, which map to a left-eye and right-eye view.
     * This configuration requires two views in XrViewConfigurationProperties and two views in each XrCompositionLayerProjection layer.
     * View index 0 must represent the left eye and view index 1 must represent the right eye.
     */
    int viewConfigType = XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;

    XrViewConfigurationView.Buffer viewConfigs;
    XrView.Buffer views; //Each view represents an eye in the headset with views[0] being left and views[1] being right
    Swapchain[] swapchains;  //One swapchain per view

    @Getter
    boolean sessionRunning;

    @Getter
    SessionState sessionState;

    XrEventDataBuffer eventDataBuffer = XrEventDataBuffer.calloc()
            .type$Default();

    private final XrSettings xrSettings;

    @Getter
    private long predictedFrameTime;

    /**
     * The data on the  extensions that were secured by the OpenXR instance. A map of the extension name and if
     * it was successfully loaded. This is provided for library clients to check if the requested extensions were
     * available before using optional features (or giving a user-friendly error message if they aren't optional.
     */
    @Getter
    private Map<String, Boolean> extensionsLoaded = new HashMap<>();

    /**
     * Because of buffering the OpenXR swapchains ask for a series of images to be used to write to, these are
     * the buffers that are used to write to those images.
     */
    private final Map<Integer, FrameBuffer> frameBuffers = new HashMap<>();

    private static Map<Integer, Image.Format> DESIRED_SWAPCHAIN_FORMATS = new LinkedHashMap<>();

    static {
        DESIRED_SWAPCHAIN_FORMATS.put(GL30.GL_RGBA16F, Image.Format.RGBA16F);
        DESIRED_SWAPCHAIN_FORMATS.put(GL11.GL_RGB10_A2, Image.Format.RGB10A2);

        //fall backs with not enough bits for color; expect banding
        DESIRED_SWAPCHAIN_FORMATS.put(GL11.GL_RGBA8, Image.Format.RGBA8);
        DESIRED_SWAPCHAIN_FORMATS.put(GL31.GL_RGBA8_SNORM, Image.Format.RGBA8I); //not sure if this is right
        //other formats that were not mentioned in the helloOpenXRGL example
        DESIRED_SWAPCHAIN_FORMATS.put(GL11.GL_RGB8, Image.Format.RGB8);
        DESIRED_SWAPCHAIN_FORMATS.put(GL11.GL_RGB5_A1, Image.Format.RGB5A1);
    }

    public static OpenXrSessionManager createOpenXrSession(long windowHandle, XrSettings xrSettings){
        OpenXrSessionManager openXrSessionManager = new OpenXrSessionManager(xrSettings);
        openXrSessionManager.window = windowHandle;
        openXrSessionManager.createOpenXRInstance();
        openXrSessionManager.determineOpenXRSystem();
        openXrSessionManager.initializeAndBindOpenGL();
        openXrSessionManager.createXRReferenceSpace();
        openXrSessionManager.createXRSwapchains();

        openXrSessionManager.pollEvents();
        return openXrSessionManager;
    }

    private OpenXrSessionManager(XrSettings xrSettings){
        this.xrSettings = xrSettings;
    }

    private void createOpenXRInstance() {
        try (MemoryStack stack = stackPush()) {
            LayerCheckResult layerCheckResult = makeLayersCheck(stack);
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

            XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .createFlags(0)
                    .applicationInfo(XrApplicationInfo.calloc(stack)
                            .applicationName(stack.UTF8(xrSettings.getApplicationName()))
                            .apiVersion(XR_CURRENT_API_VERSION))
                    .enabledApiLayerNames(layerCheckResult.wantedLayers())
                    .enabledExtensionNames(extensionsCheckResult.extensionsToLoadBuffer());

            PointerBuffer pp = stack.mallocPointer(1);
            checkResponseCode(xrCreateInstance(createInfo, pp));
            xrInstance = new XrInstance(pp.get(0), createInfo);
        }
    }

    /**
     * Determine what the system type (e.g. Oculus quest) of the XR device is
     */
    public void determineOpenXRSystem() {
        try (MemoryStack stack = stackPush()) {
            //Get headset type
            LongBuffer systemIdBuffer = stack.longs(0);

            checkResponseCode(XR10.xrGetSystem(
                    xrInstance,
                    XrSystemGetInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .formFactor(XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY),
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
        try (MemoryStack stack = MemoryStack.stackPush()){
            XrSystemProperties systemProperties = XrSystemProperties.malloc(stack)
                    .type(XR10.XR_TYPE_SYSTEM_PROPERTIES);
            checkResponseCode(XR10.xrGetSystemProperties(xrInstance, systemID, systemProperties));
            return systemProperties.systemNameString();
        }
    }


    public void initializeAndBindOpenGL() {
        try (MemoryStack stack = stackPush()) {
            //Initialize OpenXR's OpenGL compatability
            XrGraphicsRequirementsOpenGLKHR graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .minApiVersionSupported(0)
                    .maxApiVersionSupported(0);
            KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR(xrInstance, systemID, graphicsRequirements);

            int minMajorVersion = XR_VERSION_MAJOR(graphicsRequirements.minApiVersionSupported());
            int minMinorVersion = XR_VERSION_MINOR(graphicsRequirements.minApiVersionSupported());

            int maxMajorVersion = XR_VERSION_MAJOR(graphicsRequirements.maxApiVersionSupported());
            int maxMinorVersion = XR_VERSION_MINOR(graphicsRequirements.maxApiVersionSupported());
            LOGGER.info("The OpenXR runtime supports OpenGL " + minMajorVersion + "." + minMinorVersion
                    + " to OpenGL " + maxMajorVersion + "." + maxMinorVersion);
            // Check if OpenGL version is supported by OpenXR runtime
            int actualMajorVersion = GL11.glGetInteger(GL30.GL_MAJOR_VERSION);
            int actualMinorVersion = GL11.glGetInteger(GL30.GL_MINOR_VERSION);

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
            Struct graphicsBinding = XrUtils.createGraphicsBindingOpenGL(stack, window, useEglGraphicsBinding);
            PointerBuffer sessionPointerBuffer = stack.mallocPointer(1);
            checkResponseCode(XR10.xrCreateSession(
                    xrInstance,
                    XrSessionCreateInfo.malloc(stack)
                            .type$Default()
                            .next(graphicsBinding.address())
                            .createFlags(0)
                            .systemId(systemID),
                    sessionPointerBuffer
            ));
            xrSession = new XrSession(sessionPointerBuffer.get(0), xrInstance);
            if (!missingXrDebug) {
                XrDebugUtilsMessengerCreateInfoEXT ciDebugUtils = XrDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .type$Default()
                        .messageSeverities(
                                EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                                        EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                        EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageTypes(
                                EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT |
                                        EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
                        )
                        .userCallback((messageSeverity, messageTypes, pCallbackData, userData) -> {
                            XrDebugUtilsMessengerCallbackDataEXT callbackData = XrDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                            System.out.println("XR Debug Utils: " + callbackData.messageString());
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
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);

            checkResponseCode(XR10.xrCreateReferenceSpace(
                    xrSession,
                    XrReferenceSpaceCreateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .referenceSpaceType(XR10.XR_REFERENCE_SPACE_TYPE_STAGE)
                            .poseInReferenceSpace(XrPosef.malloc(stack)
                                    .orientation(XrQuaternionf.malloc(stack)
                                            .x(0)
                                            .y(0)
                                            .z(0)
                                            .w(1))
                                    .position$(XrVector3f.calloc(stack))),
                    pp
            ));

            xrAppSpace = new XrSpace(pp.get(0), xrSession);
        }
    }

    /**
     * Swapchains are for double buffering or triple buffering, techniques used to reduce screen tearing and improve visual performance in XR environments.
     */
    public void createXRSwapchains() {
        try (MemoryStack stack = stackPush()) {
            XrSystemProperties systemProperties = XrSystemProperties.calloc(stack);
            MemoryUtil.memPutInt(systemProperties.address(), XR10.XR_TYPE_SYSTEM_PROPERTIES);
            checkResponseCode(XR10.xrGetSystemProperties(xrInstance, systemID, systemProperties));

            System.out.printf("Headset name:%s vendor:%d \n",
                    MemoryUtil.memUTF8(MemoryUtil.memAddress(systemProperties.systemName())),
                    systemProperties.vendorId());

            XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
            System.out.printf("Headset orientationTracking:%b positionTracking:%b \n",
                    trackingProperties.orientationTracking(),
                    trackingProperties.positionTracking());

            XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
            System.out.printf("Headset MaxWidth:%d MaxHeight:%d MaxLayerCount:%d \n",
                    graphicsProperties.maxSwapchainImageWidth(),
                    graphicsProperties.maxSwapchainImageHeight(),
                    graphicsProperties.maxLayerCount());

            IntBuffer viewCountPointer = stack.mallocInt(1);

            checkResponseCode(XR10.xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType, viewCountPointer, null));
            viewConfigs = XrUtils.fill(
                    XrViewConfigurationView.calloc(viewCountPointer.get(0)), // use calloc() rather than malloc() to ensure the next field is correctly initialized
                    XrViewConfigurationView.TYPE,
                    XR10.XR_TYPE_VIEW_CONFIGURATION_VIEW
            );

            checkResponseCode(XR10.xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType, viewCountPointer, viewConfigs));
            int viewCountNumber = viewCountPointer.get(0);

            views = XrUtils.fill(
                    XrView.calloc(viewCountNumber),
                    XrView.TYPE,
                    XR10.XR_TYPE_VIEW
            );

            if (viewCountNumber != 2){
                throw new IllegalStateException("Expected 2 views, got " + viewCountNumber);
            }

            IntBuffer formatCountPointer = stack.mallocInt(1);

            checkResponseCode(XR10.xrEnumerateSwapchainFormats(xrSession, formatCountPointer, null));
            LongBuffer swapchainFormats = stack.mallocLong(formatCountPointer.get(0));
            checkResponseCode(XR10.xrEnumerateSwapchainFormats(xrSession, formatCountPointer, swapchainFormats));

            List<Integer> availableFormats = new ArrayList<>();
            for (int i = 0; i < swapchainFormats.limit(); i++) {
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
                        .usageFlags(XR10.XR_SWAPCHAIN_USAGE_SAMPLED_BIT | XR10.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
                        .format(glColorFormat)
                        .sampleCount(viewConfig.recommendedSwapchainSampleCount())
                        .width(viewConfig.recommendedImageRectWidth())
                        .height(viewConfig.recommendedImageRectHeight())
                        .faceCount(1)
                        .arraySize(1)
                        .mipCount(1);

                this.swapchainHeight = viewConfig.recommendedImageRectHeight();
                this.swapchainWidth = viewConfig.recommendedImageRectWidth();

                PointerBuffer swapchainHanglePointerBuffer = stack.mallocPointer(1);
                checkResponseCode(XR10.xrCreateSwapchain(xrSession, swapchainCreateInfo, swapchainHanglePointerBuffer));

                XrSwapchain swapchainHandle = new XrSwapchain(swapchainHanglePointerBuffer.get(0), xrSession);

                checkResponseCode(XR10.xrEnumerateSwapchainImages(swapchainHandle, viewCountPointer, null));
                int imageCount = viewCountPointer.get(0);

                XrSwapchainImageOpenGLKHR.Buffer swapchainImageBuffer = XrUtils.fill(
                        XrSwapchainImageOpenGLKHR.create(imageCount),
                        XrSwapchainImageOpenGLKHR.TYPE,
                        KHROpenGLEnable.XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
                );

                checkResponseCode(XR10.xrEnumerateSwapchainImages(swapchainHandle, viewCountPointer, XrSwapchainImageBaseHeader.create(swapchainImageBuffer.address(), swapchainImageBuffer.capacity())));

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
                case XR10.XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING: {
                    XrEventDataInstanceLossPending instanceLossPending = XrEventDataInstanceLossPending.create(event.address());
                    LOGGER.severe("XrEventDataInstanceLossPending by " + instanceLossPending.lossTime());

                    return true;
                }
                case XR10.XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED: {
                    XrEventDataSessionStateChanged sessionStateChangedEvent = XrEventDataSessionStateChanged.create(event.address());
                    return handleSessionStateChangedEvent(sessionStateChangedEvent);
                }
                case XR10.XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED:
                    break;
                case XR10.XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING:
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
        int result = XR10.xrPollEvent(xrInstance, eventDataBuffer);
        if (result == XR10.XR_SUCCESS) {
            if (eventDataBuffer.type() == XR10.XR_TYPE_EVENT_DATA_EVENTS_LOST) {
                XrEventDataEventsLost dataEventsLost = XrEventDataEventsLost.create(eventDataBuffer.address());
                LOGGER.info(dataEventsLost.lostEventCount() + " events lost");
            }
            return XrEventDataBaseHeader.create(eventDataBuffer.address());
        }
        if (result == XR10.XR_EVENT_UNAVAILABLE) {
            return null;
        }
        throw new IllegalStateException(String.format("[XrResult failure %d in xrPollEvent]", result));
    }

    boolean handleSessionStateChangedEvent(XrEventDataSessionStateChanged stateChangedEvent) {
        SessionState oldState = sessionState;
        sessionState = SessionState.fromXRValue(stateChangedEvent.state());

        System.out.printf("XrEventDataSessionStateChanged: state %s->%s session=%d time=%d\n", oldState, sessionState, stateChangedEvent.session(), stateChangedEvent.time());

        if ((stateChangedEvent.session() != NULL) && (stateChangedEvent.session() != xrSession.address())) {
            System.err.println("XrEventDataSessionStateChanged for unknown session");
            return false;
        }

        switch (sessionState) {
            case READY: {
                assert (xrSession != null);
                try (MemoryStack stack = stackPush()) {
                    checkResponseCode(XR10.xrBeginSession(
                            xrSession,
                            XrSessionBeginInfo.malloc(stack)
                                    .type$Default()
                                    .next(NULL)
                                    .primaryViewConfigurationType(viewConfigType)
                    ));
                    sessionRunning = true;
                    return false;
                }
            }
            case STOPPING: {
                assert (xrSession != null);
                sessionRunning = false;
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
        if (!sessionRunning){
            return InProgressXrRender.NO_XR_FRAME;
        }


        try (MemoryStack stack = stackPush()) {
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

            IntBuffer pi = stack.mallocInt(1);
            checkResponseCode(XR10.xrLocateViews(
                    xrSession,
                    XrViewLocateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .viewConfigurationType(viewConfigType)
                            .displayTime(frameState.predictedDisplayTime())
                            .space(xrAppSpace),
                    viewState,
                    pi,
                    views
            ));

            this.predictedFrameTime = frameState.predictedDisplayTime();

            if ((viewState.viewStateFlags() & XR10.XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                    (viewState.viewStateFlags() & XR10.XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
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
                        XrUtils.convertOpenXRToJme(position),
                        XrUtils.convertOpenXRQuaternionToJme(orientation),
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
            if (frameState.shouldRender()){

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
                                    .timeout(XR10.XR_INFINITE_DURATION)
                    ));

                    int image = viewSwapchain.images.get(swapchainImageIndex).image();

                    FrameBuffer frameBuffer = getOrCreateFrameBuffer(image);

                    if (viewIndex == 0){
                        leftFrameBuffer = frameBuffer;
                    }else{
                        rightFrameBuffer = frameBuffer;
                    }

                }

            }

            return new InProgressXrRender(true, frameState.shouldRender(), frameState.predictedDisplayTime(), leftEye, rightEye, leftFrameBuffer, rightFrameBuffer);
        }
    }

    private FrameBuffer getOrCreateFrameBuffer(int swapchainImageId){
        return frameBuffers.computeIfAbsent(swapchainImageId, id -> {
            Image.Format format = DESIRED_SWAPCHAIN_FORMATS.get(glColorFormat);
            Texture2D texture = new Texture2D(new SwapchainImage(id, format, swapchainWidth, swapchainHeight));
            FrameBuffer frameBuffer = new FrameBuffer(swapchainWidth, swapchainHeight, 1);
            frameBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
            frameBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
            return frameBuffer;
        });
    }

    public void presentFrameBuffersToOpenXr(InProgressXrRender continuation){

        if (!continuation.inProgressXr){
            return;
        }


        try (MemoryStack stack = stackPush()) {

            XrCompositionLayerProjection layerProjection = XrCompositionLayerProjection.calloc(stack)
                    .type$Default();

            PointerBuffer layers = stack.callocPointer(1);
            boolean didRender = false;

            if (continuation.isShouldRender()) {

                XrCompositionLayerProjectionView.Buffer projectionLayerViews = XrUtils.fill(
                        XrCompositionLayerProjectionView.calloc(2, stack),
                        XrCompositionLayerProjectionView.TYPE,
                        XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
                );

                for(int viewIndex=0;viewIndex<2;viewIndex++){

                    Swapchain viewSwapchain = swapchains[viewIndex];

                    XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex)
                            .pose(views.get(viewIndex).pose())
                            .fov(views.get(viewIndex).fov())
                            .subImage(si -> si
                                    .swapchain(viewSwapchain.handle)
                                    .imageRect(rect -> rect
                                            .offset(offset -> offset
                                                    .x(0)
                                                    .y(0))
                                            .extent(extent -> extent
                                                    .width(viewSwapchain.width)
                                                    .height(viewSwapchain.height)
                                            )));

                    checkResponseCode(XR10.xrReleaseSwapchainImage(
                            viewSwapchain.handle,
                            XrSwapchainImageReleaseInfo.calloc(stack)
                                    .type$Default()
                    ));

                }
                GL11.glFlush();

                layerProjection.space(xrAppSpace);
                layerProjection.views(projectionLayerViews);
                layers.put(0, layerProjection.address());
                didRender = true;
            } else {
                System.out.println("Shouldn't render");
            }

            checkResponseCode(XR10.xrEndFrame(
                    xrSession,
                    XrFrameEndInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .displayTime(continuation.getPredictedDisplayTime())
                            .environmentBlendMode(xrSettings.getXrVrMode().getXrValue())
                            .layers(didRender ? layers : null)
                            .layerCount(didRender ? layers.remaining() : 0)
            ));
        }
    }

    /*
     * Layers:
     * - Optional components that add functionality or debugging features.
     * - Act as intermediaries between the application and the OpenXR runtime.
     * - Can validate or intercept API calls for debugging or profiling.
     * - Examples include validation layers that check API usage for correctness.
     */
    private LayerCheckResult makeLayersCheck(MemoryStack stack){
        IntBuffer numberOfLayersPointer = stack.mallocInt(1);

        boolean hasCoreValidationLayer = false;
        checkResponseCode(xrEnumerateApiLayerProperties(numberOfLayersPointer, null));
        int numLayers = numberOfLayersPointer.get(0);

        XrApiLayerProperties.Buffer pLayers = XrUtils.prepareApiLayerProperties(stack, numLayers);
        checkResponseCode(xrEnumerateApiLayerProperties(numberOfLayersPointer, pLayers));
        LOGGER.fine("No. of XR layers available: " + numLayers);
        for (int index = 0; index < numLayers; index++) {
            XrApiLayerProperties layer = pLayers.get(index);

            String layerName = layer.layerNameString();
            System.out.println(layerName);
            if (layerName.equals("XR_APILAYER_LUNARG_core_validation")) {
                hasCoreValidationLayer = true;
            }
        }

        PointerBuffer wantedLayers;
        if (hasCoreValidationLayer) {
            wantedLayers = stack.callocPointer(1);
            wantedLayers.put(0, stack.UTF8("XR_APILAYER_LUNARG_core_validation"));
            LOGGER.info("Enabling XR core validation");
        } else {
            LOGGER.info("XR core validation not available");
            wantedLayers = null;
        }
        return new LayerCheckResult(wantedLayers, hasCoreValidationLayer);
    }


    /**
     * Extensions:
     * - Optional features that extend the core OpenXR API capabilities.
     * - Directly integrated into the OpenXR runtime and API.
     * - Enable support for additional hardware, software features, or APIs like OpenGL.
     * - Must be explicitly enabled when creating an OpenXR instance.
     */
    private ExtensionsCheckResult makeExtensionsCheck(MemoryStack stack, Collection<String> desiredExtensions){
        IntBuffer numberOfExtensionsPointer = stack.mallocInt(1);

        checkResponseCode(xrEnumerateInstanceExtensionProperties((ByteBuffer)null, numberOfExtensionsPointer, null));
        int numExtensions = numberOfExtensionsPointer.get(0);

        XrExtensionProperties.Buffer properties = XrUtils.createExtensionProperties(stack, numExtensions);

        checkResponseCode(xrEnumerateInstanceExtensionProperties((ByteBuffer)null, numberOfExtensionsPointer, properties));

        PointerBuffer extensions = stack.mallocPointer(desiredExtensions.size()); //note must have space for the max possible no. of extensions

        Map<String, Boolean> extensionsLoaded = new HashMap<>();
        desiredExtensions.forEach(e -> extensionsLoaded.put(e, false));

        for (int i = 0; i < numExtensions; i++) {
            XrExtensionProperties prop = properties.get(i);
            String extensionName = prop.extensionNameString();

            if (extensionsLoaded.containsKey(extensionName)) {
                extensionsLoaded.put(extensionName, true);
                extensions.put(prop.extensionName());
            }
        }
        extensions.flip();

        return new ExtensionsCheckResult(extensions, extensionsLoaded);
    }

    public void checkResponseCode(int result) throws IllegalStateException {
        if (XR_SUCCEEDED(result)) {
            return;
        }

        if (xrInstance != null) {
            ByteBuffer str = stackMalloc(XR_MAX_RESULT_STRING_SIZE);
            if (XR10.xrResultToString(xrInstance, result, str) >= 0) {
                if (result == XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE){
                    throw new OpenXrDeviceNotAvailableException(MemoryUtil.memUTF8(str, memLengthNT1(str)), result);
                }
                throw new OpenXrException(MemoryUtil.memUTF8(str, memLengthNT1(str)),result);
            }
        }else{
            if (result == XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE){
                throw new OpenXrDeviceNotAvailableException("Open XR returned an error code "+result, result);
            }
            throw new OpenXrException("Open XR returned an error code " + result, result);
        }
        throw new RuntimeException("Open XR returned an error code " + result); //I don't think it should ever actually get here

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
            EXTDebugUtils.xrDestroyDebugUtilsMessengerEXT(xrDebugMessenger);
        }
        XR10.xrDestroySession(xrSession);
        XR10.xrDestroyInstance(xrInstance);
    }

    public static class OpenXrException extends RuntimeException {
        @Getter
        private final int errorCode;

        public OpenXrException(String s, int errorCode) {
            super(s);
            this.errorCode = errorCode;
        }
    }

    private record ExtensionsCheckResult(
            PointerBuffer extensionsToLoadBuffer,
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

    private record LayerCheckResult(PointerBuffer wantedLayers, boolean hasCoreValidationLayer){}

    private record Swapchain (
        XrSwapchain handle,
        int width,
        int height,
        XrSwapchainImageOpenGLKHR.Buffer images
    ){}

}

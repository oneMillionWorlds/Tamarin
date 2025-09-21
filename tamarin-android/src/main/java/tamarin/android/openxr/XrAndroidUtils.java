package tamarin.android.openxr;


import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.openxrbindings.XR10;
import com.onemillionworlds.tamarin.openxrbindings.XrApiLayerProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrExtensionProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrQuaternionf;
import com.onemillionworlds.tamarin.openxrbindings.XrVector3f;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrStructureType;
import com.onemillionworlds.tamarin.openxrbindings.memory.MemoryStack;


import java.util.logging.Logger;

import static com.onemillionworlds.tamarin.openxrbindings.memory.MemoryUtil.NULL;


public class XrAndroidUtils {

    public static Quaternion HALF_TURN = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);

    private static final Logger LOGGER = Logger.getLogger(XrAndroidUtils.class.getName());

    /**
     * Allocates an {@link XrExtensionProperties.Buffer} onto the stack with the requested number of extensions
     * and sets the type of each element in the buffer to {@link XrStructureType#XR_TYPE_EXTENSION_PROPERTIES}.
     * <p>
     * Note that the buffer will auto free when the stack does
     *
     * @param stack the stack onto which to allocate the buffer
     * @param numExtensions the number of elements the buffer should get
     *
     * @return the created buffer
     */
    public static XrExtensionProperties.Buffer createExtensionProperties(MemoryStack stack, int numExtensions) {
        XrExtensionProperties.Buffer xrExtensionPropertiesBuffer = XrExtensionProperties.calloc(numExtensions, stack);

        for(int i = 0; i < numExtensions; i++){
            XrExtensionProperties xrExtensionProperties = xrExtensionPropertiesBuffer.get(i);
            xrExtensionProperties.type(XrStructureType.XR_TYPE_EXTENSION_PROPERTIES);
        }
        return xrExtensionPropertiesBuffer;
    }

    /**
     * Allocates an {@link XrApiLayerProperties.Buffer} on the stack with the given number of layers and
     * sets the type of each element in the buffer to {@link XR10#XR_TYPE_API_LAYER_PROPERTIES XR_TYPE_API_LAYER_PROPERTIES}.
     * <p>
     * Note that the buffer will auto free when the stack does
     *
     * @param stack the stack to allocate the buffer on
     * @param numLayers the number of elements the buffer should get
     *
     * @return the created buffer
     */
    public static XrApiLayerProperties.Buffer prepareApiLayerProperties(MemoryStack stack, int numLayers) {

        XrApiLayerProperties.Buffer buffer = XrApiLayerProperties.calloc(numLayers, stack);

        for(int i = 0; i < numLayers; i++){
            XrApiLayerProperties xrExtensionProperties = buffer.get(i);
            xrExtensionProperties.type(XrStructureType.XR_TYPE_API_LAYER_PROPERTIES);
        }
        return buffer;
    }

    /**
     * Appends the right <i>XrGraphicsBinding</i>** struct to the next chain of <i>sessionCreateInfo</i>.
     * Uses the cross platform XrGraphicsBindingEGLMNDX if its available, otherwise uses the platform specific version.
     * <p>
     * There are 4 graphics binding structs available:
     *
     * <ul>
     *     <li> XrGraphicsBindingOpenGLWin32KHR - which can only be used on Windows </li>
     *     <li> XrGraphicsBindingOpenGLXlibKHR - Linux computers with the X11 windowing system </li>
     *     <li> XrGraphicsBindingOpenGLWaylandKHR - theoretically Linux computers with the Wayland windowing system but actually no one </li>
     *     <li> XrGraphicsBindingEGLMNDX - cross-platform, but also experimental and not widely supported, use with Wayland windowing system </li>
     * </ul>
     * @param stack The <i>MemoryStack</i> onto which this method should allocate the graphics binding struct
     * @param window The GLFW window
     * @param useEGL Whether this method should use XrGraphicsBindingEGLMNDX
     * @return sessionCreateInfo (after appending a graphics binding to it)
     * @throws IllegalStateException If the current OS and/or windowing system needs EGL, but <b>useEGL</b> is false
     */
    static Struct<?> createGraphicsBindingOpenGL(MemoryStack stack, long window, boolean useEGL) throws IllegalStateException {
        if (useEGL) {
            long eglDisplay = XR10.glfwGetEGLDisplay();

            if (eglDisplay != NULL){ //check that the egl display is actually available (even if the extension is)
                return XrGraphicsBindingEGLMNDX.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .getProcAddress(EGL.getCapabilities().eglGetProcAddress)
                        .display(eglDisplay)
                        .config(glfwGetEGLConfig(window))
                        .context(glfwGetEGLContext(window));
            }
        }
        switch (Platform.get()) {
            case LINUX:
                int platform = glfwGetPlatform();
                if (platform == GLFW_PLATFORM_X11) {
                    long display   = glfwGetX11Display();
                    long glxConfig = glfwGetGLXFBConfig(window);

                    XVisualInfo visualInfo = glXGetVisualFromFBConfig(display, glxConfig);
                    if (visualInfo == null) {
                        throw new IllegalStateException("Failed to get visual info");
                    }
                    long visualId = visualInfo.visualid();

                    LOGGER.info("Using XrGraphicsBindingOpenGLXlibKHR to create the session");
                    return XrGraphicsBindingOpenGLXlibKHR.malloc(stack)
                                    .type$Default()
                                    .next(NULL)
                                    .xDisplay(display)
                                    .visualid((int)visualId)
                                    .glxFBConfig(glxConfig)
                                    .glxDrawable(glXGetCurrentDrawable())
                                    .glxContext(glfwGetGLXContext(window));
                } else {
                    throw new IllegalStateException(
                            "X11 is the only Linux windowing system with explicit OpenXR support. All other Linux systems must use EGL."
                    );
                }
            case WINDOWS:
                LOGGER.info("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
                return XrGraphicsBindingOpenGLWin32KHR.malloc(stack)
                                .type$Default()
                                .next(NULL)
                                .hDC(GetDC(glfwGetWin32Window(window)))
                                .hGLRC(glfwGetWGLContext(window));
            default:
                throw new IllegalStateException(
                        "Windows and Linux are the only platforms with explicit OpenXR support. All other platforms must use EGL."
                );
        }
    }

    public static Vector3f convertOpenXRToJme(XrVector3f openxrVec) {
        return new Vector3f(openxrVec.x(), openxrVec.y(), openxrVec.z());
    }


    /**
     * JME and OpenXr both use right-handed coordinate systems but there are differences
     * <p>
     * JMonkeyEngine (JME): Uses a right-handed coordinate system where the X-axis points to the right, the Y-axis points up, and the Z-axis points backward (i.e., into the screen).
     * <p>
     * OpenXR: Also uses a right-handed coordinate system but with a different orientation. In the OpenXR coordinate system,
     * the X-axis points to the right, the Y-axis points down, and the Z-axis points forward (i.e., out of the screen).
     * <p>
     * The converts from one to the other
     */
    public static Quaternion convertOpenXRQuaternionToJme(XrQuaternionf openxrQuat) {
        return new Quaternion(openxrQuat.x(), openxrQuat.y(), openxrQuat.z(), openxrQuat.w()).mult(HALF_TURN);
    }

}

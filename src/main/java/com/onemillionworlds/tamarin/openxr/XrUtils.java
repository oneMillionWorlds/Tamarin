package com.onemillionworlds.tamarin.openxr;


import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.lwjgl.egl.EGL;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrApiLayerProperties;
import org.lwjgl.openxr.XrExtensionProperties;
import org.lwjgl.openxr.XrGraphicsBindingEGLMNDX;
import org.lwjgl.openxr.XrGraphicsBindingOpenGLWin32KHR;
import org.lwjgl.openxr.XrGraphicsBindingOpenGLXlibKHR;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.system.linux.XVisualInfo;

import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLConfig;
import static org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLContext;
import static org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLDisplay;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXFBConfig;
import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display;
import static org.lwjgl.opengl.GLX.glXGetCurrentDrawable;
import static org.lwjgl.opengl.GLX13.glXGetVisualFromFBConfig;
import static org.lwjgl.openxr.XR10.XR_TYPE_API_LAYER_PROPERTIES;
import static org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutInt;
import static org.lwjgl.system.windows.User32.GetDC;

public class XrUtils{

    public static Quaternion HALF_TURN = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);

    private static final Logger LOGGER = Logger.getLogger(XrUtils.class.getName());

    /**
     * Allocates an {@link XrExtensionProperties.Buffer} onto the stack with the requested number of extensions
     * and sets the type of each element in the buffer to {@link XR10#XR_TYPE_EXTENSION_PROPERTIES XR_TYPE_EXTENSION_PROPERTIES}.
     * <p>
     * Note that the buffer will auto free when the stack does
     *
     * @param stack the stack onto which to allocate the buffer
     * @param numExtensions the number of elements the buffer should get
     *
     * @return the created buffer
     */
    public static XrExtensionProperties.Buffer createExtensionProperties(MemoryStack stack, int numExtensions) {
        return fill(
                XrExtensionProperties.calloc(numExtensions, stack),
                XrExtensionProperties.TYPE,
                XR_TYPE_EXTENSION_PROPERTIES
        );
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
        return fill(
                XrApiLayerProperties.calloc(numLayers, stack),
                XrApiLayerProperties.TYPE,
                XR_TYPE_API_LAYER_PROPERTIES
        );
    }

    static <S extends Struct, T extends StructBuffer<S, T>> T fill(T buffer, int offset, int value) {
        long ptr    = buffer.address() + offset;
        int  stride = buffer.sizeof();
        for (long i = 0; i < buffer.limit(); i++) {
            memPutInt(ptr + i * stride, value);
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
    static Struct createGraphicsBindingOpenGL(MemoryStack stack, long window, boolean useEGL) throws IllegalStateException {
        if (useEGL) {
            return XrGraphicsBindingEGLMNDX.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .getProcAddress(EGL.getCapabilities().eglGetProcAddress)
                    .display(glfwGetEGLDisplay())
                    .config(glfwGetEGLConfig(window))
                    .context(glfwGetEGLContext(window));
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
                System.out.println("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
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

    /**
     * Creates and returns a Matrix4f that can be used as a projection matrix
     * with the given fov, nearZ, and farZ.
     *
     * @param fov   The desired Field of View for the projection matrix.
     * @param nearZ The nearest Z value that the user should see (also known as the near plane)
     * @param farZ  The furthest Z value that the user should see (also known as far plane)
     * @return A Matrix4f that contains the projection matrix.
     */
    public static Matrix4f createProjectionMatrix(InProgressXrRender.FieldOfViewData fov, float nearZ, float farZ) {
        float tanLeft = FastMath.tan(fov.angleLeft());
        float tanRight = FastMath.tan(fov.angleRight());
        float tanDown = FastMath.tan(fov.angleDown());
        float tanUp = FastMath.tan(fov.angleUp());
        float tanAngleWidth = tanRight - tanLeft;
        float tanAngleHeight = tanUp - tanDown;

        Matrix4f m = new Matrix4f();

        m.m00 = 2.0f / tanAngleWidth;
        m.m01 = 0.0f;
        m.m02 = (tanRight + tanLeft) / tanAngleWidth;
        m.m03 = 0.0f;

        m.m10 = 0.0f;
        m.m11 = 2.0f / tanAngleHeight;
        m.m12 = (tanUp + tanDown) / tanAngleHeight;
        m.m13 = 0.0f;

        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = -(farZ + nearZ) / (farZ - nearZ);
        m.m23 = -(farZ * (nearZ + nearZ)) / (farZ - nearZ);

        m.m30 = 0.0f;
        m.m31 = 0.0f;
        m.m32 = -1.0f;
        m.m33 = 0.0f;

        return m;
    }
}

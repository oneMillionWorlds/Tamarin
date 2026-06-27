package com.onemillionworlds.tamarin.openxr;


import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrApiLayerProperties;
import org.lwjgl.openxr.XrExtensionProperties;
import org.lwjgl.openxr.XrGraphicsBindingOpenGLWin32KHR;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;


import java.util.logging.Logger;

import static org.lwjgl.opengl.WGL.wglGetCurrentContext;
import static org.lwjgl.opengl.WGL.wglGetCurrentDC;
import static org.lwjgl.openxr.XR10.XR_TYPE_API_LAYER_PROPERTIES;
import static org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutInt;


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

    static <S extends Struct<S>, T extends StructBuffer<S, T>> T fill(T buffer, int offset, int value) {
        long ptr    = buffer.address() + offset;
        int  stride = buffer.sizeof();
        for (long i = 0; i < buffer.limit(); i++) {
            memPutInt(ptr + i * stride, value);
        }
        return buffer;
    }

    /**
     * Appends an {@link XrGraphicsBindingOpenGLWin32KHR} struct to the next chain of <i>sessionCreateInfo</i>.
     * <p>
     * Desktop OpenXR runtimes (e.g. SteamVR) support the Win32 OpenGL graphics binding
     * ({@code XR_KHR_opengl_enable}) but not the cross-platform EGL binding ({@code XR_MNDX_egl_enable}).
     * Since jMonkeyEngine 3.10 windowing is now SDL based (no GLFW window handle is available), the
     * WGL context and device context are obtained directly from the current thread via
     * {@link org.lwjgl.opengl.WGL#wglGetCurrentContext()} / {@link org.lwjgl.opengl.WGL#wglGetCurrentDC()}.
     * This requires jMonkeyEngine to run under a real desktop OpenGL renderer (AppSettings.LWJGL_OPENGL45),
     * not the ANGLE/GLES renderer.
     *
     * @param stack The <i>MemoryStack</i> onto which this method should allocate the graphics binding struct
     * @param window The window handle (unused, retained for API compatibility)
     * @return the Win32 OpenGL graphics binding struct
     * @throws IllegalStateException If no current WGL context / device context can be found
     */
    static Struct<?> createGraphicsBindingOpenGL(MemoryStack stack, long window) throws IllegalStateException {
        long hglrc = wglGetCurrentContext(stack.callocInt(1));
        if (hglrc == NULL) {
            throw new IllegalStateException("No current WGL OpenGL context found. jMonkeyEngine must run under a real desktop OpenGL renderer (AppSettings.LWJGL_OPENGL45) before creating the OpenXR session.");
        }

        long hdc = wglGetCurrentDC();
        if (hdc == NULL) {
            throw new IllegalStateException("No current WGL device context (HDC) found. jMonkeyEngine must run under a real desktop OpenGL renderer (AppSettings.LWJGL_OPENGL45) before creating the OpenXR session.");
        }

        LOGGER.info("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
        return XrGraphicsBindingOpenGLWin32KHR.malloc(stack)
                .type$Default()
                .next(NULL)
                .hDC(hdc)
                .hGLRC(hglrc);
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

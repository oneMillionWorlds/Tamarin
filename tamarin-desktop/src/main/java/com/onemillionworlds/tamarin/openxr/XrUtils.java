package com.onemillionworlds.tamarin.openxr;


import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL10;
import org.lwjgl.egl.EGL14;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrApiLayerProperties;
import org.lwjgl.openxr.XrExtensionProperties;
import org.lwjgl.openxr.XrGraphicsBindingEGLMNDX;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;


import java.nio.IntBuffer;
import java.util.logging.Logger;

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
     * Appends an {@link XrGraphicsBindingEGLMNDX} struct to the next chain of <i>sessionCreateInfo</i>.
     * <p>
     * Since jMonkeyEngine moved from GLFW to ANGLE, the OpenGL calls are forwarded through EGL on every
     * platform, so the EGL graphics binding is used unconditionally (ANGLE forwards the calls), removing
     * the need for the platform specific Win32 / Xlib bindings.
     *
     * @param stack The <i>MemoryStack</i> onto which this method should allocate the graphics binding struct
     * @param window The window handle (unused, retained for API compatibility)
     * @return the EGL graphics binding struct
     * @throws IllegalStateException If no current EGL display/context can be found
     */
    static Struct<?> createGraphicsBindingOpenGL(MemoryStack stack, long window) throws IllegalStateException {
        long eglDisplay = EGL10.eglGetCurrentDisplay();
        if (eglDisplay == NULL) {
            throw new IllegalStateException("No current EGL display found. An EGL context (e.g. via ANGLE) must be current before creating the OpenXR session.");
        }

        long eglContext = EGL14.eglGetCurrentContext();
        if (eglContext == NULL) {
            throw new IllegalStateException("No current EGL context found. An EGL context (e.g. via ANGLE) must be current before creating the OpenXR session.");
        }

        IntBuffer cfgIdBuf = stack.callocInt(1);
        EGL10.eglQueryContext(eglDisplay, eglContext, EGL10.EGL_CONFIG_ID, cfgIdBuf);

        int configId = cfgIdBuf.get(0);

        // Now, get the actual EGLConfig handle
        // You need to enumerate configs and match by ID
        IntBuffer numConfigs = stack.callocInt(1);

        EGL10.eglGetConfigs(eglDisplay, null, numConfigs);
        PointerBuffer configs = stack.callocPointer(numConfigs.get(0));
        EGL10.eglGetConfigs(eglDisplay, configs, numConfigs);

        long eglConfig = NULL;

        for (int i = 0; i < numConfigs.get(0); i++) {
            IntBuffer currentConfigIdBuf = stack.callocInt(1);
            EGL10.eglGetConfigAttrib(eglDisplay, configs.get(i), EGL10.EGL_CONFIG_ID, currentConfigIdBuf);
            if (currentConfigIdBuf.get(0) == configId) {
                eglConfig = configs.get(i);
                break;
            }
        }

        if (eglConfig == NULL) {
            throw new IllegalStateException("Failed to find matching EGLConfig");
        }

        LOGGER.info("Using XrGraphicsBindingEGLMNDX to create the session");
        return XrGraphicsBindingEGLMNDX.malloc(stack)
                .type$Default()
                .next(NULL)
                .getProcAddress(EGL.getCapabilities().eglGetProcAddress)
                .display(eglDisplay)
                .config(eglConfig)
                .context(eglContext);
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

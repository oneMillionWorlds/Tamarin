package com.onemillionworlds.tamarin.openxr;


import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL10;
import org.lwjgl.egl.EGL14;
import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.GLX13;
import org.lwjgl.opengl.GLXEXTImportContext;
import org.lwjgl.opengl.WGL;
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

import java.nio.IntBuffer;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GLX.glXGetCurrentContext;
import static org.lwjgl.opengl.GLX.glXGetCurrentDrawable;
import static org.lwjgl.opengl.GLX13.glXGetVisualFromFBConfig;
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
     * Appends the right <i>XrGraphicsBinding</i> struct to the next chain of <i>sessionCreateInfo</i>.
     * Uses the cross-platform XrGraphicsBindingEGLMNDX if available, otherwise uses the platform-specific version.
     * <p>
     * All handle queries use the currently-bound OpenGL context directly (WGL/GLX/EGL APIs), so
     * this works regardless of the windowing backend (GLFW, SDL3, or any other that calls
     * wglMakeCurrent / glXMakeCurrent before this point).
     * <p>
     * There are 4 graphics binding structs available:
     * <ul>
     *     <li> XrGraphicsBindingOpenGLWin32KHR - Windows </li>
     *     <li> XrGraphicsBindingOpenGLXlibKHR - Linux X11 / GLX </li>
     *     <li> XrGraphicsBindingOpenGLWaylandKHR - Linux Wayland (use EGL instead) </li>
     *     <li> XrGraphicsBindingEGLMNDX - cross-platform, experimental </li>
     * </ul>
     * @param stack The <i>MemoryStack</i> onto which this method should allocate the graphics binding struct
     * @param window Unused (retained for API compatibility); handles are queried from the current GL context
     * @param useEGL Whether this method should use XrGraphicsBindingEGLMNDX
     * @return the graphics binding struct
     * @throws IllegalStateException If no usable GL context binding can be determined
     */
    static Struct<?> createGraphicsBindingOpenGL(MemoryStack stack, long window, boolean useEGL) throws IllegalStateException {
        if (useEGL) {
            // Query the current EGL display/context directly — works with any windowing system.
            long eglDisplay = EGL10.eglGetCurrentDisplay();
            if (eglDisplay != NULL) {
                long eglContext = EGL14.eglGetCurrentContext(); // added in EGL 1.1, exposed via EGL14 in LWJGL
                // Retrieve the EGLConfig ID from the current context, then resolve the EGLConfig.
                IntBuffer cfgIdBuf = stack.callocInt(1);
                EGL10.eglQueryContext(eglDisplay, eglContext, EGL10.EGL_CONFIG_ID, cfgIdBuf);
                IntBuffer cfgAttribs = stack.ints(EGL10.EGL_CONFIG_ID, cfgIdBuf.get(0), EGL10.EGL_NONE);
                PointerBuffer cfgBuf = stack.mallocPointer(1);
                IntBuffer numCfg = stack.callocInt(1);
                EGL10.eglChooseConfig(eglDisplay, cfgAttribs, cfgBuf, numCfg);
                long eglConfig = (numCfg.get(0) > 0) ? cfgBuf.get(0) : NULL;
                return XrGraphicsBindingEGLMNDX.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .getProcAddress(EGL.getCapabilities().eglGetProcAddress)
                        .display(eglDisplay)
                        .config(eglConfig)
                        .context(eglContext);
            }
        }
        switch (Platform.get()) {
            case LINUX: {
                // Use GLX_EXT_import_context to get the current X11 Display, then query the
                // current context for screen + FBConfig — no GLFW required.
                long glxCtx = glXGetCurrentContext();
                long display = GLXEXTImportContext.glXGetCurrentDisplayEXT();
                if (glxCtx == NULL || display == NULL) {
                    throw new IllegalStateException(
                            "X11/GLX is the only Linux windowing system with explicit OpenXR support (no current " +
                            "GLX context or GLX_EXT_import_context display found). " +
                            "For Wayland / EGL, set useEGL=true in XrSettings.");
                }

                // Query the screen and FBConfig ID from the current GLX context.
                IntBuffer val = stack.callocInt(1);
                GLX13.glXQueryContext(display, glxCtx, GLX13.GLX_SCREEN, val);
                int screen = val.get(0);
                val.clear();
                GLX13.glXQueryContext(display, glxCtx, GLX13.GLX_FBCONFIG_ID, val);
                int fbConfigId = val.get(0);

                // glXChooseFBConfig returns a PointerBuffer sized to the matching configs.
                IntBuffer fbAttribs = stack.ints(GLX13.GLX_FBCONFIG_ID, fbConfigId, 0);
                PointerBuffer cfgs = GLX13.glXChooseFBConfig(display, screen, fbAttribs);
                if (cfgs == null || cfgs.limit() == 0) {
                    throw new IllegalStateException("Failed to find GLXFBConfig for the current context");
                }
                long glxConfig = cfgs.get(0);

                XVisualInfo visualInfo = glXGetVisualFromFBConfig(display, glxConfig);
                if (visualInfo == null) {
                    throw new IllegalStateException("Failed to get XVisualInfo from GLXFBConfig");
                }

                LOGGER.info("Using XrGraphicsBindingOpenGLXlibKHR to create the session");
                return XrGraphicsBindingOpenGLXlibKHR.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .xDisplay(display)
                        .visualid((int) visualInfo.visualid())
                        .glxFBConfig(glxConfig)
                        .glxDrawable(glXGetCurrentDrawable())
                        .glxContext(glxCtx);
            }
            case WINDOWS:
                // Query hDC and hGLRC directly from the current WGL context — works with any
                // windowing backend (GLFW, SDL3, etc.) since jME already called wglMakeCurrent.
                LOGGER.info("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
                return XrGraphicsBindingOpenGLWin32KHR.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .hDC(WGL.wglGetCurrentDC())
                        .hGLRC(WGL.wglGetCurrentContext(stack.callocInt(1)));
            default:
                throw new IllegalStateException(
                        "Windows and Linux are the only platforms with explicit OpenXR support. All other platforms must use EGL.");
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

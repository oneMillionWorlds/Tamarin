package tamarin.android.openxr;


import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.openxrbindings.XR10;
import com.onemillionworlds.tamarin.openxrbindings.XrApiLayerProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrExtensionProperties;
import com.onemillionworlds.tamarin.openxrbindings.XrGraphicsBindingOpenGLESAndroidKHR;
import com.onemillionworlds.tamarin.openxrbindings.XrQuaternionf;
import com.onemillionworlds.tamarin.openxrbindings.XrVector3f;
import com.onemillionworlds.tamarin.openxrbindings.enums.XrStructureType;
import com.onemillionworlds.tamarin.openxrbindings.handles.EGLConfig;
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
     * sets the type of each element in the buffer to XR_TYPE_API_LAYER_PROPERTIES.
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


    static XrGraphicsBindingOpenGLESAndroidKHR createGraphicsBindingOpenGL(MemoryStack stack, long window, boolean useEGL) throws IllegalStateException {
        XrGraphicsBindingOpenGLESAndroidKHR graphicsBindingOpenGLESAndroidKHR = XrGraphicsBindingOpenGLESAndroidKHR.calloc(stack);
        graphicsBindingOpenGLESAndroidKHR.type(XrStructureType.XR_TYPE_GRAPHICS_BINDING_OPENGL_ES_ANDROID_KHR);
        graphicsBindingOpenGLESAndroidKHR.next(NULL);

        // Get the current EGL display, config, and context
        EGLDisplay eglDisplay = EGL14.eglGetCurrentDisplay();
        EGLContext eglContext = EGL14.eglGetCurrentContext();

        graphicsBindingOpenGLESAndroidKHR.display(new com.onemillionworlds.tamarin.openxrbindings.handles.EGLDisplay(eglDisplay.getNativeHandle()));
        graphicsBindingOpenGLESAndroidKHR.context(new com.onemillionworlds.tamarin.openxrbindings.handles.EGLContext(eglContext.getNativeHandle()));

        // Get the EGL config
        int[] configAttribs = {
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        };
        android.opengl.EGLConfig[] configs = new android.opengl.EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0);

        if (numConfigs[0] <= 0 || configs[0] == null) {
            LOGGER.warning("Failed to get EGL config");
        } else {
            graphicsBindingOpenGLESAndroidKHR.config(new com.onemillionworlds.tamarin.openxrbindings.handles.EGLConfig(configs[0].getNativeHandle()));
        }

        return graphicsBindingOpenGLESAndroidKHR;
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

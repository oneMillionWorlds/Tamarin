package tamarin.android.openxr;

import android.app.Activity;
import android.content.Context;
import android.opengl.EGL14;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.openxr.XrVrAppState;
import com.onemillionworlds.tamarin.openxr.XrVrMode;
import com.onemillionworlds.tamarin.openxrbindings.thickc.InitialisationData;
import com.onemillionworlds.tamarin.openxrbindings.thickc.ThickC;

import java.util.Map;

public class XrAndroidAppState extends XrVrAppState {

    OpenXrAndroidSessionManager xrSession;
    Activity androidActivity;

    @SuppressWarnings("unused")
    public XrAndroidAppState(Activity androidActivity){
        this(androidActivity, new XrSettings());
    }
    public XrAndroidAppState(Activity androidActivity, XrSettings xrSettings){
        super(xrSettings);
        this.androidActivity = androidActivity;
        xrSettings.addRequiredXrExtension("XR_KHR_opengl_es_enable"); //openGL support see KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
    }

    @Override
    protected void initialize(Application app){
        super.initialize(app);
        Context context = androidActivity.getApplicationContext();
        //InitialisationData initialisationData = ThickC.initializeLoader(context);
        InitialisationData initialisationData = ThickC.initializeLoader(androidActivity);
        // Get the current EGL context, which is already set up by jMonkeyEngine
        long windowHandle = EGL14.eglGetCurrentContext().getNativeHandle();

        AppSettings settings = app.getContext().getSettings();

        xrSession = OpenXrAndroidSessionManager.createOpenXrSession(windowHandle, xrSettings, settings, app.getRenderer(), initialisationData);
        xrSession.setXrVrBlendMode(xrSettings.getInitialXrVrMode());
        int width = xrSession.getSwapchainWidth();
        int height = xrSession.getSwapchainHeight();

        initialiseCameras(width, height);
    }

    public OpenXrAndroidSessionManager getXrSession(){
        return xrSession;
    }


    @Override
    public String getSystemName(){
        return xrSession.getSystemName();
    }

    @Override
    protected void cleanup(Application app){
        super.cleanup(app);
        xrSession.destroy();
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        inProgressXrRender = xrSession.startXrFrame();
        if (inProgressXrRender.shouldRender){
            render();
        }
    }

    @Override
    public Map<String, Boolean> getExtensionsLoaded(){
        return xrSession.getExtensionsLoaded();
    }



    @Override
    public void postRender(){
        super.postRender();
        if (inProgressXrRender !=null){
            xrSession.presentFrameBuffersToOpenXr(inProgressXrRender);
            inProgressXrRender = null;
        }
    }


    @Override
    public void setXrVrMode(XrVrMode xrVrMode){
        xrSession.setXrVrBlendMode(xrVrMode);
    }

}

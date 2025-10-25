package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;

import java.util.Map;

public class XrAppState extends XrVrAppState{

    OpenXrSessionManager xrSession;


    @SuppressWarnings("unused")
    public XrAppState(){
        this(new XrSettings());
    }
    public XrAppState(XrSettings xrSettings){
        super(xrSettings);
        xrSettings.addRequiredXrExtension("XR_KHR_opengl_enable"); //openGL support see KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
    }

    @Override
    protected void initialize(Application app){
        super.initialize(app);
        long windowHandle;
        if (app.getContext() instanceof LwjglWindow lwjglWindow) {
            windowHandle = lwjglWindow.getWindowHandle();
        }else{
            //maybe something like this on android? (and then using the XrGraphicsBindingEGLMNDX binding)
            //EGL14.eglGetCurrentContext()
            throw new RuntimeException("Only LwjglWindow is supported (need to get the window handle)");
        }
        AppSettings settings = app.getContext().getSettings();

        xrSession = OpenXrSessionManager.createOpenXrSession(windowHandle, xrSettings, settings, app.getRenderer());
        xrSession.setXrVrBlendMode(xrSettings.getInitialXrVrMode());
        int width = xrSession.getSwapchainWidth();
        int height = xrSession.getSwapchainHeight();

        initialiseCameras(width, height);
    }

    public OpenXrSessionManager getXrSession(){
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

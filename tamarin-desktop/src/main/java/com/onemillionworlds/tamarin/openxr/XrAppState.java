package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.Application;
import com.jme3.app.LostFocusBehavior;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;

import java.util.Map;
import java.util.logging.Logger;

public class XrAppState extends XrVrAppState{

    private static final Logger LOGGER = Logger.getLogger(XrAppState.class.getName());

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
        if (app.getContext() instanceof LwjglWindow) {
            LwjglWindow lwjglWindow = (LwjglWindow) app.getContext();
            windowHandle = lwjglWindow.getWindowHandle();
        }else{
            //maybe something like this on android? (and then using the XrGraphicsBindingEGLMNDX binding)
            //EGL14.eglGetCurrentContext()
            throw new RuntimeException("Only LwjglWindow is supported (need to get the window handle)");
        }
        AppSettings settings = app.getContext().getSettings();

        if (xrSettings.isOverrideLostFocusBehaviour()){
            overrideLostFocusBehaviour(app);
        }

        xrSession = OpenXrSessionManager.createOpenXrSession(windowHandle, xrSettings, settings, app.getRenderer(), sessionObservables);
        xrSession.setXrVrBlendMode(xrSettings.getInitialXrVrMode());
        int width = xrSession.getSwapchainWidth();
        int height = xrSession.getSwapchainHeight();

        initialiseCameras(width, height);
    }

    /**
     * JME's lost focus behaviour refers to the desktop window, which in VR is just a mirror and is often unfocused.
     * The default behaviour (ThrottleOnLostFocus) caps the whole main loop (and so the headset) at 20 fps and
     * PauseOnLostFocus would stop the OpenXR frame loop entirely (which the headset would see as a frozen application).
     * So in VR the window focus is ignored, applications should use {@link #subscribeToSessionFocused()} instead.
     */
    private static void overrideLostFocusBehaviour(Application app){
        LostFocusBehavior originalBehaviour = app.getLostFocusBehavior();
        if (originalBehaviour == LostFocusBehavior.Disabled){
            return;
        }
        String message = "Changing the JME LostFocusBehavior from " + originalBehaviour + " to Disabled. In VR the desktop window " +
                "losing focus should not throttle or pause the application. Use XrBaseAppState#subscribeToSessionFocused() " +
                "to react to the headset losing focus instead (or XrSettings#setOverrideLostFocusBehaviour(false) to keep JME's behaviour)";
        if (originalBehaviour == LostFocusBehavior.PauseOnLostFocus){
            LOGGER.warning(message);
        }else{
            LOGGER.info(message);
        }
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        // the window may already have lost focus (and so be throttled), with the behaviour now disabled regaining
        // focus would no longer undo that, so undo it now
        app.getContext().setAutoFlushFrames(true);
    }

    public OpenXrSessionManager getXrSession(){
        return xrSession;
    }

    @Override
    protected PassthroughControl getPassthroughControl(){
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

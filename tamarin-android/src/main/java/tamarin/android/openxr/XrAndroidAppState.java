package tamarin.android.openxr;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.openxr.XrSettings;
import com.onemillionworlds.tamarin.openxr.XrVrAppState;
import com.onemillionworlds.tamarin.openxr.XrVrMode;

import java.util.Map;

public class XrAndroidAppState extends XrVrAppState {

    OpenXrAndroidSessionManager xrSession;


    @SuppressWarnings("unused")
    public XrAndroidAppState(){
        this(new XrSettings());
    }
    public XrAndroidAppState(XrSettings xrSettings){
        super(xrSettings);
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

        xrSession = OpenXrAndroidSessionManager.createOpenXrSession(windowHandle, xrSettings, settings, app.getRenderer());
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

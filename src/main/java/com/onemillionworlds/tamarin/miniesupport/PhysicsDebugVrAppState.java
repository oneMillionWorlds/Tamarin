package com.onemillionworlds.tamarin.miniesupport;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.bullet.debug.DebugConfiguration;
import com.jme3.renderer.ViewPort;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;

import java.util.ArrayList;
import java.util.List;

public class PhysicsDebugVrAppState extends BaseAppState{
    private static final int FRAMES_TO_COLLECT_VIEWPORTS_FOR = 10;

    public static final String ID = "PhysicsVrAppState";

    public PhysicsDebugVrAppState(){
        super(ID);
        MinieUtils.ensureMinieIsAvailable();
    }

    private BulletAppState bulletAppState;
    private BulletDebugAppState bulletDebugAppState;

    List<ViewPort> debugOverlayViewports = new ArrayList<>();

    int initialisationCounter = 0;
    ViewportConfigurator viewportConfigurator;

    @Override
    protected void initialize(Application app){
        bulletAppState = getState(BulletAppState.class);
        XrBaseAppState xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);

        viewportConfigurator = xrAppState.addAdditionalViewport(AdditionalViewportRequest
                .builder(null) // the BulletDebugAppState will attach the scenes to the viewport, null tells tamarin not to
                .setConfigureViewport(vp -> {
                    /*
                     * A bit nasty, tamarin will only create view ports on demand.
                     * We collect them in the first 10 frames, then once we have them all
                     * we initialise a BulletDebugAppState using them
                     */
                    debugOverlayViewports.add(vp);
                })
                .build()
        );
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        initialisationCounter++;
        if(initialisationCounter==FRAMES_TO_COLLECT_VIEWPORTS_FOR){
            DebugConfiguration debugConfig = new DebugConfiguration();
            debugConfig.setSpace(bulletAppState.getPhysicsSpace());
            debugConfig.setViewPorts(debugOverlayViewports.toArray(new ViewPort[0]));
            debugConfig.initialize(getApplication());
            bulletDebugAppState = new BulletDebugAppState(debugConfig);
            getStateManager().attach(bulletDebugAppState);
        }
    }

    @Override
    protected void cleanup(Application app){
        if(bulletDebugAppState!=null){
            getStateManager().detach(bulletDebugAppState);
        }
        viewportConfigurator.removeViewports();
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}
}

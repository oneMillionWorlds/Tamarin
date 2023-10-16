package com.onemillionworlds.tamarin.audio;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.Listener;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.onemillionworlds.tamarin.openxr.XrAppState;

public class VrAudioListenerState extends BaseAppState {

    private Listener listener;
    private XrAppState xrAppState;
    private float lastTpf;

    public VrAudioListenerState() {
    }

    @Override
    protected void initialize(Application app) {
        this.listener = app.getListener();
        this.xrAppState = app.getStateManager().getState(XrAppState.ID, XrAppState.class);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    public void update(float tpf) {
        lastTpf = tpf;
    }

    @Override
    public void render(RenderManager rm) {
        if (!isEnabled() || listener == null) {
            return;
        }

        Vector3f lastLocation = listener.getLocation();
        Vector3f currentLocation = xrAppState.getVrCameraPosition();
        Vector3f velocity = listener.getVelocity();

        if (!lastLocation.equals(currentLocation)) {
            velocity.set(currentLocation).subtractLocal(lastLocation);
            velocity.multLocal(1f / lastTpf);
            listener.setLocation(currentLocation);
            listener.setVelocity(velocity);
        } else if (!velocity.equals(Vector3f.ZERO)) {
            listener.setVelocity(Vector3f.ZERO);
        }

        Quaternion lastRotation = listener.getRotation();
        Quaternion currentRotation = xrAppState.getLeftCamera().getRotation();
        if (!lastRotation.equals(currentRotation)) {
            listener.setRotation(currentRotation);
        }
    }

    @Override protected void onEnable() {}

    @Override protected void onDisable() {}
}

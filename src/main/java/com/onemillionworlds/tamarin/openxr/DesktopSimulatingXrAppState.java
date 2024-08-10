package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class DesktopSimulatingXrAppState extends XrBaseAppState{

    private final Queue<Runnable> runOnceInitialised = new LinkedList<>();

    private float nearClip = 0.05f;

    private float farClip = 500;

    private boolean refreshProjectionMatrix = true;

    /**
     * In this simulated mode the most important thing the observer does is define the height of the
     * floor.
     */
    @Getter
    Node observer = new Node("Xr Observer");

    @Override
    public void setMainViewportConfiguration(Consumer<ViewPort> configureViewport){
        // not relevant for desktop simulation
    }

    @Override
    public ViewportConfigurator addAdditionalViewport(AdditionalViewportRequest additionalViewportRequest){
        return new ViewportConfigurator(){
            @Override
            public void updateViewportConfiguration(Consumer<ViewPort> configureViewport){
                // not relevant for desktop simulation
            }

            @Override
            public void removeViewports(){
                // not relevant for desktop simulation
            }
        };
    }

    @Override
    public void runAfterInitialisation(Runnable runnable){
        runOnceInitialised.add(runnable);
    }

    @Override
    public String getSystemName(){
        return "Desktop Simulating OpenXR";
    }

    @Override
    public boolean checkExtensionLoaded(String extensionName){
        return false;
    }

    @Override
    public void setNearClip(float nearClip){
        this.nearClip = nearClip;
        this.refreshProjectionMatrix = true;
    }

    @Override
    public void setFarClip(float farClip){
        this.farClip = farClip;
        this.refreshProjectionMatrix = true;
    }

    @Override
    public void setObserverPosition(Vector3f observerPosition){
        observer.setLocalTranslation(observerPosition);
    }

    @Override
    public Vector3f getObserverPosition(){
        return observer.getLocalTranslation();
    }

    @Override
    public void movePlayersFaceToPosition(Vector3f facePosition){
        //this runs defered to be the same as the VR version
        runOnceInitialised.add(() -> {
            Vector3f playerCurrentPosition = getVrCameraPosition();
            Vector3f movement = facePosition.subtract(playerCurrentPosition);

            Vector3f currentObserverPosition = getObserverPosition();

            setObserverPosition(new Vector3f(currentObserverPosition.x + movement.x, currentObserverPosition.y, currentObserverPosition.z + movement.z));

            //because this is simulated (so no VR headset to respond to the change) we need to update the camera position
            getApplication().getCamera().setLocation(facePosition);
        });
    }

    @Override
    public void movePlayersFeetToPosition(Vector3f feetPosition){
        //this runs defered to be the same as the VR version
        runOnceInitialised.add(() -> {
            Vector3f playerCurrentPosition = getVrCameraPosition();
            Node observerNode = getObserver();
            Vector3f movement = feetPosition.subtract(playerCurrentPosition);

            Vector3f currentObserverPosition = observerNode.getWorldTranslation();

            observerNode.setLocalTranslation(currentObserverPosition.x+movement.x, feetPosition.y, currentObserverPosition.z+movement.z);

            //because this is simulated (so no VR headset to respond to the change) we need to update the camera position
            float headHeightAboveFeet = playerCurrentPosition.y - feetPosition.y;
            getApplication().getCamera().setLocation(feetPosition.add(0, headHeightAboveFeet, 0));
        });

    }

    @Override
    public void setObserverRotation(Quaternion observerRotation){
        //nothing good can come of this is simulated mode. Ignore
    }

    @Override
    public void rotateObserverWithoutMovingPlayer(float angleAboutYAxis){
        Camera camera = getApplication().getCamera();
        camera.setRotation(new Quaternion().fromAngleAxis(angleAboutYAxis, Vector3f.UNIT_Y).mult(camera.getRotation()));
    }

    @Override
    public void playerLookInDirection(Vector3f lookDirection){
        //this ignores up/down. Only rotates in the X-Z plane
        Camera camera = getApplication().getCamera();
        Vector3f currentLookDirection = new Vector3f(camera.getDirection());
        currentLookDirection.y = 0;
        Vector3f requestedLookDirection = new Vector3f(lookDirection);
        requestedLookDirection.y = 0;

        if (currentLookDirection.lengthSquared()>0 && requestedLookDirection.lengthSquared()>0){
            float currentAngle = currentLookDirection.angleBetween(Vector3f.UNIT_Z);
            float requestedAngle = requestedLookDirection.angleBetween(Vector3f.UNIT_Z);
            float changeInAngle = requestedAngle - currentAngle;
            rotateObserverWithoutMovingPlayer(changeInAngle);
        }
    }

    @Override
    public void playerLookAtPosition(Vector3f position){
        //this runs defered to be the same as the VR version
        runOnceInitialised.add(() -> {
            Vector3f currentPosition = getVrCameraPosition();

            Vector3f desiredLookDirection = position.subtract(currentPosition);
            desiredLookDirection.y = 0;
            if(desiredLookDirection.lengthSquared() > 0){
                playerLookInDirection(desiredLookDirection.normalizeLocal());
            }
        });
    }

    @Override
    public Vector3f getVrCameraLookDirection(){
        return getApplication().getCamera().getDirection();
    }

    @Override
    public Vector3f getVrCameraPosition(){
        return getApplication().getCamera().getLocation();
    }

    @Override
    public Vector3f getPlayerFeetPosition(){
        Vector3f feetPosition = new Vector3f(getApplication().getCamera().getLocation());
        feetPosition.y = observer.getLocalTranslation().y;
        return feetPosition;
    }

    @Override
    protected void initialize(Application application){
        ((SimpleApplication)getApplication()).getRootNode().attachChild(observer);
    }

    @Override
    protected void cleanup(Application application){
        observer.removeFromParent();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        if (refreshProjectionMatrix){
            Camera camera = getApplication().getCamera();
            camera.setFrustumPerspective(45f, (float) camera.getWidth() / camera.getHeight(), nearClip, farClip);
            refreshProjectionMatrix = false;
        }
        while (!runOnceInitialised.isEmpty()) {
            runOnceInitialised.poll().run();
        }
    }

    @Override
    public Quaternion getVrCameraRotation(){
        return getApplication().getCamera().getRotation();
    }
}

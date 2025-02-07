package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioListenerState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.FrameBuffer;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.audio.VrAudioListenerState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportData;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class XrAppState extends XrBaseAppState{
    private static final Logger LOGGER = Logger.getLogger(XrAppState.class.getName());

    public static String ID = XrBaseAppState.ID;

    @Getter
    OpenXrSessionManager xrSession;
    @Getter
    Camera leftCamera;
    @Getter
    Camera rightCamera;

    /**
     * Will contain 6 viewports, 3 for each eye (left and right) because it is triple buffered. The reason we use multiple
     * viewports per eye (rather than changing the output frame buffer for a single viewport) is that the viewport may have
     * had scene processors added to it that want to insert themselves into the rendering pipeline; they do this
     * by setting the viewports output framebuffer to their own input framebuffer and use the viewports current output framebuffer
     * as the processors output. In other words the scene processors mess with the output frame buffer and expect no one
     * else to mess with it. If we were to change the output framebuffer we get flickering if a scene processor is added.
     */
    Map<FrameBuffer, ViewPort> viewPorts = new HashMap<>(6);

    /**
     * These are extra viewports that are used to render overlays (e.g. debug shapes).
     */
    List<AdditionalViewportData> additionalViewports = new ArrayList<>(1);

    /**
     *
     * The observer's position in the virtual world maps to the VR origin in the real world.
     */
    @Getter
    Node observer = new Node("Xr Observer");

    InProgressXrRender inProgressXrRender;

    private boolean refreshProjectionMatrix = true;

    //these are kept so we update the cameras only when they change
    InProgressXrRender.FieldOfViewData leftFovLastRendered = null;
    InProgressXrRender.FieldOfViewData rightFovLastRendered = null;

    private float nearClip = 0.05f;

    private float farClip = 500;

    private final XrSettings xrSettings;

    private final Queue<Runnable> runOnceHaveCameraPositions = new LinkedList<>();

    private Consumer<ViewPort> newViewportConfiguration = viewPort -> {};

    @SuppressWarnings("unused")
    public XrAppState(){
        this(new XrSettings());
    }
    public XrAppState(XrSettings xrSettings){
        super();
        this.xrSettings = xrSettings;
    }

    @Override
    protected void initialize(Application app){
        long windowHandle;
        if (app.getContext() instanceof LwjglWindow lwjglWindow) {
            windowHandle = lwjglWindow.getWindowHandle();
        }else{
            //maybe something like this on android? (and then using the XrGraphicsBindingEGLMNDX binding)
            //EGL14.eglGetCurrentContext()
            throw new RuntimeException("Only LwjglWindow is supported (need to get the window handle)");
        }
        AppSettings settings = app.getContext().getSettings();
        if (xrSettings.getApplicationName().isEmpty()){
            xrSettings.setApplicationName(settings.getTitle());
        }
        if (xrSettings.getSamples() == -1){
            xrSettings.setSamples(settings.getSamples());
        }
        if(xrSettings.getDrawMode() == DrawMode.AUTOSELECT){
            xrSettings.setDrawMode(xrSettings.getSamples() > 1 ? DrawMode.BLITTED : DrawMode.DIRECT);
        }
        if (xrSettings.getSamples()>1 && xrSettings.getDrawMode() == DrawMode.DIRECT){
            throw new RuntimeException("MSAA is not supported in DIRECT draw mode, change to COPIED");
        }

        if (settings.isVSync()){
            LOGGER.warning("VSync is enabled. This will cause stuttering in VR. Please disable it. Frame rate should be controlled by the headset, not the monitor");
        }

        xrSession = OpenXrSessionManager.createOpenXrSession(windowHandle, xrSettings, settings, app.getRenderer());
        xrSession.setXrVrBlendMode(xrSettings.getInitialXrVrMode());
        int width = xrSession.getSwapchainWidth();
        int height = xrSession.getSwapchainHeight();

        leftCamera = new Camera(width, height);
        rightCamera = new Camera(width, height);

        leftCamera.setParallelProjection(false);
        rightCamera.setParallelProjection(false);

        Quaternion rotation = new Quaternion();
        rotation.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        leftCamera.setRotation(rotation);
        rightCamera.setRotation(rotation);

        ((SimpleApplication) getApplication()).getRootNode().attachChild(observer);

        if (xrSettings.isMainCameraFollowsVrCamera()){
            FlyCamAppState flyCam = getStateManager().getState(FlyCamAppState.class);
            if (flyCam!=null){
                getStateManager().detach(flyCam);
            }
        }

        if (xrSettings.isEarsFollowVrCamera()){
            AudioListenerState audioListenerState = getStateManager().getState(AudioListenerState.class);
            if (audioListenerState!=null){
                getStateManager().detach(audioListenerState);
            }
            if (getStateManager().getState(VrAudioListenerState.class) == null){
                getStateManager().attach(new VrAudioListenerState());
            }
        }
    }

    private ViewPort newViewPort(EyeSide eyeSide){
        ViewPort newViewport = getApplication().getRenderManager().createMainView(  eyeSide + " Eye", eyeSide == EyeSide.LEFT ? leftCamera : rightCamera);
        newViewport.setClearFlags(true, true, true);
        newViewport.attachScene(((SimpleApplication) getApplication()).getRootNode());
        this.newViewportConfiguration.accept(newViewport);
        return newViewport;
    }

    @Override
    public void setMainViewportConfiguration(Consumer<ViewPort> configureViewport){
        viewPorts.values().forEach(configureViewport);
        this.newViewportConfiguration = configureViewport;
    }


    @Override
    public ViewportConfigurator addAdditionalViewport(AdditionalViewportRequest additionalViewportRequest){
        AdditionalViewportData additionalViewportData = new AdditionalViewportData(additionalViewportRequest, getApplication().getRenderManager(), leftCamera, rightCamera);
        this.additionalViewports.add(additionalViewportData);

        return new ViewportConfigurator(){
            @Override
            public void updateViewportConfiguration(Consumer<ViewPort> configureViewport){
                additionalViewportData.updateConfigureViewport(configureViewport);
            }

            @Override
            public void removeViewports(){
                additionalViewportData.cleanup();
                additionalViewports.remove(additionalViewportData);
            }
        };
    }


    @Override
    public void runAfterInitialisation(Runnable runnable){
        runOnceHaveCameraPositions.add(runnable);
    }

    @Override
    public String getSystemName(){
        return xrSession.getSystemName();
    }

    @Override
    protected void cleanup(Application app){
        LOGGER.info("Cleaning up OpenXR for shutdown");
        xrSession.destroy();
        viewPorts.values().forEach(app.getRenderManager()::removePreView);
        this.additionalViewports.forEach(AdditionalViewportData::cleanup);
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}



    @Override
    public void update(float tpf){
        super.update(tpf);
        inProgressXrRender = xrSession.startXrFrame();
        if (inProgressXrRender.shouldRender){
            updateEyePositions(inProgressXrRender);

            while (!runOnceHaveCameraPositions.isEmpty()) {
                runOnceHaveCameraPositions.poll().run();
                updateEyePositions(inProgressXrRender);
            }
            viewPorts.values().forEach(vp -> vp.setEnabled(false));

            //must set every frame due to OpenXR buffering to multiple images
            ViewPort leftViewPort = viewPorts.computeIfAbsent(inProgressXrRender.getLeftBufferToRenderTo(), (fb) -> {
                ViewPort viewPort = newViewPort(EyeSide.LEFT);
                viewPort.setOutputFrameBuffer(fb);
                return viewPort;
            });
            leftViewPort.setEnabled(true);

            ViewPort rightViewPort = viewPorts.computeIfAbsent(inProgressXrRender.getRightBufferToRenderTo(), (fb) -> {
                ViewPort viewPort = newViewPort(EyeSide.RIGHT);
                viewPort.setOutputFrameBuffer(fb);
                return viewPort;
            });
            rightViewPort.setEnabled(true);

            for(AdditionalViewportData additionalViewportData : additionalViewports){
                additionalViewportData.setActiveViewports(inProgressXrRender.getLeftBufferToRenderTo(), inProgressXrRender.getRightBufferToRenderTo());
            }

            if (refreshProjectionMatrix || !inProgressXrRender.leftEye.fieldOfView().equals(leftFovLastRendered) || !inProgressXrRender.rightEye.fieldOfView().equals(rightFovLastRendered)){
                leftCamera.setProjectionMatrix(inProgressXrRender.leftEye.calculateProjectionMatrix(nearClip, farClip));
                setCameraFrustum(leftCamera, inProgressXrRender.getLeftEye().fieldOfView(), nearClip, farClip);
                rightCamera.setProjectionMatrix(inProgressXrRender.rightEye.calculateProjectionMatrix(nearClip, farClip));
                setCameraFrustum(rightCamera, inProgressXrRender.getRightEye().fieldOfView(), nearClip, farClip);
                refreshProjectionMatrix = false;
            }

            if (xrSettings.isMainCameraFollowsVrCamera()){
                getApplication().getCamera().setLocation(getVrCameraPosition());
                getApplication().getCamera().setRotation(getLeftCamera().getRotation());
            }
        }
    }

    public Map<String, Boolean> getExtensionsLoaded(){
        return xrSession.getExtensionsLoaded();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean checkExtensionLoaded(String extensionName){
        //the below converts nulls to false
        return getExtensionsLoaded().get(extensionName) == Boolean.TRUE;
    }

    private void updateEyePositions(InProgressXrRender inProgressXrRender){
        leftCamera.setLocation(observer.localToWorld(inProgressXrRender.leftEye.eyePosition(), null));
        rightCamera.setLocation(observer.localToWorld(inProgressXrRender.rightEye.eyePosition(), null));
        leftCamera.setRotation(observer.getWorldRotation().mult(inProgressXrRender.leftEye.eyeRotation()));
        rightCamera.setRotation(observer.getWorldRotation().mult(inProgressXrRender.rightEye.eyeRotation()));
    }

    private static void setCameraFrustum(Camera camera, InProgressXrRender.FieldOfViewData fieldOfViewData, float nearClip, float farClip) {
        float left = nearClip * FastMath.tan(fieldOfViewData.angleLeft());
        float right = nearClip * FastMath.tan(fieldOfViewData.angleRight());
        float top = nearClip * FastMath.tan(fieldOfViewData.angleUp());
        float bottom = nearClip * FastMath.tan(fieldOfViewData.angleDown());

        camera.setFrustum(nearClip, farClip, left, right, top, bottom);
    }

    @SuppressWarnings("unused")
    @Override
    public void setNearClip(float nearClip){
        this.nearClip = nearClip;
        this.refreshProjectionMatrix = true;
    }

    /**
     * Sets the far clip plane for the cameras (will trigger a refresh of the projection matrix).
     * <p>
     * Note that the field of view cannot be changed by the user because it is set by OpenXr to reflect the devices
     * lens arrangement.
     */
    @SuppressWarnings("unused")
    @Override
    public void setFarClip(float farClip){
        this.farClip = farClip;
        this.refreshProjectionMatrix = true;
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
    public void setObserverPosition(Vector3f observerPosition){
        observer.setLocalTranslation(observerPosition);
    }

    /**
     * Gets the observer position. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. This is a highly technical method you
     * probably don't want to use, if you want to move the player directly (for example to support a teleport-style movement)
     * use {@link XrAppState#movePlayersFeetToPosition(Vector3f)}.
     *
     * @return  observerPosition observer position
     */
    @Override
    @SuppressWarnings("unused")
    public Vector3f getObserverPosition(){
        return observer.getLocalTranslation();
    }

    /**
     * Moves the players face to the requested position. This is useful for teleportation style movement.
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param facePosition the facePosition.
     */
    @SuppressWarnings("unused")
    @Override
    public void movePlayersFaceToPosition(Vector3f facePosition){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.movePlayerFaceToPosition(this, facePosition));
    }

    @Override
    public void movePlayersFeetToPosition(Vector3f feetPosition){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.movePlayerFeetToPosition(this, feetPosition));
    }

    @Override
    public void setObserverRotation(Quaternion observerRotation){
        getObserver().setLocalRotation(observerRotation);
    }

    @Override
    public void rotateObserverWithoutMovingPlayer(float angleAboutYAxis){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.rotateObserverWithoutMovingPlayer(this, angleAboutYAxis));
    }

    /**
     * This will rotate the observer such that the player is looking in the requested direction. Only considered rotation
     * in the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     */
    @SuppressWarnings("unused")
    public void playerLookInDirection(Vector3f lookDirection){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.playerLookInDirection(this, lookDirection));
    }

    @Override
    public void playerLookAtPosition(Vector3f position){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.playerLookAtPosition(this, position));
    }

    @Override
    public Vector3f getVrCameraLookDirection(){
        return getLeftCamera().getDirection();
    }

    @Override
    public Vector3f getVrCameraPosition(){
        return getLeftCamera().getLocation().add(getRightCamera().getLocation()).mult(0.5f);
    }

    @Override
    public Vector3f getPlayerFeetPosition(){
        return getVrCameraPosition().clone().setY(getObserver().getWorldTranslation().y);
    }

    @Override
    public Quaternion getVrCameraRotation(){
        return getLeftCamera().getRotation();
    }

    @Override
    public void setXrVrMode(XrVrMode xrVrMode){
        xrSession.setXrVrBlendMode(xrVrMode);
    }
}

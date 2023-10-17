package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.audio.VrAudioListenerState;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class XrAppState extends BaseAppState{
    private static final Logger LOGGER = Logger.getLogger(XrAppState.class.getName());

    public static String ID = "XrAppState";

    @Getter
    OpenXrSessionManager xrSession;
    @Getter
    Camera leftCamera;
    @Getter
    Camera rightCamera;

    ViewPort leftViewPort;
    ViewPort rightViewPort;

    /**
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

    @SuppressWarnings("unused")
    public XrAppState(){
        this(new XrSettings());
    }
    public XrAppState(XrSettings xrSettings){
        super(ID);
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

        if (settings.isVSync()){
            LOGGER.warning("VSync is enabled. This will cause stuttering in VR. Please disable it. Frame rate should be controlled by the headset, not the monitor");
        }

        xrSession = OpenXrSessionManager.createOpenXrSession(windowHandle, xrSettings);

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

        leftViewPort = app.getRenderManager().createPreView("Left Eye", leftCamera);
        leftViewPort.setClearFlags(true, true, true);

        rightViewPort = app.getRenderManager().createPreView("Right Eye", rightCamera);
        rightViewPort.setClearFlags(true, true, true);
        leftViewPort.attachScene(((SimpleApplication) app).getRootNode());
        rightViewPort.attachScene(((SimpleApplication) app).getRootNode());

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

    /**
     * Allows initialisation of both eyes viewports (e.g. adding scene processors or changing the background colour).
     * Note that it is safe to call this before the state is initialised (the code will wait till it is and then run it)
     */
    @SuppressWarnings("unused")
    public void configureBothViewports(Consumer<ViewPort> configureViewport){
        if (leftViewPort == null){
            runOnceHaveCameraPositions.add(() -> {
                configureViewport.accept(leftViewPort);
                configureViewport.accept(rightViewPort);
            });
        }else{
            configureViewport.accept(leftViewPort);
            configureViewport.accept(rightViewPort);
        }
    }

    @Override
    protected void cleanup(Application app){
        xrSession.destroy();
        app.getRenderManager().removePreView(leftViewPort);
        app.getRenderManager().removePreView(rightViewPort);
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
            //must set every frame due to OpenXR buffering to multiple images
            leftViewPort.setOutputFrameBuffer(inProgressXrRender.getLeftBufferToRenderTo());
            rightViewPort.setOutputFrameBuffer(inProgressXrRender.getRightBufferToRenderTo());

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

    /**
     * If you've requested extra extensions in {@link XrSettings} this method can be used to check if they really
     * were loaded. Extensions are things like "XR_KHR_binding_modification"
     */
    @SuppressWarnings("unused")
    public boolean checkExtensionLoaded(String extensionName){
        return getExtensionsLoaded().get(extensionName);
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

    /**
     * Sets the near clip plane for the cameras (will trigger a refresh of the projection matrix).
     * <p>
     * Note that the field of view cannot be changed by the user because it is set by OpenXr to reflect the devices
     * lens arrangement.
     */
    @SuppressWarnings("unused")
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

    /**
     * Sets the observer position. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. This is a highly technical method you
     * probably don't want to use, if you want to move the player directly (for example to support a teleport-style movement)
     * use {@link XrAppState#movePlayersFeetToPosition(Vector3f)}.
     *
     * @param observerPosition observer position
     */
    @SuppressWarnings("unused")
    public void setObserverPosition(Vector3f observerPosition){
        observer.setLocalTranslation(observerPosition);
    }

    /**
     * Moves the players face to the requested position. This is useful for teleportation style movement.
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param facePosition the facePosition.
     */
    @SuppressWarnings("unused")
    public void movePlayersFaceToPosition(Vector3f facePosition){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.movePlayerFaceToPosition(this, facePosition));
    }

    /**
     * Moves the players feet to the requested position. This is useful for teleportation style movement.
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param feetPosition the feetPosition.
     */
    public void movePlayersFeetToPosition(Vector3f feetPosition){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.movePlayerFeetToPosition(this, feetPosition));
    }


    /**
     * Sets the observer rotation. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. Note that rotating the
     * observer may implicitly move the player if they aren't currently standing exactly at the observer position.
     * <p>
     * Note that it's probably a bad idea to apply any sort of rotation other than about the Y axis, but you can (this is
     * the only rotation method that supports that).
     * </p>
     * This is a highly technical method, and you're more likely to want one of these methods:
     * <ul>
     *     <li>{@link XrAppState#rotateObserverWithoutMovingPlayer}</li>
     *     <li>{@link XrAppState#playerLookInDirection}</li>
     *     <li>{@link XrAppState#playerLookAtPosition}</li>
     * </ul>
     * @param observerRotation observer rotation
     */
    @SuppressWarnings("unused")
    public void setObserverRotation(Quaternion observerRotation){
        getObserver().setLocalRotation(observerRotation);
    }

    /**
     * Applies a <strong>relative</strong> rotation to the observer. This also applys the same relative rotation to the player.
     * The observer is also moved so the player doesn't seem to move in the virtual world.
     * <p>
     * Often you'll want to programatically turn the player, which should be done by rotating the observer.
     * However, if the player isn't standing directly above the observer this rotation will induce motion.
     * This method corrects for that and gives the impression the player is just turning
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param angleAboutYAxis the requested turn angle. Positive numbers turn left, negative numbers turn right
     */
    @SuppressWarnings("unused")
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

    /**
     * This will rotate the observer such that the player is looking at the requested position. Only considered rotation
     * in the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     * <p>
     * If the position is the same as the current position, this will do nothing.
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     */
    @SuppressWarnings("unused")
    public void playerLookAtPosition(Vector3f position){
        this.runOnceHaveCameraPositions.add(() -> TamarinUtilities.playerLookAtPosition(this, position));
    }

    /**
     * Returns the rotation of the VR cameras (technically the left camera, but they should be the same
     */
    public Vector3f getVrCameraLookDirection(){
        return getLeftCamera().getDirection();
    }

    /**
     * Returns the average position of the 2 VR cameras (i.e. half way between the left and right eyes)
     */
    public Vector3f getVrCameraPosition(){
        return getLeftCamera().getLocation().add(getRightCamera().getLocation()).mult(0.5f);
    }

    public Vector3f getPlayerFeetPosition(){
        return getVrCameraPosition().clone().setY(getObserver().getWorldTranslation().y);
    }
}

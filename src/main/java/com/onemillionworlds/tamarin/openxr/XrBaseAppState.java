package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;

import java.util.function.Consumer;

/**
 * Applications that want to work in both VR and Desktop mode can rely on this class, and it will provide a
 * consistent interface for both modes. Calls that don't make sense in one mode will be ignored (not exception!).
 */
public abstract class XrBaseAppState extends BaseAppState{

    public static String ID = "XrAppState";

    public XrBaseAppState(){
        super(ID);
    }

    /**
     * Allows initialisation of both eyes viewports (e.g. adding scene processors or changing the background colour).
     * Note that Tamarin forms MORE THAN TWO viewports (because it is triple buffered).  This method may be called
     * now (for existing viewports) or when a new viewport is created (if they haven't yet been intialised). You
     * should anticipate that this method may be called 6 times.
     */
    public abstract void setMainViewportConfiguration(Consumer<ViewPort> configureViewport);

    /**
     * Adds an additional scene (with associated viewports for both eyes and triple buffering) that will be
     * an overlay to the main scene. This is useful for things like debug shapes or a menu screen that shouldn't be clipped by the
     * main game scene.
     *
     * <p>
     *     Note that Tamarin will not take charge of calling `node.updateLogicalState()` and `node.updateGeometricState(tpf)`
     *     on the additional viewport's root node, so you need to do that within your update method after all other node
     *     mutations are done.
     * </p>
     *
     * @return a ViewportConfigurator that can be used to remove the additional viewports or update their configuration
     */
    public abstract ViewportConfigurator addAdditionalViewport(AdditionalViewportRequest additionalViewportRequest);

    /**
     * If the state has not yet been initialised will run when the eye cameras are positioned for the first time
     * (Otherwise will run the next time they are positioned. i.e. the next update)
     * <p>
     * This is useful for things you'd like to run once the XR environment is set up
     * </p>
     * @param runnable the code to run
     */
    public abstract void runAfterInitialisation(Runnable runnable);

    /**
     * Runs the provided function just after any already requested player movements have occurred.
     * E.g. if you've called {@link XrBaseAppState#movePlayersFaceToPosition(Vector3f)} but then want to
     * do something that is going to query that position putting it within this enqueue may make it more reliable
     *
     * <p>
     *     This method <b>is not</b> thread safe. It is intended to be called from the JME thread
     * </p>
     */
    public void enqueue(Runnable runnable){
        runAfterInitialisation(runnable);
    }

    /**
     * Will return a string that describes the system (e.g. "SteamVR/OpenXR : oculus"). This is useful for debugging.
     * <p>
     *     In general an application should not change it's behaviour by sniffing the device type, actions should be
     *     used instead to abstract away the specific device. Logging is a good use case for this method.
     * </p>
     * @return the system name, which may include both the headset and OpenXR runtime
     */
    public abstract String getSystemName();

    /**
     * If you've requested extra extensions in {@link XrSettings} this method can be used to check if they really
     * were loaded. Extensions are things like "XR_KHR_binding_modification"
     */
    public abstract boolean checkExtensionLoaded(String extensionName);

    /**
     * Sets the near clip plane for the cameras (will trigger a refresh of the projection matrix).
     * <p>
     * Note that the field of view cannot be changed by the user because it is set by OpenXr to reflect the devices
     * lens arrangement.
     */
    @SuppressWarnings("unused")
    public abstract void setNearClip(float nearClip);

    /**
     * Sets the far clip plane for the cameras (will trigger a refresh of the projection matrix).
     * <p>
     * Note that the field of view cannot be changed by the user because it is set by OpenXr to reflect the devices
     * lens arrangement.
     */
    public abstract void setFarClip(float farClip);

    /**
     * Sets the observer position. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. This is a highly technical method you
     * probably don't want to use, if you want to move the player directly (for example to support a teleport-style movement)
     * use {@link XrAppState#movePlayersFeetToPosition(Vector3f)}.
     *
     * @param observerPosition observer position
     */
    public abstract void setObserverPosition(Vector3f observerPosition);

    /**
     * Gets the observer position. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. This is a highly technical method you
     * probably don't want to use, if you want to move the player directly (for example to support a teleport-style movement)
     * use {@link XrAppState#movePlayersFeetToPosition(Vector3f)}.
     *
     * @return  observerPosition observer position
     */
    public abstract Vector3f getObserverPosition();

    /**
     * Moves the players face to the requested position. This is useful for teleportation style movement.
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param facePosition the facePosition.
     */
    public abstract void movePlayersFaceToPosition(Vector3f facePosition);

    /**
     * Moves the players feet to the requested position. This is useful for teleportation style movement.
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     * @param feetPosition the feetPosition.
     */
    public abstract void movePlayersFeetToPosition(Vector3f feetPosition);

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
    public abstract void setObserverRotation(Quaternion observerRotation);


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
    public abstract void rotateObserverWithoutMovingPlayer(float angleAboutYAxis);

    /**
     * This will rotate the observer such that the player is looking in the requested direction. Only considered rotation
     * in the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     * <p>
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     */
    public abstract void playerLookInDirection(Vector3f lookDirection);

    /**
     * This will rotate the observer such that the player is looking at the requested position. Only considered rotation
     * in the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     * <p>
     * If the position is the same as the current position, this will do nothing.
     * Note that this queues the result till the next update, which is much better when these calls are chained multiple in the same tick
     */
    public abstract void playerLookAtPosition(Vector3f position);

    /**
     * Returns the rotation of the VR cameras (technically the left camera, but they should be the same
     */
    public abstract Vector3f getVrCameraLookDirection();

    /**
     * Returns the average position of the 2 VR cameras (i.e. half way between the left and right eyes)
     */
    public abstract Vector3f getVrCameraPosition();

    public abstract Vector3f getPlayerFeetPosition();

    /**
     * The observer's position in the virtual world maps to the VR origin in the real world.
     *
     * <p>
     *     Note the observer IS NOT the VR camera position.
     *     See {@link <a href="https://github.com/oneMillionWorlds/Tamarin/wiki/Understanding-the-observer">Understanding the observer</a>}
     * </p>
     */
    public abstract Node getObserver();

    public abstract Quaternion getVrCameraRotation();

    /**
     * Configures the XR/VR mode for the application. This method determines the specific
     * XR/VR mode to be used during runtime, if in XR mode (and the hardware supports it)
     * then black/transparent pixels may show through to the real world (based on the mode)
     *
     * <p>
     *     It is likely you'll need to add a passthrough extension, e.g. FBPassthrough.XR_FB_PASSTHROUGH_EXTENSION_NAME
     * </p>
     * <p>
     *      <b>EXPERIMENTAL</b> Note; this feature is currently untested and may not work. Many headsets do not yet support AR
     * </p>
     *
     * @param xrVrMode the XR/VR mode to set, which defines the behavior and configuration
     *                 of the application in the XR/VR environment.
     */
    public abstract void setXrVrMode(XrVrMode xrVrMode);

    public abstract CameraResolution getCameraResolution();

    public record CameraResolution(int width, int height){

    }
}

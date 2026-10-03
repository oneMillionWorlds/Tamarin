package com.onemillionworlds.tamarin.openxr;

import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.observable.ObservableDataEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;

import java.util.Map;
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

    public abstract Map<String, Boolean> getExtensionsLoaded();

    /**
     * If you've requested extra extensions in {@link XrSettings} this method can be used to check if they really
     * were loaded. Extensions are things like "XR_KHR_binding_modification"
     */
    public boolean checkExtensionLoaded(String extensionName){
        //the below converts nulls to false
        return getExtensionsLoaded().get(extensionName) == Boolean.TRUE;
    }
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
     * use {@link XrBaseAppState#movePlayersFeetToPosition(Vector3f)}.
     *
     * @param observerPosition observer position
     */
    public abstract void setObserverPosition(Vector3f observerPosition);

    /**
     * Gets the observer position. The observer is the point in the virtual world that maps to the VR origin in the real world.
     * <strong>NOTE: the observer is only indirectly related to the players head position</strong>. This is a highly technical method you
     * probably don't want to use, if you want to move the player directly (for example to support a teleport-style movement)
     * use {@link XrBaseAppState#movePlayersFeetToPosition(Vector3f)}.
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
     *     <li>{@link XrBaseAppState#rotateObserverWithoutMovingPlayer}</li>
     *     <li>{@link XrBaseAppState#playerLookInDirection}</li>
     *     <li>{@link XrBaseAppState#playerLookAtPosition}</li>
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
     * </p>
     * @see <a href="https://github.com/oneMillionWorlds/Tamarin/wiki/Understanding-the-observer">Understanding the observer</a>
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

    /**
     * The observables that the session (or simulated session) reports its state to. All the session related methods
     * (e.g. {@link #subscribeToShouldPause()}) are backed by this.
     */
    protected abstract XrSessionObservables getSessionObservables();

    /**
     * The current state of the OpenXR session. See {@link SessionState} for what each state means.
     * <p>
     *     In desktop simulation mode this is always {@link SessionState#FOCUSED}.
     * </p>
     */
    public SessionState getSessionState(){
        return getSessionObservables().getSessionState();
    }

    /**
     * Returns true if the session is focused, i.e. the application is visible to the user and is receiving input.
     * <p>
     *     The session will lose focus if (for example) the user opens the system menu (e.g. the SteamVR dashboard
     *     or the Quest universal menu). Note that taking the headset off does not necessarily cause focus to be lost
     *     (it doesn't on the Quest), see {@link #isUserPresent()} for that.
     * </p>
     * <p>
     *     Most applications that want to pause their game logic should use {@link #shouldPause()} which combines
     *     focus and user presence.
     * </p>
     */
    public boolean isSessionFocused(){
        return getSessionObservables().isSessionFocused();
    }

    /**
     * Returns true if the user is wearing the headset.
     * <p>
     *     This relies on the XR_EXT_user_presence extension (requested by default in {@link XrSettings}), which uses
     *     the headset's proximity sensor. If the runtime doesn't support it (or the proximity sensor is disabled, as
     *     can be done on developer headsets) the user is always reported as present.
     * </p>
     * <p>
     *     In desktop simulation mode this is always true.
     * </p>
     */
    public boolean isUserPresent(){
        return getSessionObservables().isUserPresent();
    }

    /**
     * Returns true if the game logic should be paused. This is the case if the session is not focused (e.g. the
     * user has opened the system menu) or the user is not present (e.g. they have taken the headset off).
     * <p>
     *     In desktop simulation mode this is always false.
     * </p>
     * @see #subscribeToShouldPause()
     */
    public boolean shouldPause(){
        return getSessionObservables().shouldPause();
    }

    /**
     * Obtains a subscription that can be used to determine if the {@link SessionState} has changed (and what it now is).
     * <p>
     *     Most applications will want {@link #subscribeToShouldPause()} instead.
     * </p>
     */
    public ObservableValueSubscription<SessionState> subscribeToSessionState(){
        return getSessionObservables().subscribeToSessionState();
    }

    /**
     * Obtains a subscription that can be used to determine if the session has gained or lost focus. Focus is lost
     * when the application is no longer receiving input, e.g. because the user has opened the system menu.
     * <p>
     *     Note that taking the headset off does not necessarily cause focus to be lost. Most applications will want
     *     {@link #subscribeToShouldPause()} instead, which also considers whether the user is wearing the headset.
     * </p>
     */
    public ObservableValueSubscription<Boolean> subscribeToSessionFocused(){
        return getSessionObservables().subscribeToSessionFocused();
    }

    /**
     * Obtains a subscription that can be used to determine if the user has put on or taken off the headset.
     * See {@link #isUserPresent()} for caveats.
     */
    public ObservableValueSubscription<Boolean> subscribeToUserPresent(){
        return getSessionObservables().subscribeToUserPresent();
    }

    /**
     * Obtains a subscription that can be used to determine if the game logic should pause or resume. The value is true
     * (should pause) if the session is not focused (e.g. the user has opened the system menu) or the user is not present
     * (e.g. they have taken the headset off).
     * <p>
     *     The pausing should be done within the application (e.g. by disabling game app states) rather than by pausing
     *     the JME application as a whole (e.g. via {@link com.jme3.app.LostFocusBehavior#PauseOnLostFocus}). The OpenXR
     *     frame loop must continue to run while paused, otherwise the headset will consider the application to have
     *     frozen.
     * </p>
     * <p>
     *     Example:
     * </p>
     * <pre>{@code
     * ObservableValueSubscription<Boolean> shouldPauseSubscription = xrAppState.subscribeToShouldPause();
     * ...
     * public void update(float tpf){
     *     if(shouldPauseSubscription.checkHasChanged()){
     *         gameState.setEnabled(!shouldPauseSubscription.get());
     *     }
     * }
     * }</pre>
     */
    public ObservableValueSubscription<Boolean> subscribeToShouldPause(){
        return getSessionObservables().subscribeToShouldPause();
    }

    /**
     * Obtains a subscription that reports everything that interrupts the user's gameplay: the session losing focus
     * (e.g. the system menu being opened), the headset being taken off, and a controller being lost (e.g. its battery
     * dying). Each interrupt is reported when it {@link GameplayInterrupt.Phase#STARTED starts} and when it
     * {@link GameplayInterrupt.Phase#ENDED ends}.
     * <p>
     *     This is an alternative to {@link #subscribeToShouldPause()} for applications that want to pause and let the
     *     user decide when to resume (e.g. a pause menu with a "Continue" button) rather than resuming automatically.
     *     It is also the only one of the two that includes lost controllers, which don't end by themselves (the user
     *     may want to carry on without the controller).
     * </p>
     * <p>
     *     Example:
     * </p>
     * <pre>{@code
     * ObservableDataEventSubscription<GameplayInterrupt> interrupts = xrAppState.subscribeToGameplayInterrupts();
     * ...
     * public void update(float tpf){
     *     for(GameplayInterrupt interrupt : interrupts.pollEvents()){
     *         if (interrupt.isStarted()){
     *             showPauseMenu(interrupt); // which pauses the game until the user presses continue
     *         }
     *     }
     * }
     * }</pre>
     * <p>
     *     Controller interrupts require an {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState} to be
     *     attached. Nothing is ever reported in desktop simulation mode.
     * </p>
     */
    public ObservableDataEventSubscription<GameplayInterrupt> subscribeToGameplayInterrupts(){
        return getSessionObservables().subscribeToGameplayInterrupts();
    }

    /**
     * Reports a gameplay interrupt to subscribers of {@link #subscribeToGameplayInterrupts()}.
     * <p>
     *     This is primarily used internally (by the action state to report lost controllers), applications don't
     *     normally need to call it.
     * </p>
     */
    public void reportGameplayInterrupt(GameplayInterrupt interrupt){
        getSessionObservables().fireGameplayInterrupt(interrupt);
    }

    /**
     * Obtains a subscription that will report when the OpenXR runtime has signalled that a reference space is about to
     * change. This happens when (for example) the user recentres their view or redefines their play area (guardian/chaperone).
     * <p>
     *     This fires for changes to any reference space (e.g. stage or local). Tamarin's camera and hand positions
     *     will update automatically, but if the application has placed things relative to the player's real world
     *     position (e.g. a menu placed in front of the player) it may want to reposition them.
     * </p>
     * <p>
     *     In desktop simulation mode this never fires.
     * </p>
     */
    public ObservableEventSubscription subscribeToReferenceSpaceChangePending(){
        return getSessionObservables().subscribeToReferenceSpaceChangePending();
    }

    public static final class CameraResolution{
        private final int width;
        private final int height;

        public CameraResolution(int width, int height){
            this.width = width;
            this.height = height;
        }

        public int width(){
            return width;
        }

        public int height(){
            return height;
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CameraResolution that = (CameraResolution) o;
            return width == that.width && height == that.height;
        }

        @Override
        public int hashCode(){
            int result = width;
            result = 31 * result + height;
            return result;
        }

        @Override
        public String toString(){
            return "CameraResolution{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}

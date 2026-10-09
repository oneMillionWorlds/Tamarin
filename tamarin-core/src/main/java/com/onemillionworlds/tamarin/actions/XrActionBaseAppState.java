package com.onemillionworlds.tamarin.actions;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.actions.state.HandTrackingAimState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;
import com.onemillionworlds.tamarin.handgestures.HandGestureState;
import com.onemillionworlds.tamarin.handgestures.HandGestureTracker;
import com.onemillionworlds.tamarin.handgestures.HandTrackingDataSource;
import com.onemillionworlds.tamarin.handgestures.TrackedHand;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.observable.ObservableDataEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.openxr.GameplayInterrupt;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class XrActionBaseAppState extends BaseAppState{

    public static final String ID = "OpenXrActionAppState";

    private final InteractionProfileTracker interactionProfileTracker = new InteractionProfileTracker();

    /**
     * Time in seconds since this state started updating, used for the controller lost grace period
     */
    private double time = 0;

    /*
     * These are used to forward controller lost/regained events on to the XrBaseAppState's gameplay interrupts
     */
    private final ObservableDataEventSubscription<HandSide> controllerLostForInterrupts = interactionProfileTracker.controllerLost.subscribe();
    private final ObservableDataEventSubscription<HandSide> controllerRegainedForInterrupts = interactionProfileTracker.controllerRegained.subscribe();

    private final Map<HandSide, HandGestureTracker> handGestureTrackers = new EnumMap<>(HandSide.class);

    private HandGestureSourcePreference handGestureSourcePreference = HandGestureSourcePreference.AUTOMATIC;

    /**
     * Hands are only located (and gestures tracked) once something has asked for them, to avoid the cost for
     * applications that don't use them
     */
    private boolean handTrackingRequested = false;

    /**
     * Each hand's tracking data, located once per frame (in update) so the getters don't have to make OpenXR calls
     */
    private final Map<HandSide, Optional<TrackedHand>> latestTrackedHands = new EnumMap<>(HandSide.class);

    private SyntheticGestureBindings syntheticGestureBindings = SyntheticGestureBindings.none();

    private List<String> activeActionSetNames = List.of();

    /**
     * The sub action paths (usually the hands) of each action, needed to work out actions with gesture bindings hand by hand
     */
    private Map<ActionHandle, List<String>> subActionPathsByAction = Map.of();

    public XrActionBaseAppState(){
        super(ID);
        for(HandSide handSide : HandSide.values()){
            handGestureTrackers.put(handSide, new HandGestureTracker(handSide));
            latestTrackedHands.put(handSide, Optional.empty());
        }
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        time += tpf;
        interactionProfileTracker.tick(isAlreadyPaused(), time);
        forwardControllerInterrupts();
        updateHandTracking(tpf);
    }

    /**
     * Locates the hands (once per frame, the getters use the cached result) and updates the hand gestures.
     */
    private void updateHandTracking(float tpf){
        if (!handTrackingRequested && !syntheticGestureBindings.hasAny()){
            return;
        }
        boolean anyTracked = false;
        for(HandSide handSide : HandSide.values()){
            Optional<TrackedHand> trackedHand = locateTrackedHand(handSide);
            latestTrackedHands.put(handSide, trackedHand);
            anyTracked |= trackedHand.isPresent();
        }

        Vector3f headPosition = new Vector3f();
        Quaternion headRotation = new Quaternion();
        XrBaseAppState xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        if (anyTracked && xrAppState != null){
            headPosition = xrAppState.getVrCameraPosition();
            headRotation = xrAppState.getVrCameraRotation();
        }
        for(HandSide handSide : HandSide.values()){
            handGestureTrackers.get(handSide).update(latestTrackedHands.get(handSide), headPosition, headRotation, tpf);
        }
        syntheticGestureBindings.update(handSide -> handGestureTrackers.get(handSide).getState(handGestureSourcePreference), activeActionSetNames);
    }

    private void forwardControllerInterrupts(){
        List<HandSide> lost = controllerLostForInterrupts.pollEvents();
        List<HandSide> regained = controllerRegainedForInterrupts.pollEvents();
        if (lost.isEmpty() && regained.isEmpty()){
            return;
        }
        XrBaseAppState xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        if (xrAppState == null){
            return;
        }
        lost.forEach(hand -> xrAppState.reportGameplayInterrupt(GameplayInterrupt.controllerLost(GameplayInterrupt.Phase.STARTED, hand)));
        regained.forEach(hand -> xrAppState.reportGameplayInterrupt(GameplayInterrupt.controllerLost(GameplayInterrupt.Phase.ENDED, hand)));
    }

    /**
     * Given the action handle (e.g. a logical "fire" or "walk") returns the localised description of the
     * button that is currently bound to that action
     *
     * <p>
     *     Note that tamarin requests the user-path (E.g. "Left Hand") and input component portion
     *     (e.g. "Trigger") so ideally the results will look like "Left Hand Trigger". However,
     *     some runtimes (e.g. the quest) return the interaction profile even if unasked for, so the
     *     returned value may be "Left Hand Meta Touch Plus Controller Trigger". This is unfortunate
     *     but tamarin can't prevent this happening
     * </p>
     *
     * <p>
     *     Note this method will return an empty list until the actions are synced (which happens once the session is fully running
     *     and this action state updates. Basically be careful calling this method at application start up).
     * </p>
     *
     * @param actionHandle the handle to get the binding string for
     * @return a String containing a localised description of the physical button(s) paired with the action
     */
    public abstract List<String> getLocalisedButtonNameForAction(ActionHandle actionHandle);


    /**
     * This sets the action sets that will be active. I.e. the actions in this action set will work, others will be ignored
     * @param actionSets the names of the action sets
     */
    public void setActiveActionSets(String... actionSets){
        setActiveActionSets(Arrays.asList(actionSets));
    }

    /**
     * This sets the action sets that will be active. I.e. the actions in this action set will work, others will be ignored
     * @param actionSets the names of the action sets
     */
    public abstract void setActiveActionSets(List<String> actionSets);

    public abstract void doNotSuppressRepeatedErrors();

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for digital style actions (a button is pressed, or not)
     *
     * @param action The action. E.g. openInventory
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    @SuppressWarnings("unused")
    public BooleanActionState getBooleanActionState(ActionHandle action){
        return getBooleanActionState(action, null);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for digital style actions (a button is pressed, or not)
     *
     * @param action The action. E.g. openInventory
     * @param restrictToInput If the same action is bound to multiple hands then restrict to hand can be used to
     *                        only return the value from one hand. E.g. "/user/hand/left". See {@link HandSide} which
     *                        contains common values. Other (probably less useful) known values are: "/user/gamepad"
     *                        and "/user/head". Can be null for no restriction
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    public abstract BooleanActionState getBooleanActionState(ActionHandle action, String restrictToInput);

    /**
     * A pose is where a hand is, and what its rotation is.
     * <p>
     * This returns the pose in the jMonkeyEngine's world coordinate system (which may have been offset from the
     * real world space by moving the observer).
     *
     * @param actionName the action name that has been bound to a pose in the action manifest
     * @return the PoseActionState
     */
    public abstract Optional<PoseActionState> getPose_worldRelative(ActionHandle actionName, HandSide handSide);

    /**
     * A pose is where a hand is, and what its rotation is.
     * <p>
     * This returns the pose in the observers coordinate system (note that observer does not mean "eyes", it means
     * a reference point placed in the scene that corresponds to the real world VR origin)
     *
     * @param action the action that has been bound to a pose in the action manifest
     * @param stageRelative if the output should be relative to the stage origin (at the feet, in the centre of the device defined
     *                      stage). If not will be relative to the local origin (the headsets position/rotation at start up time
     *                      - excluding pitch/roll).
     */
    public abstract Optional<PoseActionState> getPose(ActionHandle action, HandSide handSide, boolean stageRelative);


    /**
     * Gets the joint positions of the hand in the coordinate system defined by the pose (So if the pose is a grip
     * pose it's relative to the grip, if it's the aim pose its relative to the aim).
     * <p>
     * It's only really a good idea to call this if the pose fetch has already succeeded
     * </p>
     * @param poseAction the pose (just for the coordinate system)
     * @param handSide the handside to get the joint positions for
     */
    public abstract Optional<Map<HandJoint, BonePose>> getSkeleton(ActionHandle poseAction, HandSide handSide);

    /**
     * <b>META ONLY.</b> Gets the extra gesture information a Meta runtime provides for a bare (tracked) hand: the pinch
     * strength of each finger against the thumb, the runtime's own decision on whether each finger is pinching, and a
     * stabilised aim pose. This comes from the XR_FB_hand_tracking_aim extension (which Tamarin requests by default).
     * <p>
     *     This doesn't go through the action system, so it can't be rebound by the user and isn't affected by action
     *     sets being active or not. Prefer actions (e.g. {@link com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt}
     *     pinch) where they can express what is needed. This is most useful for things the action system can't express,
     *     like the thumb pinching the middle, ring or little finger.
     * </p>
     * <p>
     *     Returns empty if the extension isn't available (any non-Meta runtime, or desktop simulation), if hand tracking
     *     isn't available, or if the hand isn't currently being tracked (including when the user is holding a
     *     controller).
     * </p>
     * <p>
     *     Hands are only tracked once this (or another hand tracking method) has been called, so the very first call
     *     will return empty. Call it every frame. The value is updated once per frame.
     * </p>
     * @param handSide the hand to get the state for
     * @return the aim state, if available
     */
    public Optional<HandTrackingAimState> getHandTrackingAimState(HandSide handSide){
        handTrackingRequested = true;
        return latestTrackedHands.get(handSide).flatMap(TrackedHand::getRuntimeAimState);
    }

    /**
     * Where the hand's tracking data (joints) is currently coming from: the bare hand itself, or a controller held in
     * the hand (some runtimes, e.g. SteamVR with Valve Index controllers, work out hand joints from the controller).
     * Useful for things like choosing a hand model, or tutorial text ("pinch" vs "pull the trigger").
     * <p>
     *     Returns {@link HandTrackingDataSource#UNKNOWN} if the runtime doesn't support XR_EXT_hand_tracking_data_source,
     *     and empty if the hand isn't tracked at all. Note that some runtimes (e.g. Quest) provide no hand tracking
     *     data at all while a controller is held, so on those holding a controller gives empty rather than
     *     {@link HandTrackingDataSource#CONTROLLER}.
     * </p>
     * <p>
     *     Hands are only tracked once this (or another hand tracking method) has been called, so the very first call
     *     will return empty. Call it every frame. The value is updated once per frame.
     * </p>
     *
     * @param handSide the hand
     * @return the data source, or empty if the hand isn't tracked (or hand tracking isn't supported, or in desktop simulation)
     */
    public Optional<HandTrackingDataSource> getHandTrackingDataSource(HandSide handSide){
        handTrackingRequested = true;
        return latestTrackedHands.get(handSide).map(TrackedHand::getDataSource);
    }

    /**
     * Gets the gestures a bare (tracked) hand is making: per finger pinches, an aim ray, finger curl, fist, point,
     * thumbs up and palm direction. See {@link HandGestureState} for details.
     * <p>
     *     Pinches and the aim ray come from the runtime's own gesture recognition where available (currently Meta
     *     runtimes), otherwise from Tamarin's own recognition based on the positions of the hand's joints. This can be
     *     controlled with {@link #setHandGestureSourcePreference}. Everything else is always Tamarin's.
     * </p>
     * <p>
     *     Returns empty if the hand isn't currently tracked as a bare hand (e.g. the user is holding a controller, see
     *     {@link #getHandTrackingDataSource}),
     *     if hand tracking isn't supported, or in desktop simulation.
     * </p>
     * <p>
     *     Hand gestures are only tracked once this has been called (or the action manifest contains gesture
     *     bindings), so the very first call will return empty. Call it every frame.
     * </p>
     *
     * @param handSide the hand
     * @return the hand's gestures, if available
     */
    public Optional<HandGestureState> getHandGestureState(HandSide handSide){
        return getHandGestureState(handSide, handGestureSourcePreference);
    }

    /**
     * As {@link #getHandGestureState(HandSide)}, but forces a particular source for the pinches and aim ray for
     * this call only (ignoring {@link #setHandGestureSourcePreference}). Mostly useful for comparing the runtime's
     * and Tamarin's gesture recognition.
     *
     * @param handSide the hand
     * @param preference which source to use for pinches and aim
     * @return the hand's gestures, if available
     */
    public Optional<HandGestureState> getHandGestureState(HandSide handSide, HandGestureSourcePreference preference){
        handTrackingRequested = true;
        return handGestureTrackers.get(handSide).getState(preference);
    }

    /**
     * Controls where the pinches and aim ray in {@link #getHandGestureState(HandSide)} come from, and whether actions
     * with gesture bindings combine them with the runtime's bindings or give one precedence. See
     * {@link HandGestureSourcePreference}. Defaults to {@link HandGestureSourcePreference#AUTOMATIC}.
     */
    public void setHandGestureSourcePreference(HandGestureSourcePreference handGestureSourcePreference){
        this.handGestureSourcePreference = handGestureSourcePreference;
    }

    public HandGestureSourcePreference getHandGestureSourcePreference(){
        return handGestureSourcePreference;
    }

    /**
     * For VR implementations to provide the raw hand tracking data (joints in world coordinates, plus the runtime's
     * gesture recognition if available). Called by this class once per hand per frame (only if hand tracking is in use),
     * and the result is cached for the getters, so implementations can make the OpenXR calls needed directly. Nothing
     * else should call this.
     *
     * @return the hand's data, or empty if the hand isn't tracked (or hand tracking isn't supported)
     */
    protected Optional<TrackedHand> locateTrackedHand(HandSide handSide){
        return Optional.empty();
    }

    /**
     * For VR implementations to register the gesture bindings in the action manifest.
     */
    protected void registerGestureBindings(ActionManifest manifest){
        this.syntheticGestureBindings = new SyntheticGestureBindings(manifest);
        this.subActionPathsByAction = new HashMap<>();
        for(ActionSet actionSet : manifest.getActionSets()){
            for(Action action : actionSet.getActions()){
                subActionPathsByAction.put(action.getActionHandle(), action.getSupportedSubActionPaths());
            }
        }
    }

    /**
     * For VR implementations to report which action sets are active (gesture bindings in inactive sets are ignored).
     */
    protected void recordActiveActionSets(List<String> actionSetNames){
        this.activeActionSetNames = List.copyOf(actionSetNames);
    }

    /**
     * For VR implementations to work out a boolean action's state from the runtime's state and any gesture bindings.
     * The runtime state function may be called several times (once per hand) when the action has gesture bindings.
     *
     * @param runtimeState gets the runtime's state of the action for a sub action path (or null for all)
     */
    protected BooleanActionState combineBooleanWithGestureBindings(ActionHandle action, String restrictToInput, Function<String, ActionSourceState> runtimeState){
        if (!syntheticGestureBindings.hasGestureBindings(action)){
            ActionSourceState runtime = runtimeState.apply(restrictToInput);
            return new BooleanActionState(runtime.getBooleanValue(), runtime.isChangedSinceLastSync());
        }
        // gesture bindings are per hand (and which source wins can differ per hand) so each hand is worked out
        // separately, then combined the way OpenXR would if the action was read for all hands
        List<BooleanActionState> subActionStates = new ArrayList<>();
        for(String subActionPath : subActionPathsToRead(action, restrictToInput)){
            ActionSourceState runtime = runtimeState.apply(subActionPath);
            ActionSourceState gesture = handFor(subActionPath)
                    .map(hand -> syntheticGestureBindings.getBooleanState(action, hand))
                    .orElse(ActionSourceState.INACTIVE);
            subActionStates.add(ActionSourceCombiner.combineBoolean(runtime, gesture, handGestureSourcePreference));
        }
        return ActionSourceCombiner.anyOf(subActionStates);
    }

    /**
     * For VR implementations to work out a float action's state from the runtime's state and any gesture bindings.
     * The runtime state function may be called several times (once per hand) when the action has gesture bindings.
     *
     * @param runtimeState gets the runtime's state of the action for a sub action path (or null for all)
     */
    protected FloatActionState combineFloatWithGestureBindings(ActionHandle action, String restrictToInput, Function<String, ActionSourceState> runtimeState){
        if (!syntheticGestureBindings.hasGestureBindings(action)){
            ActionSourceState runtime = runtimeState.apply(restrictToInput);
            return new FloatActionState(runtime.getValue(), runtime.isChangedSinceLastSync());
        }
        // see combineBooleanWithGestureBindings
        List<FloatActionState> subActionStates = new ArrayList<>();
        for(String subActionPath : subActionPathsToRead(action, restrictToInput)){
            ActionSourceState runtime = runtimeState.apply(subActionPath);
            ActionSourceState gesture = handFor(subActionPath)
                    .map(hand -> syntheticGestureBindings.getFloatState(action, hand))
                    .orElse(ActionSourceState.INACTIVE);
            subActionStates.add(ActionSourceCombiner.combineFloat(runtime, gesture, handGestureSourcePreference));
        }
        return ActionSourceCombiner.largestOf(subActionStates);
    }

    private List<String> subActionPathsToRead(ActionHandle action, String restrictToInput){
        if (restrictToInput != null){
            return List.of(restrictToInput);
        }
        return subActionPathsByAction.getOrDefault(action, Action.DEFAULT_SUB_ACTIONS);
    }

    private static Optional<HandSide> handFor(String subActionPath){
        for(HandSide handSide : HandSide.values()){
            if (handSide.restrictToInputString.equals(subActionPath)){
                return Optional.of(handSide);
            }
        }
        return Optional.empty();
    }

    /**
     * For VR implementations to add descriptions of gesture bindings to {@link #getLocalisedButtonNameForAction}.
     */
    protected List<String> describeGestureBindings(ActionHandle action){
        return syntheticGestureBindings.describe(action);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for analog style actions (most commonly triggers but button pressure can also be mapped in analog).
     * <p>
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     * <p>
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from
     *
     * @param action The action.
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the AnalogActionState that has details on how much the state has changed, what the state is etc.
     */
    public abstract FloatActionState getFloatActionState(ActionHandle action, String restrictToInput );

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for analog style actions (most commonly triggers but button pressure can also be mapped in analog).
     * </p>
     * <p>
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     * </p>
     * @param action The action.
     * @return the AnalogActionState that has details on how much the state has changed, what the state is etc.
     */
    public FloatActionState getFloatActionState(ActionHandle action){
        return getFloatActionState(action, null);
    }


    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for 2D style actions (most commonly joystick positions).
     * @param action The action.
     * @return the AnalogActionState that has details on how much the state has changed, what the state is etc.
     */
    public abstract Vector2fActionState getVector2fActionState(ActionHandle action);

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for 2D style actions (most commonly joystick positions).
     * <p>
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     * <p>
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from
     * @param action The action.
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the AnalogActionState that has details on how much the state has changed, what the state is etc.
     */
    public abstract Vector2fActionState getVector2fActionState(ActionHandle action, String restrictToInput );

    /**
     * Triggers a haptic action (aka a vibration).
     * <p>
     * Note if you want a haptic action in only one hand that is done either by only binding the action to one hand in
     * the action manifest's standard bindings or by binding to both and using {@link #triggerHapticAction(ActionHandle, float, float, float, String)}
     * to control which input it gets set to at run time
     *
     * @param actionHandle the action for haptic vibration (must be an action of type haptic in the action manifest).
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     */
    @SuppressWarnings("unused")
    public abstract void triggerHapticAction(ActionHandle actionHandle, float duration, float frequency, float amplitude);

    /**
     * Triggers a haptic action (aka a vibration) restricted to just one input (e.g. left or right hand).
     * <p>
     * Note that restrictToInput only restricts, it must still be bound to the input you want to send the haptic to in
     * the action manifest default bindings.
     * <p>
     * This method is typically used to bind the haptic to both hands then decide at run time which hand to sent to     *
     *
     * @param action The action for haptic vibration.
     * @param duration how long in seconds the vibration should be.
     * @param frequency in cycles per second (aka Hz)
     * @param amplitude between 0 and 1
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right, /user/hand/left. Or null, which means "both hands"
     */
    public abstract void triggerHapticAction(ActionHandle action, float duration, float frequency, float amplitude, String restrictToInput );

    /**
     * Stops a haptic action (aka a vibration) that is currently running, e.g. to end a long vibration early.
     *
     * @param action The action for haptic vibration.
     */
    @SuppressWarnings("unused")
    public void stopHapticAction(ActionHandle action){
        stopHapticAction(action, null);
    }

    /**
     * Stops a haptic action (aka a vibration) that is currently running, restricted to just one input (e.g. left
     * or right hand).
     *
     * @param action The action for haptic vibration.
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right, /user/hand/left. Or null, which means "both hands"
     */
    public abstract void stopHapticAction(ActionHandle action, String restrictToInput);

    /**
     * Returns the interaction profile currently in use for the given hand, e.g.
     * "/interaction_profiles/oculus/touch_controller". These strings match the PROFILE constants in the
     * {@link com.onemillionworlds.tamarin.actions.controllerprofile} classes (although the runtime may report profiles
     * Tamarin does not have a class for).
     * <p>
     *     This is useful for things like showing the correct controller model or button prompts. Note that, in
     *     general, applications should not change their <i>behaviour</i> based on the controller type, actions should
     *     be used for that.
     * </p>
     * <p>
     *     Empty if no profile is active for that hand (e.g. the controller is turned off), before the actions have
     *     been registered with the runtime, and always in desktop simulation mode. Note that the runtime will only
     *     report profiles for which the action manifest has suggested bindings.
     * </p>
     */
    public Optional<String> getCurrentInteractionProfile(HandSide handSide){
        return interactionProfileTracker.getCurrentProfile(handSide);
    }

    /**
     * Obtains a subscription that can be used to determine if the interaction profile in use for either hand has changed
     * (e.g. the user has switched from controllers to hand tracking). Use {@link #getCurrentInteractionProfile(HandSide)}
     * to get the new value(s).
     */
    public ObservableEventSubscription subscribeToInteractionProfileChanges(){
        return interactionProfileTracker.profileChanged.subscribe();
    }

    /**
     * Obtains a subscription that reports when a controller is lost, i.e. a hand that had a controller (an interaction
     * profile) no longer has one, and hasn't got it back within the grace period (see {@link #setControllerLostGracePeriod(double)}).
     * This is typically because the controller's battery has run out or it has been turned off. The data is the hand
     * that lost its controller.
     * <p>
     *     Many applications will want to pause and show a message when this happens (which the user can dismiss, as
     *     they may want to carry on without the controller). Unlike {@link com.onemillionworlds.tamarin.openxr.XrBaseAppState#subscribeToShouldPause()}
     *     this doesn't clear itself, the application decides when to resume.
     * </p>
     * <p>
     *     Notes:
     * </p>
     * <ul>
     *     <li>A hand that never had a controller (e.g. the user started with only one) is never reported as lost.</li>
     *     <li>A hand switching from a controller to hand tracking is not a loss (it still has a profile). However,
     *     hand tracking only gets a profile if the action manifest has suggested bindings for a hand tracking
     *     interaction profile, otherwise putting the controllers down to use hand tracking will look like a loss.</li>
     *     <li>Controllers disappearing while {@link com.onemillionworlds.tamarin.openxr.XrBaseAppState#shouldPause()}
     *     is already true are not reported. This is because taking the headset off causes controllers to sleep.</li>
     *     <li>Never fires in desktop simulation mode.</li>
     * </ul>
     *
     * @see #subscribeToControllerRegained()
     */
    public ObservableDataEventSubscription<HandSide> subscribeToControllerLost(){
        return interactionProfileTracker.controllerLost.subscribe();
    }

    /**
     * Obtains a subscription that reports when a controller that was previously reported lost (see
     * {@link #subscribeToControllerLost()}) has come back. The data is the hand that regained its controller.
     */
    public ObservableDataEventSubscription<HandSide> subscribeToControllerRegained(){
        return interactionProfileTracker.controllerRegained.subscribe();
    }

    /**
     * Updates the current interaction profiles, firing the change (and controller lost/regained) events as appropriate.
     * @param newProfiles the profile for each hand. Hands with no profile should be absent.
     */
    protected void updateCurrentInteractionProfiles(Map<HandSide, String> newProfiles){
        interactionProfileTracker.update(newProfiles, isAlreadyPaused(), time);
        forwardControllerInterrupts();
    }

    /**
     * Sets how long (in seconds) a controller must be missing before it is reported as lost (see
     * {@link #subscribeToControllerLost()}). Defaults to 1 second.
     * <p>
     *     This is a debounce, so that a controller that disappears only momentarily (e.g. it is briefly put down and
     *     picked up again) isn't reported as lost.
     * </p>
     */
    public void setControllerLostGracePeriod(double seconds){
        interactionProfileTracker.setLossGracePeriod(seconds);
    }

    public double getControllerLostGracePeriod(){
        return interactionProfileTracker.getLossGracePeriod();
    }

    private boolean isAlreadyPaused(){
        XrBaseAppState xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        return xrAppState != null && xrAppState.shouldPause();
    }

    /**
     * The OpenXR session may take some time to start, after that time the acion manifest is registered. Until that time
     * the actions are not available. This method allows you to see if the application is ready
     * @return if the state is ready to be queried about action states
     */
    public abstract boolean isReady();

    /**
     * If actions are already registered (which happens after the XR session is running) then runs the runnable immediately.
     * Otherwise, runs it after the actions are registered
     */
    @SuppressWarnings("unused")
    public abstract void runAfterActionsRegistered(Runnable runnable);

    /**
     * When the XR session is running actions are synced every update. Runnables added here will be run after the next
     * such sync.
     */
    @SuppressWarnings("unused")
    public abstract void runAfterNextActionsSync(Runnable runnable);

    /**
     *
     * Given a hand armature with names as defined in the passed boneNameMappings
     * it will use the bone stances (presumably from an ealier call to get the actions) to set the armature positions.
     * <p>
     * See <a href="https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XR_EXT_hand_tracking">Hand tracking spec</a>
     * <p>
     * NOTE: the bone orientation is surprising and non-natural. If you build a hand model, and it appears
     * distorted try importing the bones from Tamarin's starter blender file
     *
     * @param armature a JMonkey armature (aka set of bones)
     * @param boneStances the bone positions (as reported by OpenXR)
     * @param boneNameMappings the bone names to use. This is a map from the HandJoint enum to the bone name
     */
    public static void updateHandSkeletonPositions(Armature armature, Map<HandJoint, BonePose> boneStances, Map<HandJoint, String> boneNameMappings){
        for(Map.Entry<HandJoint, BonePose> bonePose : boneStances.entrySet()){
            String boneName = boneNameMappings.get(bonePose.getKey());
            Joint joint = armature.getJoint(boneName);
            if (joint!=null){
                joint.setLocalTranslation(bonePose.getValue().position());
                joint.setLocalRotation(bonePose.getValue().orientation());
            }
        }
    }

    public static class IncorrectActionTypeException extends RuntimeException{
        public IncorrectActionTypeException(String message){
            super(message);
        }
    }

}
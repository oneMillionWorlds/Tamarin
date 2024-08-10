package com.onemillionworlds.tamarin.actions;

import com.jme3.app.state.BaseAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class XrActionBaseAppState extends BaseAppState{

    public static final String ID = "OpenXrActionAppState";

    public XrActionBaseAppState(){
        super(ID);
    }

    /**
     * Gets the details of the physical button that is bound to the action. If multiple buttons are bound then multiple
     * results will be returned.
     * <p>
     *  This is useful for things like tutorial messages like "press the ... to fire" where ... can be infered
     *  based on the result of this call. This means your tutorial messages can work for all devices, even ones that
     *  did not exist when your application was released.
     * </p>
     *
     * <p>Example raw value outputs:</p>
     * <ul>
     *     <li>/user/hand/right/input/joystick/click</li>
     *     <li>/user/hand/left/input/joystick/y</li>
     *     <li>/user/hand/right/input/a/touch</li>
     *     <li>/user/hand/right/input/a/click</li>
     * </ul>
     *
     * <p>
     *     Be slightly careful as joysticks used as dPads may give surprising results like "/user/hand/left/input/joystick/y".
     *     For traditional button presses it should be fine, giving results like
     * </p>
     *
     * <p>
     *     Note this method will return an empty list until the actions are synced (which happens once the session is fully running
     *     and this action state updates. Basically be careful calling this method at application start up).
     * </p>
     * @param actionHandle the action that represents an action in the application (e.g. "fire")
     * @return the list of physical buttons that are bound to the action
     */
    public abstract List<PhysicalBindingInterpretation> getPhysicalBindingForAction(ActionHandle actionHandle);

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
    public abstract BooleanActionState getBooleanActionState(ActionHandle action);

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

}
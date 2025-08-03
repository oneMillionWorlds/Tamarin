package com.onemillionworlds.tamarin.actions.actionprofile;

import com.jme3.input.controls.KeyTrigger;
import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.controllerprofile.GoogleDaydreamController;
import com.onemillionworlds.tamarin.actions.controllerprofile.HtcProVive;
import com.onemillionworlds.tamarin.actions.controllerprofile.HtcViveController;
import com.onemillionworlds.tamarin.actions.controllerprofile.KhronosSimpleController;
import com.onemillionworlds.tamarin.actions.controllerprofile.MixedRealityMotionController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusGoController;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.actions.controllerprofile.ValveIndexController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Action{

    /**
     * By default, all actions support the left and right hand sides.
     */
    public static List<String> DEFAULT_SUB_ACTIONS = new ArrayList<>();

    static{
        DEFAULT_SUB_ACTIONS.add(HandSide.LEFT.restrictToInputString);
        DEFAULT_SUB_ACTIONS.add(HandSide.RIGHT.restrictToInputString);
    }

    private final ActionHandle actionHandle;

    /**
     * This is presented to the user as a description of the action. This string should be presented in the system’s current active locale.
     */
    private final String translatedName;

    private final ActionType actionType;
    /**
     * These are physical bindings to specific devices for this action.
     */
    private final List<SuggestedBinding> suggestedBindings;

    private final List<String> supportedSubActionPaths;

    private final Map<String,DesktopSimulationKeybinding> desktopSimulationKeybinding;

    public Action(ActionHandle actionHandle, String translatedName, ActionType actionType, List<SuggestedBinding> suggestedBindings){
        this.actionHandle = actionHandle;
        this.translatedName = translatedName;
        this.actionType = actionType;
        this.suggestedBindings = suggestedBindings;
        this.supportedSubActionPaths = DEFAULT_SUB_ACTIONS;
        this.desktopSimulationKeybinding = new HashMap<>();
    }

    public Action(ActionHandle actionName, String translatedName, ActionType actionType, List<SuggestedBinding> suggestedBindings, List<String> supportedSubActionPaths){
        this.actionHandle = actionName;
        this.translatedName = translatedName;
        this.actionType = actionType;
        this.suggestedBindings = suggestedBindings;
        this.supportedSubActionPaths = supportedSubActionPaths;
        this.desktopSimulationKeybinding = new HashMap<>();
    }

    private Action(ActionHandle actionName, String translatedName, ActionType actionType, List<SuggestedBinding> suggestedBindings, List<String> supportedSubActionPaths, Map<String,DesktopSimulationKeybinding> desktopSimulationKeybinding){
        this.actionHandle = actionName;
        this.translatedName = translatedName;
        this.actionType = actionType;
        this.suggestedBindings = suggestedBindings;
        this.supportedSubActionPaths = supportedSubActionPaths;
        this.desktopSimulationKeybinding = desktopSimulationKeybinding;
    }

    public String getActionName(){
        return actionHandle.actionName();
    }

    public String getActionSetName(){
        return actionHandle.actionSetName();
    }

    public ActionHandle getActionHandle(){
        return actionHandle;
    }

    /**
     * This is presented to the user as a description of the action. This string should be presented in the system’s current active locale.
     */
    public String getTranslatedName(){
        return translatedName;
    }

    public ActionType getActionType(){
        return actionType;
    }

    /**
     * These are physical bindings to specific devices for this action.
     */
    public List<SuggestedBinding> getSuggestedBindings(){
        return suggestedBindings;
    }

    public List<String> getSupportedSubActionPaths(){
        return supportedSubActionPaths;
    }

    public Map<String, DesktopSimulationKeybinding> getDesktopSimulationKeybinding(){
        return desktopSimulationKeybinding;
    }

    @SuppressWarnings("unused")
    public static ActionBuilder builder(){
        return new ActionBuilder();
    }

    @SuppressWarnings("unused")
    public static class ActionBuilder{
        private ActionHandle actionHandle;
        private String translatedName;

        private ActionType actionType;
        /**
         * These are physical bindings to specific devices for this action.
         */
        private final List<SuggestedBinding> suggestedBindings = new ArrayList<>();

        private List<String> supportedSubActionPaths = DEFAULT_SUB_ACTIONS;

        private Map<String,DesktopSimulationKeybinding> desktopDebugKeyTrigger = new HashMap<>();

        /**
         * This is used to identify the action when you want to programatically interact with it e.g. getting an actions value. It is anticipated that
         * these may be held in a static enum, or a static final field, or something similar where they can be easily accessed
         * application wide.
         * <p>
         * The action name should be things like "teleport", not things like "X Click". The idea is that they are
         * abstract concept your application would like to support and they are bound to specific buttons based on the suggested
         * bindings (which may be changed by the user, or guessed at by the binding).
         */
        public ActionBuilder actionHandle(ActionHandle actionHandle){
            this.actionHandle = actionHandle;
            return this;
        }

        /**
         * This is presented to the user as a description of the action. This string should be presented in the system’s current active locale.
         */
        public ActionBuilder translatedName(String translatedName){
            this.translatedName = translatedName;
            return this;
        }

        public ActionBuilder actionType(ActionType actionType){
            this.actionType = actionType;
            return this;
        }

        /**
         * Suggested bindings are physical bindings to specific devices for this action.
         * This method can be called multiple times to add more bindings (to the same action).
         * <p>
         * At least one binding should be added for each profile (aka controller) you explicitly want to support.
         * <p>
         * Intended usage is :
         * <code>
         *     withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().haptic())
         * </code>
         * <p>
         * Tamarin provided profiles are:
         * <ul>
         *     <li>{@link GoogleDaydreamController#PROFILE}</li>
         *     <li>{@link HtcProVive#PROFILE} - note this is the headset itself</li>
         *     <li>{@link HtcViveController#PROFILE}</li>
         *     <li>{@link KhronosSimpleController#PROFILE}</li>
         *     <li>{@link MixedRealityMotionController#PROFILE}</li>
         *     <li>{@link OculusGoController#PROFILE}</li>
         *     <li>{@link OculusTouchController#PROFILE}</li>
         *     <li>{@link ValveIndexController#PROFILE}</li>
         *</ul>
         * @param profile the name of the controller (e.g. {@link OculusTouchController#PROFILE})
         * @param binding the physics binding (e.g. `OculusTouchController.pathBuilder().leftHand().haptic()`)
         */
        @SuppressWarnings("UnusedReturnValue")
        public ActionBuilder withSuggestedBinding(String profile, String binding){
            this.suggestedBindings.add(new SuggestedBinding(profile, binding));
            return this;
        }
        /**
         * Suggested bindings are physical bindings to specific devices for this action.
         * This method can be called multiple times to add more bindings.
         */
        public ActionBuilder withSuggestedBinding(SuggestedBinding suggestedBinding){
            this.suggestedBindings.add(suggestedBinding);
            return this;
        }

        /**
         * Binds all the devices this library knows about (and that have haptics) to this action for both hands.
         * <p>
         * Note that the occulus go does not have haptics so no binding is added for it
         */
        public ActionBuilder withSuggestAllKnownHapticBindings(){
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().haptic());
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().haptic());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().haptic());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().haptic());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().leftHand().haptic());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().rightHand().haptic());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().haptic());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().haptic());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().haptic());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().haptic());
            return this;
        }

        /**
         * Binds all the devices this library knows about (and that have aim poses) to this action for both hands.
         * <p>
         * Represents the position and orientation of the user's hand grip.
         * <p>
         * This pose is typically derived from a hand-held controller's physical design and is generally aligned with the device's handle.
         * In many cases, this pose aligns with the location of the user's hand when they are holding the device naturally.
         * <p>
         * The orientation of this pose is typically as follows:
         * - The X-axis points to the right of the handle.
         * - The Y-axis points up along the handle.
         * - The Z-axis points in the direction opposite to the front of the handle.
         * <p>
         * This pose is commonly used for:
         * - Placing virtual objects in the user's hand.
         * - Representing the user's hand position in the virtual world.
         * <p>
         * Note: The exact position and orientation can vary between devices. Always test with the specific device to ensure it behaves as expected.
         */
        public ActionBuilder withSuggestAllKnownGripPoseBindings(){
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().gripPose());
            withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().gripPose());
            withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().gripPose());
            return this;
        }

        /**
         * Binds all the devices this library knows about (and that have aim poses) to this action for both hands.
         * <p>
         * Represents the position and orientation of the user's aiming direction.
         * <p>
         * This pose is typically derived from a hand-held controller's physical design and is generally aligned with the device's primary pointing direction.
         * In many cases, this pose aligns with the direction the user is pointing the device, like a laser pointer.
         * <p>
         * The orientation of this pose is typically as follows:
         * - The X-axis points to the right of the controller (as viewed from the back of the controller).
         * - The Y-axis points up from the top of the controller.
         * - The Z-axis points in the direction the user is pointing the controller (i.e., from the back of the controller towards the front).
         * <p>
         * This pose is commonly used for:
         * - Directing the user's gaze or aim in the virtual world.
         * - Placing the origin of a raycast for selecting or interacting with virtual objects.
         * <p>
         * Note: The exact position and orientation can vary between devices. Always test with the specific device to ensure it behaves as expected.
         */
        public ActionBuilder withSuggestAllKnownAimPoseBindings(){
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(HtcViveController.PROFILE, HtcViveController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(KhronosSimpleController.PROFILE, KhronosSimpleController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(MixedRealityMotionController.PROFILE, MixedRealityMotionController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(ValveIndexController.PROFILE, ValveIndexController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(OculusGoController.PROFILE, OculusGoController.pathBuilder().rightHand().aimPose());
            withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().leftHand().aimPose());
            withSuggestedBinding(GoogleDaydreamController.PROFILE, GoogleDaydreamController.pathBuilder().rightHand().aimPose());
            return this;
        }

        /**
         * Sub action paths are things like "/user/hand/left", for use when restricting actions to a specific input source.
         * This is defaulted to ["/user/hand/left", "/user/hand/right"] and there is usually no reason to change it.
         */
        public ActionBuilder overrideSupportedSubActionPaths(List<String> supportedSubActionPaths){
            this.supportedSubActionPaths = supportedSubActionPaths;
            return this;
        }

        /**
         * This is only used with the {@link com.onemillionworlds.tamarin.actions.DesktopSimulatingXrActionAppState}.
         * It allows a keyboard button to be bound to this action for debugging purposes (allowing VR like behaviour
         * without having actually plugged in a VR headset).
         * Note this only applies to boolean and float actions. Haptics are N/A and Vector2fs are not supported currently.
         * Poses are handled differently (as the mouse is used to simulate those).
         * @param inputStringGeneratingPress the input string for the controller the keypress is pretending to come from
         *                                  (e.g. "/user/hand/left", available as HandSide.LEFT.restrictToInputString).
         *                                  (even if it "doesn't matter" from a simulation perspective the
         *                                  keypress still "comes from somewhere")
         * @param desktopDebugKeyTrigger the key to bind to this action
         * @param toggle if true, the action will be toggled on and off when the key is pressed, if false, the action will be on while the key is pressed
         */
        public ActionBuilder withDesktopSimulationKeyTrigger(String inputStringGeneratingPress, KeyTrigger desktopDebugKeyTrigger, boolean toggle){
            this.desktopDebugKeyTrigger.put(inputStringGeneratingPress, new DesktopSimulationKeybinding(desktopDebugKeyTrigger, toggle));
            return this;
        }

        /**
         * This is only used with the {@link com.onemillionworlds.tamarin.actions.DesktopSimulatingXrActionAppState}.
         * It allows a keyboard button to be bound to this action for debugging purposes (allowing VR like behaviour
         * without having actually plugged in a VR headset).
         * Note this only applies to boolean and float actions. Haptics are N/A and Vector2fs are not supported currently.
         * Poses are handled differently (as the mouse is used to simulate those).
         * @param handPretendingToCreateAction the hand that this key trigger comes from (even if it "doesn't matter" from a simulation perspective the
         *                       keypress still "comes from somewhere")
         * @param desktopDebugKeyTrigger the key to bind to this action
         * @param toggle if true, the action will be toggled on and off when the key is pressed, if false, the action will be on while the key is pressed
         */
        public ActionBuilder withDesktopSimulationKeyTrigger(HandSide handPretendingToCreateAction, KeyTrigger desktopDebugKeyTrigger, boolean toggle){
            this.desktopDebugKeyTrigger.put(handPretendingToCreateAction.restrictToInputString, new DesktopSimulationKeybinding(desktopDebugKeyTrigger, toggle));
            return this;
        }

        public Action build(){
            if(actionHandle == null){
                throw new IllegalArgumentException("actionHandle cannot be null");
            }
            if(translatedName == null){
                translatedName = actionHandle.actionName();
            }
            if(actionType == null){
                throw new IllegalArgumentException("actionType cannot be null");
            }
            if(suggestedBindings.isEmpty()){
                throw new IllegalArgumentException("suggestedBindings cannot be empty");
            }
            if(actionType != ActionType.BOOLEAN && actionType != ActionType.FLOAT && !desktopDebugKeyTrigger.isEmpty()){
                throw new IllegalArgumentException("desktopDebugKeyTrigger can only be set for boolean and float actions");
            }
            return new Action(actionHandle, translatedName, actionType, suggestedBindings, supportedSubActionPaths, desktopDebugKeyTrigger);
        }

    }


}

package com.onemillionworlds.tamarin.actions;

import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.DesktopSimulationKeybinding;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.logging.Logger;

public class DesktopSimulatingXrActionAppState extends XrActionBaseAppState{

    private static final Logger LOGGER = Logger.getLogger(DesktopSimulatingXrActionAppState.class.getName());

    private final List<Runnable> runAfterActionsSync = new ArrayList<>(0);

    ActionHandle handPoseActionHandle;

    /**
     * Sets the hand position to be in a sensible position based on the head position and direction.
     */
    @Setter
    private boolean simulateHandPose = true;

    private XrBaseAppState xrAppState;

    private List<String> activeActionSets;

    private final ActionManifest manifest;

    /**
     * Map of "restrictToInputString" -> "ActionHandle" -> "BoundActionHandle"
     */
    Map<String, Map<ActionHandle, BoundActionHandle>> keyBindingMap = new HashMap<>();

    public DesktopSimulatingXrActionAppState(ActionManifest manifest, ActionHandle handPoseActionHandle, String startingActionSet){
        this(manifest, handPoseActionHandle, List.of(startingActionSet));
    }


    public DesktopSimulatingXrActionAppState(ActionManifest manifest, ActionHandle handPoseActionHandle, List<String> startingActionSets){
        this.handPoseActionHandle = handPoseActionHandle;
        this.activeActionSets = startingActionSets;
        this.manifest = manifest;
    }

    @Override
    public List<PhysicalBindingInterpretation> getPhysicalBindingForAction(ActionHandle actionHandle){
        return List.of(new PhysicalBindingInterpretation("Simulated/Simulated", Optional.empty(), "Simulated", "Simulated"));
    }

    @Override
    public void setActiveActionSets(List<String> actionSets){
        this.activeActionSets = actionSets;
    }

    @Override
    public void doNotSuppressRepeatedErrors(){
        // doesn't mean anything in this context
    }

    @Override
    public BooleanActionState getBooleanActionState(ActionHandle action, String restrictToInput){
        if(actionIsActive(action)){
            List<BoundActionHandle> availableKeyBindings = new ArrayList<>();
            if( restrictToInput!=null ){
                BoundActionHandle singleResult = keyBindingMap.getOrDefault(restrictToInput, Map.of()).get(action);

                if(singleResult != null){
                    availableKeyBindings.add(singleResult);
                }
            } else{
                for(Map<ActionHandle, BoundActionHandle> actionHandleBoundActionHandleMap : keyBindingMap.values()){
                    BoundActionHandle availableKeyBinding = actionHandleBoundActionHandleMap.get(action);
                    if(availableKeyBinding!=null){
                        availableKeyBindings.add(availableKeyBinding);
                    }
                }
            }

            if(availableKeyBindings.size() == 1){
                BoundActionHandle availableKeyBinding = availableKeyBindings.get(0);
                NonVrKeyBinding keyBinding = availableKeyBinding.keyBinding();
                if(availableKeyBinding.toggleMode()){
                    return new BooleanActionState(keyBinding.getStateAsToggle(), keyBinding.toggleValueHasChangedThisTick());
                }else{
                    return new BooleanActionState(keyBinding.isKeyPressed(), keyBinding.valueHasChangedThisTick());
                }
            } else if (availableKeyBindings.size() > 1){
                //have to munge the results together
                boolean state = false;
                boolean changed = false;

                for(BoundActionHandle actionHandle : availableKeyBindings){
                    NonVrKeyBinding keyBinding = actionHandle.keyBinding();

                    if(actionHandle.toggleMode()){
                        state = state || keyBinding.getStateAsToggle();
                        changed = changed || keyBinding.toggleValueHasChangedThisTick();
                    } else{
                        state = state || keyBinding.isKeyPressed();
                        changed = changed || keyBinding.valueHasChangedThisTick();
                    }
                }

                return new BooleanActionState(state, changed);
            }
        }
        return new BooleanActionState(false, false);
    }

    @Override
    public Optional<PoseActionState> getPose_worldRelative(ActionHandle actionName, HandSide handSide){
        if (actionName.equals(handPoseActionHandle) && simulateHandPose){
            Vector3f cameraPosition = xrAppState.getVrCameraPosition();
            Vector3f feetPosition = xrAppState.getPlayerFeetPosition();
            Vector3f lookDirection = new Vector3f(xrAppState.getVrCameraLookDirection());
            lookDirection.y = 0;
            if(lookDirection.length()!=0){
                lookDirection.normalizeLocal();

                Node bodyAtHandHeight = new Node("bodyAtHandHeight");
                bodyAtHandHeight.lookAt(lookDirection, Vector3f.UNIT_Y);
                bodyAtHandHeight.setLocalTranslation(cameraPosition.x, 0.75f*cameraPosition.y+0.25f*feetPosition.y, cameraPosition.z);

                Node hand = new Node("hand");
                bodyAtHandHeight.attachChild(hand);
                hand.setLocalTranslation((handSide == HandSide.LEFT ? 0.25f: - 0.25f),0,0.25f);

                return Optional.of(new PoseActionState(hand.getWorldTranslation(), bodyAtHandHeight.getWorldRotation()));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PoseActionState> getPose(ActionHandle action, HandSide handSide, boolean stageRelative){
        if(stageRelative){
            LOGGER.warning("Stage relative poses are not supported in this context");
        }

        return getPose_worldRelative(action, handSide).map(pose -> {
            Node observer = xrAppState.getObserver();
            Quaternion observerRotation = observer.getWorldRotation().inverse();

            Vector3f velocity_local = observerRotation.mult(pose.velocity());
            Vector3f angularVelocity_local = observerRotation.mult(pose.angularVelocity());
            Vector3f localPosition = observer.worldToLocal(pose.position(), null);
            Quaternion localRotation = observerRotation.mult(pose.orientation());

            return new PoseActionState(localPosition, localRotation, velocity_local, angularVelocity_local);
        });
    }

    @Override
    public Optional<Map<HandJoint, BonePose>> getSkeleton(ActionHandle poseAction, HandSide handSide){
        return Optional.empty();
    }

    @Override
    public FloatActionState getFloatActionState(ActionHandle action, String restrictToInput){
        BooleanActionState booleanActionState = getBooleanActionState(action, restrictToInput);

        return new FloatActionState(booleanActionState.getState()?1:0, booleanActionState.hasChanged());
    }

    @Override
    public Vector2fActionState getVector2fActionState(ActionHandle action){
        return new Vector2fActionState(0,0, false);
    }

    @Override
    public Vector2fActionState getVector2fActionState(ActionHandle action, String restrictToInput){
        return new Vector2fActionState(0,0, false);
    }

    @Override
    public void triggerHapticAction(ActionHandle actionHandle, float duration, float frequency, float amplitude){

    }

    @Override
    public void triggerHapticAction(ActionHandle action, float duration, float frequency, float amplitude, String restrictToInput){

    }

    @Override
    public boolean isReady(){
        return true;
    }

    @Override
    public void runAfterActionsRegistered(Runnable runnable){
        runnable.run(); // as there is no "action registration" in this context just run immediately
    }

    @Override
    public void runAfterNextActionsSync(Runnable runnable){
        runAfterActionsSync.add(runnable);
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        if (!runAfterActionsSync.isEmpty()){
            runAfterActionsSync.forEach(Runnable::run);
            runAfterActionsSync.clear();
        }
        keyBindingMap.values().stream().flatMap(m -> m.values().stream()).forEach(kb -> kb.keyBinding().update());
    }

    @Override
    protected void initialize(Application application){
        xrAppState = application.getStateManager().getState(XrBaseAppState.ID, XrBaseAppState.class);

        InputManager inputManager = application.getInputManager();

        for(Action action : manifest.getActionSets().stream().flatMap(as -> as.getActions().stream()).toList()){
            for(Map.Entry<String, DesktopSimulationKeybinding> restrictToInputBindingPair: action.getDesktopSimulationKeybinding().entrySet()){
                String controllerString = restrictToInputBindingPair.getKey();
                DesktopSimulationKeybinding keyTrigger = restrictToInputBindingPair.getValue();
                NonVrKeyBinding keyBinding = createKeyBinding(inputManager, keyTrigger.desktopDebugKeyTrigger());
                keyBindingMap.computeIfAbsent(controllerString, k -> new HashMap<>())
                        .put(action.getActionHandle(), new BoundActionHandle(keyBinding, keyTrigger.toggle()));
            }
        }
    }

    @Override
    protected void cleanup(Application application){
        keyBindingMap.values().stream().flatMap(m -> m.values().stream()).forEach(kb -> kb.keyBinding().closeProcedure());
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private boolean actionIsActive(ActionHandle actionHandle){
        return manifest.getActionSets().stream().filter(actionSet ->
            actionSet.getActions().stream().map(Action::getActionHandle).anyMatch(h -> h == actionHandle)
        ).anyMatch(actionSet -> activeActionSets.contains(actionSet.getName()));
    }

    private static NonVrKeyBinding createKeyBinding(InputManager inputManager, KeyTrigger keyInput){

        String mappingName = "Key " + keyInput.getKeyCode();
        inputManager.addMapping(mappingName, keyInput);

        NonVrKeyBinding newKeyBinding = new NonVrKeyBinding(){
            private boolean isKeyPressedLastTick;
            private boolean isKeyPressed;
            private boolean hasBecomeTrueThisTick;
            private boolean hasBecomeFalseThisTick;
            private boolean toggleState;
            private boolean toggleChangedThisTick;
            @Override
            public void onAction(String name, boolean isPressed, float tpf){
                isKeyPressed = isPressed;
            }

            @Override
            public void closeProcedure(){
                inputManager.removeListener(this);
                inputManager.deleteMapping(mappingName);
            }

            @Override
            public boolean isKeyPressed(){
                return isKeyPressed;
            }

            @Override
            public boolean getStateAsToggle(){
                return toggleState;
            }

            @Override
            public void update(){
                if(!isKeyPressedLastTick && isKeyPressed){
                    hasBecomeTrueThisTick = true;
                    toggleState = !toggleState;
                    toggleChangedThisTick = true;
                }else{
                    toggleChangedThisTick = false;
                }
                if(isKeyPressedLastTick && !isKeyPressed){
                    hasBecomeFalseThisTick = true;
                }
                isKeyPressedLastTick = isKeyPressed;
            }

            @Override
            public String toString(){
                return mappingName;
            }

            @Override
            public boolean valueHasChangedThisTick(){
                return hasBecomeFalseThisTick || hasBecomeTrueThisTick;
            }

            @Override
            public boolean toggleValueHasChangedThisTick(){
                return toggleChangedThisTick;
            }
        };

        inputManager.addListener(newKeyBinding, mappingName);

        return newKeyBinding;
    }

    private interface NonVrKeyBinding extends ActionListener{
        boolean isKeyPressed();

        void closeProcedure();

        boolean getStateAsToggle();

        /**
         * Called once per tick to allow things like toggles to trigger
         */
        void update();

        boolean valueHasChangedThisTick();

        boolean toggleValueHasChangedThisTick();
    }

    private record BoundActionHandle(NonVrKeyBinding keyBinding, boolean toggleMode){}

}

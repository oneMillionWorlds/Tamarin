package com.onemillionworlds.tamarin.actions;

import com.jme3.app.Application;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import lombok.Setter;

import java.util.ArrayList;
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

    public DesktopSimulatingXrActionAppState(ActionHandle handPoseActionHandle){
        this.handPoseActionHandle = handPoseActionHandle;
    }

    @Override
    public List<PhysicalBindingInterpretation> getPhysicalBindingForAction(ActionHandle actionHandle){
        return List.of(new PhysicalBindingInterpretation("Simulated/Simulated", Optional.empty(), "Simulated", "Simulated"));
    }

    @Override
    public void setActiveActionSets(List<String> actionSets){
        // doesn't mean anything in this context
    }

    @Override
    public void doNotSuppressRepeatedErrors(){
        // doesn't mean anything in this context
    }

    @Override
    public BooleanActionState getBooleanActionState(ActionHandle action){
        return new BooleanActionState(false, false);
    }

    @Override
    public BooleanActionState getBooleanActionState(ActionHandle action, String restrictToInput){
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
        return new FloatActionState(0, false);
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
    }

    @Override
    protected void initialize(Application application){
        xrAppState = application.getStateManager().getState(XrBaseAppState.ID, XrBaseAppState.class);
    }

    @Override
    protected void cleanup(Application application){

    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }
}

package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.math.RotationalVelocity;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.functions.ClimbSupport;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;
import com.onemillionworlds.tamarin.vrhands.missinghandtracking.SyntheticBonePositions;
import com.simsilica.lemur.event.BasePickState;
import lombok.Getter;
import org.lwjgl.openxr.EXTHandTracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An app state that can control multiple hands (realistically 1 or 2 at once). Once bound to the state the hands will
 * be moved to their position in the world, and their bone positions controlled based on calls to openVr.
 * <p>
 * See <a href="https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XR_EXT_hand_tracking">Hand tracking spec</a>
 */
public class VRHandsAppState extends BaseAppState{
    public static final String ID = "VRHandsAppState";

    OpenXrActionState actionState;

    XrAppState xrAppState;

    Node rootNodeDelegate = new Node();

    List<BoundHand> handControls = new ArrayList<>();

    private AssetManager assetManager;

    private HandSpec pendingHandSpec;

    /**
     * If the player is climbing (i.e. is currently grabbing something which has
     * {@link com.onemillionworlds.tamarin.vrhands.grabbing.ClimbingPointGrabControl}) this is true.
     * <p>
     * Available as convenience data to calling application
     */
    @Getter
    private boolean currentlyClimbing = false;

    /**
     * This constructor allows for bound hands to be created as soon as the state has initialised.
     * <p>
     * You could alternatively call bindHandModel yourself, but that can only be done much later in initialisation
     * and it may be easier to do it this way instead.
     */
    @SuppressWarnings("unused")
    public VRHandsAppState(HandSpec handSpec){
        super(ID);
        pendingHandSpec = handSpec;
    }

    @SuppressWarnings("unused")
    public VRHandsAppState(){
        super(ID);
    }

    @Override
    protected void initialize(Application app){
        this.assetManager = app.getAssetManager();
        actionState = app.getStateManager().getState(OpenXrActionState.class);
        xrAppState = app.getStateManager().getState(XrAppState.class);
        if (actionState == null){
            throw new IllegalStateException("VRHandsAppState requires ActionBasedOpenVr to have already been bound");
        }

        ((SimpleApplication)getApplication()).getRootNode().attachChild(rootNodeDelegate);

        if (pendingHandSpec!=null){
            updateHandsForHandSpec(pendingHandSpec);
            pendingHandSpec = null;
        }
        if (BoundHand.isLemurAvailable()){
            BasePickState basePickState = getStateManager().getState(BasePickState.class);
            if (basePickState != null){
                getStateManager().detach(basePickState);
            }
            getStateManager().attach(new VrLemurAppState());
        }
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
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

        if (isEnabled()){

            List<BoundHand> handControlsWithActiveClimbs = new ArrayList<>();

            for(BoundHand boundHand : handControls){
                Optional<PoseActionState> poseOpt = actionState.getPose_worldRelative(boundHand.getHandPoseActionName(), boundHand.getHandSide());
                poseOpt.ifPresent(pose -> {
                    boundHand.getRawOpenVrNode().setLocalRotation(pose.orientation());
                    boundHand.getRawOpenVrNode().setLocalTranslation(pose.position());
                    boundHand.updateVelocityData(pose.velocity(), new RotationalVelocity(pose.angularVelocity()));
                });

                Optional<Map<HandJoint, BonePose>> boneStancesOpt = getOrSynthesisBonePositions(boundHand);

                boneStancesOpt.ifPresent(boneStances -> {
                    boundHand.update(tpf, boneStances);
                    OpenXrActionState.updateHandSkeletonPositions(boundHand.getArmature(), boneStances, BoneMappings.getBoneMappings(boundHand.getHandSide()));
                    if (boundHand.getFunctionOpt(ClimbSupport.class).filter(cs -> cs.getGrabStartPosition() !=null).isPresent()){
                        handControlsWithActiveClimbs.add(boundHand);
                    }
                });
            }

            if (!handControlsWithActiveClimbs.isEmpty()){
                handleClimbing(handControlsWithActiveClimbs);
                currentlyClimbing = true;
            }else{
                currentlyClimbing = false;
            }

        }
    }

    public Optional<Map<HandJoint, BonePose>> getOrSynthesisBonePositions(BoundHand boundHand){
        if (xrAppState.checkExtensionLoaded(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME)){
            return actionState.getSkeleton(boundHand.getSkeletonActionName(), boundHand.getHandSide());
        }else{
            //real hand tracking is not available, so we need to synthesise it
            float grabStrength = boundHand.getFunctionOpt(GrabPickingFunction.class)
                    .map(GrabPickingFunction::getGrabAction)
                    .map(a -> actionState.getFloatActionState(a, boundHand.getHandSide().restrictToInputString))
                    .map(FloatActionState::getState)
                    .orElse(0f);

            return Optional.of(SyntheticBonePositions.synthesizeBonePositions(boundHand.getHandSide(), grabStrength));
        }
    }

    /**
     * If a teleport or other event happens mid-climb (e.g. ending a level) it can be useful to force
     * end any climb events. This method can be called to do so if necessary.
     * <p>
     * Failing to call this method may mean that the player is pulled back to their previous position by the
     * hand that is still clinging on to a grab point
     */
    @SuppressWarnings("unused")
    public void forceTerminateClimbing(){
        for(BoundHand hand  : handControls){
            hand.getFunctionOpt(ClimbSupport.class).ifPresent(cs -> cs.setGrabStartPosition(null));
        }
    }

    private void handleClimbing(List<BoundHand> handControlsWithActiveClimbs){
        //update for any climbing that might be taking place
        Vector3f climbingErrorSum = new Vector3f();
        for(BoundHand boundHand : handControlsWithActiveClimbs){
            ClimbSupport climbSupport = boundHand.getFunction(ClimbSupport.class);
            Vector3f startGrabPosition = climbSupport.getGrabStartPosition();
            Vector3f currentGrabPosition = boundHand.getHandNode_xPointing().getWorldTranslation();
            climbingErrorSum.addLocal(currentGrabPosition.subtract(startGrabPosition));
        }
        climbingErrorSum.multLocal(1f/handControlsWithActiveClimbs.size());
        Spatial observer = xrAppState.getObserver();
        observer.setLocalTranslation(observer.getLocalTranslation().subtract(climbingErrorSum));
    }

    public List<BoundHand> getHandControls(){
        return Collections.unmodifiableList(handControls);
    }

    @SuppressWarnings("unused")
    public Optional<BoundHand> getHandControl(HandSide handSide ){
        return handControls.stream().filter(h -> h.getHandSide() == handSide).findAny();
    }

    /**
     * Updates both hands simultaneously based on a spec. Really intended to be used via the {@link VRHandsAppState}
     * constructor, but can be used directly as well to swap hands mid-game
     * @param handSpec the handSpec
     */
    public void updateHandsForHandSpec(HandSpec handSpec){
        new ArrayList<>(getHandControls()).forEach(BoundHand::unbindHand);
        AssetManager assetManager = getApplication().getAssetManager();

        Spatial leftModel = assetManager.loadModel(handSpec.leftHandModel);

        BoundHand leftHand = bindHandModel(handSpec.leftHandPoseAction, handSpec.leftHandSkeletonAction, leftModel, HandSide.LEFT);
        handSpec.applyMaterialToLeftHand.accept(leftHand, assetManager);
        handSpec.postBindLeft.accept(leftHand);

        Spatial rightModel = assetManager.loadModel(handSpec.rightHandModel);

        BoundHand rightHand = bindHandModel(handSpec.rightHandPoseAction, handSpec.rightHandSkeletonAction, rightModel, HandSide.RIGHT);
        handSpec.applyMaterialToRightHand.accept(rightHand, assetManager);
        handSpec.postBindRight.accept(rightHand);
    }

    /**
     * This expects to be given a spatial that has an armature and geometry. That geometry then becomes owned by this
     * app state (do not attempt to attach it to a node yourself). It's relatively unfussy about degenerate parent nodes
     * and will search for what it needs (primarily because blender exports can put such stuff in.)
     * <p>
     * The spatial will most likely have been loaded using `assetManager.loadModel`
     * <p>
     * After binding the armature will be deformed each update to conform to the action skeleton it has been bound to.
     * <p>
     * An BoundHandControl object is returned. This can be used to unbind the hand, this cause it to be detached from
     * the node and will not be further updated.
     * <p>
     * Note that the unbind object should only be used on the main thread as it will update the scene graph. Similarly,
     * this method should only be called on the main thread.
     *
     * @param poseToBindTo the pose action name (as found within the action manifest) controls the hands bulk movement
     * @param skeletonActionToBindTo the skeleton action name (as found within the action manifest) controls the hand fine movement
     * @param spatial the geometry of the hand (which must have a skinning control which must have an armature)
     * @param leftOrRight either {@link HandSide#LEFT} or {@link HandSide#RIGHT} to tell tamarin which side hand this is (which tamarin can use to make sure the palms are set up right)
     */
    public BoundHand bindHandModel(ActionHandle poseToBindTo, ActionHandle skeletonActionToBindTo, Spatial spatial, HandSide leftOrRight ){
        if (assetManager == null){
            throw new IllegalStateException("Attempted to bind hands before " + this.getClass().getSimpleName() + " was initialised. Either bind hands later or use a hand spec to ensure binding occurs at the right time");
        }

        Spatial trueModel = searchForArmatured(spatial);

        SkinningControl skinningControl = trueModel.getControl(SkinningControl.class);
        Armature armature = skinningControl.getArmature();

        BoundHand boundHand = new BoundHand(actionState, poseToBindTo, skeletonActionToBindTo, trueModel, armature, assetManager, leftOrRight){
            @Override
            public void unbindHand(){
                trueModel.removeFromParent();
                handControls.remove(this);
            }
        };
        rootNodeDelegate.attachChild(boundHand.getRawOpenVrNode());
        handControls.add(boundHand);

        return boundHand;
    }

    public static Spatial searchForArmatured(Spatial spatial){
        if (spatial.getControl(SkinningControl.class) !=null){
            spatial.removeFromParent();
            return spatial;
        }else if (spatial instanceof Node node){
            if (node.getChildren().size() > 1){
                throw new RuntimeException("Could not find skinnable model due to branched world or no skinning control");
            }
            if (node.getChildren().isEmpty()){
                throw new RuntimeException("Could not find skinnable model due to no model");
            }
            return searchForArmatured(node.getChildren().get(0));
        }else{
            throw new RuntimeException("Could not find skinnable model");
        }
    }

}

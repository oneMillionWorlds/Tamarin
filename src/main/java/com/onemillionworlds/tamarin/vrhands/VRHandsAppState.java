package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.BoneStance;
import com.onemillionworlds.tamarin.compatibility.ObserverRelativePoseActionState;
import com.onemillionworlds.tamarin.compatibility.PoseActionState;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.math.RotationalVelocity;
import com.simsilica.lemur.event.BasePickState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An app state that can control multiple hands (realistically 1 or 2 at once). Once bound to the state the hands will
 * be moved to their position in the world, and their bone positions controlled based on calls to openVr.
 */
public class VRHandsAppState extends BaseAppState{

    ActionBasedOpenVrState openVr;

    VRAppState vrAppState;

    Node rootNodeDelegate = new Node();

    List<BoundHand> handControls = new ArrayList<>();

    private AssetManager assetManager;

    private HandSpec pendingHandSpec;

    /**
     * This constructor allows for bound hands to be created as soon as the state has initialised.
     *
     * You could alternatively call bindHandModel yourself, but that can only be done much later in initialisation
     * and it may be easier to do it this way instead.
     */
    public VRHandsAppState(HandSpec handSpec){
        pendingHandSpec = handSpec;
    }

    public VRHandsAppState(){
    }

    @Override
    protected void initialize(Application app){


        this.assetManager = app.getAssetManager();
        openVr = app.getStateManager().getState(ActionBasedOpenVrState.class);
        vrAppState = app.getStateManager().getState(VRAppState.class);
        if (openVr == null){
            throw new IllegalStateException("VRHandsAppState requires ActionBasedOpenVr to have already been bound");
        }

        validateState();

        ((Node)vrAppState.getObserver()).attachChild(rootNodeDelegate);

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

            for(BoundHand boundHand : handControls){
                ObserverRelativePoseActionState pose = openVr.getPose_observerRelative(boundHand.getPostActionName());
                boundHand.getRawOpenVrNode().setLocalRotation(pose.getOrientation());
                boundHand.getRawOpenVrNode().setLocalTranslation(pose.getPosition());
                boundHand.updateVelocityData(pose.getPoseActionState_worldRelative().getVelocity(), new RotationalVelocity(pose.getPoseActionState_worldRelative().getAngularVelocity()));
                Map<String, BoneStance> boneStances = openVr.getModelRelativeSkeletonPositions(boundHand.getSkeletonActionName());

                boundHand.update(tpf, boneStances);
                openVr.updateHandSkeletonPositions(boundHand.getSkeletonActionName(), boundHand.getArmature(), boundHand.getHandMode());
            }
        }
    }

    public List<BoundHand> getHandControls(){
        return Collections.unmodifiableList(handControls);

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
     *
     * The spatial will most likely have been loaded using `assetManager.loadModel`
     *
     * After binding the armature will be deformed each update to conform to the action skeleton it has been bound to.
     *
     * An BoundHandControl object is returned. This can be used to unbind the hand, this cause it to be detached from
     * the node and will not be further updated.
     *
     * Note that the unbind object should only be used on the main thread as it will update the scene graph. Similarly,
     * this method should only be called on the main thread.
     *
     * @param poseToBindTo the pose action name (as found within the action manifest) controls the hands bulk movement
     * @param skeletonActionToBindTo the skeleton action name (as found within the action manifest) controls the hand fine movement
     * @param spatial the geometry of the hand (which must have a skinning control which must have an armature)
     * @param leftOrRight either {@link HandSide#LEFT} or {@link HandSide#RIGHT} to tell tamarin which side hand this is (which tamarin can use to make sure the palms are set up right)
     */
    public BoundHand bindHandModel( String poseToBindTo, String skeletonActionToBindTo, Spatial spatial, HandSide leftOrRight ){
        if (assetManager == null){
            throw new IllegalStateException("Attempted to bind hands before " + this.getClass().getSimpleName() + " was initialised. Either bind hands later or use a hand spec to ensure binding occurs at the right time");
        }

        Spatial trueModel = searchForArmatured(spatial);

        SkinningControl skinningControl = trueModel.getControl(SkinningControl.class);
        Armature armature = skinningControl.getArmature();

        BoundHand boundHand = new BoundHand(openVr, poseToBindTo, skeletonActionToBindTo, trueModel, armature, assetManager, leftOrRight){
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

    private void validateState(){
        if (!(vrAppState.getObserver() instanceof Node)){
            throw new IllegalStateException("Tamarin 1.2 onwards requires that the observer be a Node. Please call vrAppState#setObserver and give it a node that is connected to the root node");
        }
    }

    private static Spatial searchForArmatured(Spatial spatial){
        if (spatial.getControl(SkinningControl.class) !=null){
            spatial.removeFromParent();
            return spatial;
        }else if (spatial instanceof Node){
            Node node = (Node)spatial;
            if (node.getChildren().size() > 1){
                throw new RuntimeException("Could not find skinnable model due to branched world");
            }
            if (node.getChildren().size() == 0){
                throw new RuntimeException("Could not find skinnable model due to no more model");
            }
            return searchForArmatured(node.getChildren().get(0));
        }else{
            throw new RuntimeException("Could not find skinnable model");
        }
    }

}

package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.BoneStance;
import com.onemillionworlds.tamarin.compatibility.PoseActionState;

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

    Node rootNodeDelegate = new Node();

    List<BoundHand> handControls = new ArrayList<>();

    private AssetManager assetManager;

    /**
     * This constructor allows {@link VRHandsAppState#bindHandModel} to be called immediately (before the state has been
     * initialised. This is optional, but may be helpful if you want to set everything up within {@link SimpleApplication#simpleInitApp()}
     * @param assetManager the assetManager
     */
    public VRHandsAppState(AssetManager assetManager, ActionBasedOpenVrState actionBasedOpenVrState){
        this.assetManager = assetManager;
        openVr = actionBasedOpenVrState;
    }

    public VRHandsAppState(){
        super();
    }

    @Override
    protected void initialize(Application app){
        this.assetManager = app.getAssetManager();
        openVr = app.getStateManager().getState(ActionBasedOpenVrState.class);
        if (openVr == null){
            throw new IllegalStateException("VRHandsAppState requires ActionBasedOpenVr to have already been bound");
        }
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
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
                PoseActionState pose = openVr.getPose(boundHand.getPostActionName());
                boundHand.getRawOpenVrNode().setLocalRotation(pose.getOrientation());
                boundHand.getRawOpenVrNode().setLocalTranslation(pose.getPosition());
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
     * This expects to be given a spatial that has an armature and geometry. That geometry then becomes owned by this
     * app state (do not attempt to attach it to a node yourself). Its relatively unfussy about degenerate parent nodes
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
            throw new IllegalStateException("Attempted to bind hands before " + this.getClass().getSimpleName() + " was initialised. Either bind hands later or use the constructor that takes an assetManager to remove this requirement");
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

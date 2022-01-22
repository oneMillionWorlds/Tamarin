package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVr;
import com.onemillionworlds.tamarin.compatibility.PoseActionState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An app state that can control multiple hands (realistically 1 or 2 at once). Once bound to the state the hands will
 * be moved to their position in the world, and their bone positions controlled based on calls to openVr.
 */
public class VRHandsAppState extends BaseAppState{

    ActionBasedOpenVr openVr;

    Node rootNodeDelegate = new Node();

    List<BoundHand> handControls = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        openVr = app.getStateManager().getState(ActionBasedOpenVr.class);
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
                boundHand.getHandGeometry().setLocalRotation(pose.getOrientation());
                boundHand.getHandGeometry().setLocalTranslation(pose.getPosition());
                openVr.updateHandSkeletonPositions(boundHand.getSkeletonActionName(), boundHand.getArmature(), boundHand.getHandMode());
            }
        }
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
     */
    public BoundHand bindHandModel( String poseToBindTo, String skeletonActionToBindTo, Spatial spatial ){
        Spatial trueModel = searchForArmatured(spatial);

        rootNodeDelegate.attachChild(trueModel);

        SkinningControl skinningControl = trueModel.getControl(SkinningControl.class);
        Armature armature = skinningControl.getArmature();

        BoundHand boundHand = new BoundHand(poseToBindTo, skeletonActionToBindTo, trueModel, armature){
            @Override
            public void unbindHand(){
                trueModel.removeFromParent();
                handControls.remove(this);
            }
        };

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

    private static Collection<Geometry> searchForGeometry(Spatial spatial){
        if (spatial instanceof Geometry){
            return List.of((Geometry)spatial);
        }else if (spatial instanceof Node){
            List<Geometry> geometries = new ArrayList<>();
            for(Spatial child : ((Node)spatial).getChildren()){
                geometries.addAll(searchForGeometry(child));
            }

            return geometries;
        }else{
            throw new RuntimeException("Could not find skinable model");
        }
    }

}

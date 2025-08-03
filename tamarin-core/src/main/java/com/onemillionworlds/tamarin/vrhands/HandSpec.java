package com.onemillionworlds.tamarin.vrhands;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hand spec defines the hand model and material used.
 */
@SuppressWarnings("unused")
public class HandSpec{

    ActionHandle leftHandPoseAction;

    ActionHandle leftHandSkeletonAction;

    ActionHandle rightHandPoseAction;

    ActionHandle rightHandSkeletonAction;

    /**
     * An asset location for the left hand model. Ideally should be a j3o
     */
    String leftHandModel = BoundHand.DEFAULT_HAND_MODEL_LEFT;

    /**
     * An asset location for the right hand model. Ideally should be a j3o
     */
    String rightHandModel = BoundHand.DEFAULT_HAND_MODEL_RIGHT;

    /**
     * After the left hand has been created and bound this is run to set a material to it
     */
    BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand;

    /**
     * After the right hand has been created and bound this is run to set a material to it
     */
    BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand;

    /**
     * After the left hand has been created and bound this is run (used to set up grip actions or similar)
     */
    Consumer<BoundHand> postBindLeft = (hand) -> {
    };

    /**
     * After the right hand has been created an bound this is run (used to set up grip actions or similar)
     */
    Consumer<BoundHand> postBindRight = (hand) -> {
    };

    HandSpec(ActionHandle leftHandPoseAction, ActionHandle leftHandSkeletonAction, ActionHandle rightHandPoseAction, ActionHandle rightHandSkeletonAction, String leftHandModel, String rightHandModel, BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand, BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand, Consumer<BoundHand> postBindLeft, Consumer<BoundHand> postBindRight){
        this.leftHandPoseAction = leftHandPoseAction;
        this.leftHandSkeletonAction = leftHandSkeletonAction;
        this.rightHandPoseAction = rightHandPoseAction;
        this.rightHandSkeletonAction = rightHandSkeletonAction;
        this.leftHandModel = leftHandModel;
        this.rightHandModel = rightHandModel;
        this.applyMaterialToLeftHand = applyMaterialToLeftHand;
        this.applyMaterialToRightHand = applyMaterialToRightHand;
        this.postBindLeft = postBindLeft;
        this.postBindRight = postBindRight;
    }

    public HandSpecBuilder toBuilder(){
        return new HandSpecBuilder()
                .leftHandPoseAction(this.leftHandPoseAction)
                .leftHandSkeletonAction(this.leftHandSkeletonAction)
                .rightHandPoseAction(this.rightHandPoseAction)
                .rightHandSkeletonAction(this.rightHandSkeletonAction)
                .leftHandModel(this.leftHandModel)
                .rightHandModel(this.rightHandModel)
                .applyMaterialToLeftHand(this.applyMaterialToLeftHand)
                .applyMaterialToRightHand(this.applyMaterialToRightHand)
                .postBindLeft(this.postBindLeft)
                .postBindRight(this.postBindRight);
    }

    /**
     * Creates a builder for hand specs, only the 2 arguments to this method are required, the rest will be defaulted
     * if not entered.
     * <p>
     * See by "action" the openXR Action Manifest meaning of action is meant.
     *
     * @param leftHandPoseAction  the action defining the position and rotation of the left hand. The skeleton is assumed also in this space
     * @param rightHandPoseAction the action defining the position and rotation of the right hand. The skeleton is assumed also in this space
     * @return a builder
     */
    public static HandSpec.HandSpecBuilder builder(ActionHandle leftHandPoseAction, ActionHandle rightHandPoseAction){
        return builder(leftHandPoseAction, leftHandPoseAction, rightHandPoseAction, rightHandPoseAction);
    }

    /**
     * Creates a builder for hand specs, only the 4 arguments to this method are required, the rest will be defaulted
     * if not entered.
     * <p>
     * See by "action" the openVr Action Manifest meaning of action is meant.
     *
     * @param leftHandPoseAction      the action defining the position and rotation of the left hand
     * @param leftHandSkeletonAction  the action defining the positions and rotations of the left hand bones
     * @param rightHandPoseAction     the action defining the position and rotation of the right hand
     * @param rightHandSkeletonAction the action defining the positions and rotations of the right hand bones
     * @return a builder
     */
    public static HandSpec.HandSpecBuilder builder(ActionHandle leftHandPoseAction, ActionHandle leftHandSkeletonAction, ActionHandle rightHandPoseAction, ActionHandle rightHandSkeletonAction){

        return new HandSpec.HandSpecBuilder()
                .leftHandPoseAction(leftHandPoseAction)
                .leftHandSkeletonAction(leftHandSkeletonAction)
                .rightHandPoseAction(rightHandPoseAction)
                .rightHandSkeletonAction(rightHandSkeletonAction);
    }

    private static String $default$leftHandModel(){
        return BoundHand.DEFAULT_HAND_MODEL_LEFT;
    }

    private static String $default$rightHandModel(){
        return BoundHand.DEFAULT_HAND_MODEL_RIGHT;
    }

    private static BiConsumer<BoundHand, AssetManager> $default$applyMaterialToLeftHand(){
        return (hand, assetManager) -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", assetManager.loadTexture(BoundHand.DEFAULT_HAND_TEXTURE));
            hand.setMaterial(mat);
        };
    }

    private static BiConsumer<BoundHand, AssetManager> $default$applyMaterialToRightHand(){
        return (hand, assetManager) -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", assetManager.loadTexture(BoundHand.DEFAULT_HAND_TEXTURE));
            hand.setMaterial(mat);
        };
    }

    private static Consumer<BoundHand> $default$postBindLeft(){
        return (hand) -> {
        };
    }

    private static Consumer<BoundHand> $default$postBindRight(){
        return (hand) -> {
        };
    }

    public static class HandSpecBuilder{
        private ActionHandle leftHandPoseAction;
        private ActionHandle leftHandSkeletonAction;
        private ActionHandle rightHandPoseAction;
        private ActionHandle rightHandSkeletonAction;
        private String leftHandModel$value;
        private boolean leftHandModel$set;
        private String rightHandModel$value;
        private boolean rightHandModel$set;
        private BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand$value;
        private boolean applyMaterialToLeftHand$set;
        private BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand$value;
        private boolean applyMaterialToRightHand$set;
        private Consumer<BoundHand> postBindLeft$value;
        private boolean postBindLeft$set;
        private Consumer<BoundHand> postBindRight$value;
        private boolean postBindRight$set;

        HandSpecBuilder(){
        }

        public HandSpecBuilder leftHandPoseAction(ActionHandle leftHandPoseAction){
            this.leftHandPoseAction = leftHandPoseAction;
            return this;
        }

        public HandSpecBuilder leftHandSkeletonAction(ActionHandle leftHandSkeletonAction){
            this.leftHandSkeletonAction = leftHandSkeletonAction;
            return this;
        }

        public HandSpecBuilder rightHandPoseAction(ActionHandle rightHandPoseAction){
            this.rightHandPoseAction = rightHandPoseAction;
            return this;
        }

        public HandSpecBuilder rightHandSkeletonAction(ActionHandle rightHandSkeletonAction){
            this.rightHandSkeletonAction = rightHandSkeletonAction;
            return this;
        }

        public HandSpecBuilder leftHandModel(String leftHandModel){
            this.leftHandModel$value = leftHandModel;
            this.leftHandModel$set = true;
            return this;
        }

        public HandSpecBuilder rightHandModel(String rightHandModel){
            this.rightHandModel$value = rightHandModel;
            this.rightHandModel$set = true;
            return this;
        }

        public HandSpecBuilder applyMaterialToLeftHand(BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand){
            this.applyMaterialToLeftHand$value = applyMaterialToLeftHand;
            this.applyMaterialToLeftHand$set = true;
            return this;
        }

        public HandSpecBuilder applyMaterialToRightHand(BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand){
            this.applyMaterialToRightHand$value = applyMaterialToRightHand;
            this.applyMaterialToRightHand$set = true;
            return this;
        }

        public HandSpecBuilder postBindLeft(Consumer<BoundHand> postBindLeft){
            this.postBindLeft$value = postBindLeft;
            this.postBindLeft$set = true;
            return this;
        }

        public HandSpecBuilder postBindRight(Consumer<BoundHand> postBindRight){
            this.postBindRight$value = postBindRight;
            this.postBindRight$set = true;
            return this;
        }

        public HandSpec build(){
            String leftHandModel$value = this.leftHandModel$value;
            if(!this.leftHandModel$set){
                leftHandModel$value = HandSpec.$default$leftHandModel();
            }
            String rightHandModel$value = this.rightHandModel$value;
            if(!this.rightHandModel$set){
                rightHandModel$value = HandSpec.$default$rightHandModel();
            }
            BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand$value = this.applyMaterialToLeftHand$value;
            if(!this.applyMaterialToLeftHand$set){
                applyMaterialToLeftHand$value = HandSpec.$default$applyMaterialToLeftHand();
            }
            BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand$value = this.applyMaterialToRightHand$value;
            if(!this.applyMaterialToRightHand$set){
                applyMaterialToRightHand$value = HandSpec.$default$applyMaterialToRightHand();
            }
            Consumer<BoundHand> postBindLeft$value = this.postBindLeft$value;
            if(!this.postBindLeft$set){
                postBindLeft$value = HandSpec.$default$postBindLeft();
            }
            Consumer<BoundHand> postBindRight$value = this.postBindRight$value;
            if(!this.postBindRight$set){
                postBindRight$value = HandSpec.$default$postBindRight();
            }
            return new HandSpec(this.leftHandPoseAction, this.leftHandSkeletonAction, this.rightHandPoseAction, this.rightHandSkeletonAction, leftHandModel$value, rightHandModel$value, applyMaterialToLeftHand$value, applyMaterialToRightHand$value, postBindLeft$value, postBindRight$value);
        }

        public String toString(){
            return "HandSpec.HandSpecBuilder(leftHandPoseAction=" + this.leftHandPoseAction + ", leftHandSkeletonAction=" + this.leftHandSkeletonAction + ", rightHandPoseAction=" + this.rightHandPoseAction + ", rightHandSkeletonAction=" + this.rightHandSkeletonAction + ", leftHandModel$value=" + this.leftHandModel$value + ", leftHandModel$set=" + this.leftHandModel$set + ", rightHandModel$value=" + this.rightHandModel$value + ", rightHandModel$set=" + this.rightHandModel$set + ", applyMaterialToLeftHand$value=" + this.applyMaterialToLeftHand$value + ", applyMaterialToLeftHand$set=" + this.applyMaterialToLeftHand$set + ", applyMaterialToRightHand$value=" + this.applyMaterialToRightHand$value + ", applyMaterialToRightHand$set=" + this.applyMaterialToRightHand$set + ", postBindLeft$value=" + this.postBindLeft$value + ", postBindLeft$set=" + this.postBindLeft$set + ", postBindRight$value=" + this.postBindRight$value + ", postBindRight$set=" + this.postBindRight$set + ", leftHandModel$value=" + this.leftHandModel$value + ", rightHandModel$value=" + this.rightHandModel$value + ", applyMaterialToLeftHand$value=" + this.applyMaterialToLeftHand$value + ", applyMaterialToRightHand$value=" + this.applyMaterialToRightHand$value + ", postBindLeft$value=" + this.postBindLeft$value + ", postBindRight$value=" + this.postBindRight$value + ")";
        }
        // finish me
    }
}

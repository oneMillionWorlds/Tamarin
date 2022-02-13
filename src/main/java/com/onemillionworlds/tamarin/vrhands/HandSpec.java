package com.onemillionworlds.tamarin.vrhands;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import lombok.AccessLevel;
import lombok.Builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hand spec defines the hand model and material used.
 */
@Builder(toBuilder = true)
public class HandSpec{

    String leftHandPoseAction;

    String leftHandSkeletonAction;

    String rightHandPoseAction;

    String rightHandSkeletonAction;

    /**
     * An asset location for the left hand model. Ideally should be a j3o
     */
    @Builder.Default
    String leftHandModel = "Tamarin/Models/basicHands_left.j3o";

    /**
     * An asset location for the right hand model. Ideally should be a j3o
     */
    @Builder.Default
    String rightHandModel = "Tamarin/Models/basicHands_right.j3o";

    /**
     * After the left hand has been created and bound this is run to set a material to it
     */
    @Builder.Default
    BiConsumer<BoundHand, AssetManager> applyMaterialToLeftHand = (hand, assetManager) -> {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Tamarin/Textures/basicHands_left_referenceTexture.png"));
        hand.setMaterial(mat);
    };

    /**
     * After the right hand has been created and bound this is run to set a material to it
     */
    @Builder.Default
    BiConsumer<BoundHand, AssetManager> applyMaterialToRightHand = (hand, assetManager) -> {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Tamarin/Textures/basicHands_right_referenceTexture.png"));
        hand.setMaterial(mat);
    };

    /**
     * After the left hand has been created and bound this is run (used to set up grip actions or similar)
     */
    @Builder.Default
    Consumer<BoundHand> postBindLeft = (hand) -> {};

    /**
     * After the right hand has been created an bound this is run (used to set up grip actions or similar)
     */
    @Builder.Default
    Consumer<BoundHand> postBindRight = (hand) -> {};

    /**
     * Creates a builder for hand specs, only the 4 arguments to this method are required, the rest will be defaulted
     * if not entered.
     *
     * See by "action" the openVr Action Manifest meaning of action is meant.
     * @param leftHandPoseAction the action defining the position and rotation of the left hand
     * @param leftHandSkeletonAction the action defining the positions and rotations of the left hand bones
     * @param rightHandPoseAction the action defining the position and rotation of the right hand
     * @param rightHandSkeletonAction the action defining the positions and rotations of the right hand bones
     * @return a builder
     */
    public static HandSpec.HandSpecBuilder builder( String leftHandPoseAction, String leftHandSkeletonAction, String rightHandPoseAction, String rightHandSkeletonAction){

        return new HandSpec.HandSpecBuilder()
                .leftHandPoseAction(leftHandPoseAction)
                .leftHandSkeletonAction(leftHandSkeletonAction)
                .rightHandPoseAction(rightHandPoseAction)
                .rightHandSkeletonAction(rightHandSkeletonAction);
    }
}

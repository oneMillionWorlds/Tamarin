package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.compatibility.HandMode;

public abstract class BoundHand{

    private HandMode handMode = HandMode.WITHOUT_CONTROLLER;

    private final Spatial handGeometry;

    /**
     * The hand will be detached from the scene graph and will no longer receive updates
     */
    public abstract void unbindHand();

    private final String postActionName;

    private final String skeletonActionName;

    private final Armature armature;

    public BoundHand(String postActionName, String skeletonActionName, Spatial handGeometry, Armature armature){
        this.handGeometry = handGeometry;
        this.postActionName = postActionName;
        this.skeletonActionName = skeletonActionName;
        this.armature = armature;
    }

    public HandMode getHandMode(){
        return handMode;
    }

    public Spatial getHandGeometry(){
        return handGeometry;
    }

    /**
     * The way the hand is being held, see javadoc on HandMode itself for more details
     * @param handMode the handMode
     */
    public void setHandMode(HandMode handMode){
        this.handMode = handMode;
    }

    public String getPostActionName(){
        return postActionName;
    }

    public String getSkeletonActionName(){
        return skeletonActionName;
    }

    public Armature getArmature(){
        return armature;
    }
}

package com.onemillionworlds.tamarin.compatibility;

class LWJGLSkeletonData{

    /**
     * The skeleton must be bound to an action, this string is the name of that action in the action manifest
     */
    String skeletonActionName;

    /**
     * The skeleton must be bound to an action, this long is the handle to that action
     */
    long skeletonAction;

    /**
     * This is for producing the bone state map, there are a number of bones (probably 31, see https://github.com/ValveSoftware/openvr/wiki/Hand-Skeleton)
     * with human-readable names. These are those names
     */
    String[] boneNames;

    public LWJGLSkeletonData(String skeletonActionName, long skeletonAction, String[] boneNames){
        this.skeletonActionName = skeletonActionName;
        this.skeletonAction = skeletonAction;
        this.boneNames = boneNames;
    }
}

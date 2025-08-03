package com.onemillionworlds.tamarin.actions.controllerprofile;

/**
 * This is not a real controller, it is a basic profile that provides basic pose, button, and haptic support for
 * applications with simple input needs.
 */
public class KhronosSimpleController {
    public static final String PROFILE = "/interaction_profiles/khr/simple_controller";

    public static class InteractionProfiles {
        public static final String LEFT_HAND = "/user/hand/left";
        public static final String RIGHT_HAND = "/user/hand/right";
    }

    public static class ComponentPaths {
        public static final String SELECT_CLICK = "/input/select/click";
        public static final String MENU_CLICK = "/input/menu/click";
        public static final String GRIP_POSE = "/input/grip/pose";
        public static final String AIM_POSE = "/input/aim/pose";
        public static final String HAPTIC = "/output/haptic";
    }

    public static BindingPathBuilder pathBuilder() {
        return new BindingPathBuilder();
    }

    public static class BindingPathBuilder {
        public BindingPathBuilderHand leftHand(){
            return new BindingPathBuilderHand(InteractionProfiles.LEFT_HAND);
        }
        public BindingPathBuilderHand rightHand(){
            return new BindingPathBuilderHand(InteractionProfiles.RIGHT_HAND);
        }
    }

    public static class BindingPathBuilderHand {
        String handPart;

        public BindingPathBuilderHand(String handPart){
            this.handPart = handPart;
        }

        public String selectClick(){
            return handPart + ComponentPaths.SELECT_CLICK;
        }
        public String menuClick(){
            return handPart + ComponentPaths.MENU_CLICK;
        }
        public String gripPose(){
            return handPart + ComponentPaths.GRIP_POSE;
        }
        public String aimPose(){
            return handPart + ComponentPaths.AIM_POSE;
        }
        public String haptic(){
            return handPart + ComponentPaths.HAPTIC;
        }
    }
}

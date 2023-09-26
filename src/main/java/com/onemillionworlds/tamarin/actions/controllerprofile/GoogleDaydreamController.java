package com.onemillionworlds.tamarin.actions.controllerprofile;

public class GoogleDaydreamController {
    public static final String PROFILE = "/interaction_profiles/google/daydream_controller";

    public static class InteractionProfiles {
        public static final String LEFT_HAND = "/user/hand/left";
        public static final String RIGHT_HAND = "/user/hand/right";
    }

    public static class ComponentPaths {
        public static final String SELECT_CLICK = "/input/select/click";

        public static final String TRACKPAD = "/input/trackpad";
        public static final String TRACKPAD_X = "/input/trackpad/x";
        public static final String TRACKPAD_Y = "/input/trackpad/y";
        public static final String TRACKPAD_CLICK = "/input/trackpad/click";
        public static final String TRACKPAD_TOUCH = "/input/trackpad/touch";

        public static final String TRACKPAD_DPAD_UP = "/input/trackpad/dpad_up";
        public static final String TRACKPAD_DPAD_DOWN = "/input/trackpad/dpad_down";
        public static final String TRACKPAD_DPAD_LEFT = "/input/trackpad/dpad_left";
        public static final String TRACKPAD_DPAD_RIGHT = "/input/trackpad/dpad_right";
        public static final String TRACKPAD_DPAD_CENTER = "/input/trackpad/dpad_center";

        public static final String GRIP_POSE = "/input/grip/pose";
        public static final String AIM_POSE = "/input/aim/pose";
    }

    public static BindingPathBuilder pathBuilder(){
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

    @SuppressWarnings("unused")
    public static class BindingPathBuilderHand {
        String handPart;

        public BindingPathBuilderHand(String handPart){
            this.handPart = handPart;
        }

        public String selectClick(){
            return handPart + ComponentPaths.SELECT_CLICK;
        }

        public String trackpad(){
            return handPart + ComponentPaths.TRACKPAD;
        }
        public String trackpadX(){
            return handPart + ComponentPaths.TRACKPAD_X;
        }
        public String trackpadY(){
            return handPart + ComponentPaths.TRACKPAD_Y;
        }
        public String trackpadClick(){
            return handPart + ComponentPaths.TRACKPAD_CLICK;
        }
        public String trackpadTouch(){
            return handPart + ComponentPaths.TRACKPAD_TOUCH;
        }

//these  can be used once JME upgrades to LWJGL 3.3.3 or higher
//        /**
//         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String trackpadDpadUp(){
//            return handPart + ComponentPaths.TRACKPAD_DPAD_UP;
//        }
//        /**
//         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String trackpadDpadDown(){
//            return handPart + ComponentPaths.TRACKPAD_DPAD_DOWN;
//        }
//        /**
//         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String trackpadDpadLeft(){
//            return handPart + ComponentPaths.TRACKPAD_DPAD_LEFT;
//        }
//        /**
//         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String trackpadDpadRight(){
//            return handPart + ComponentPaths.TRACKPAD_DPAD_RIGHT;
//        }
//        /**
//         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String trackpadDpadCenter(){
//            return handPart + ComponentPaths.TRACKPAD_DPAD_CENTER;
//        }

        public String gripPose(){
            return handPart + ComponentPaths.GRIP_POSE;
        }
        public String aimPose(){
            return handPart + ComponentPaths.AIM_POSE;
        }
    }
}
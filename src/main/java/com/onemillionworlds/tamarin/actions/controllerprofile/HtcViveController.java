package com.onemillionworlds.tamarin.actions.controllerprofile;

public class HtcViveController {
    public static final String PROFILE = "/interaction_profiles/htc/vive_controller";

    public static class InteractionProfiles {
        public static final String LEFT_HAND = "/user/hand/left";
        public static final String RIGHT_HAND = "/user/hand/right";
    }

    public static class ComponentPaths {
        /**
         * May not be available for application use
         */
        public static final String SYSTEM_CLICK = "/input/system/click";
        public static final String SQUEEZE_CLICK = "/input/squeeze/click";
        public static final String MENU_CLICK = "/input/menu/click";
        public static final String TRIGGER_CLICK = "/input/trigger/click";
        public static final String TRIGGER_VALUE = "/input/trigger/value";

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
        public static final String HAPTIC = "/output/haptic";
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

        /**
         * May not be available for application use
         */
        public String systemClick(){
            return handPart + ComponentPaths.SYSTEM_CLICK;
        }
        public String squeezeClick(){
            return handPart + ComponentPaths.SQUEEZE_CLICK;
        }
        public String menuClick(){
            return handPart + ComponentPaths.MENU_CLICK;
        }
        public String triggerClick(){
            return handPart + ComponentPaths.TRIGGER_CLICK;
        }
        public String triggerValue(){
            return handPart + ComponentPaths.TRIGGER_VALUE;
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


        /**
         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding (which may or may not be available)
         */
        public String trackpadDpadUp(){
            return handPart + ComponentPaths.TRACKPAD_DPAD_UP;
        }
        /**
         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding (which may or may not be available)
         */
        public String trackpadDpadDown(){
            return handPart + ComponentPaths.TRACKPAD_DPAD_DOWN;
        }
        /**
         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding (which may or may not be available)
         */
        public String trackpadDpadLeft(){
            return handPart + ComponentPaths.TRACKPAD_DPAD_LEFT;
        }
        /**
         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding (which may or may not be available)
         */
        public String trackpadDpadRight(){
            return handPart + ComponentPaths.TRACKPAD_DPAD_RIGHT;
        }
        /**
         * Treats the trackpad as a dpad. This is provided by the extension XR_EXT_dpad_binding (which may or may not be available)
         */
        public String trackpadDpadCenter(){
            return handPart + ComponentPaths.TRACKPAD_DPAD_CENTER;
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
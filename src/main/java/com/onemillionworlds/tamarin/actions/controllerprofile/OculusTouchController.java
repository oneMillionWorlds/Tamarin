package com.onemillionworlds.tamarin.actions.controllerprofile;

public class OculusTouchController{
    public static final String PROFILE = "/interaction_profiles/oculus/touch_controller";

    public static class InteractionProfiles{
        public static final String LEFT_HAND = "/user/hand/left";

        public static final String RIGHT_HAND ="/user/hand/right";
    }

    public static class ComponentPathsLeft{
        public static final String X_CLICK ="/input/x/click";
        public static final String X_TOUCH ="/input/x/touch";
        public static final String Y_CLICK ="/input/y/click";
        public static final String Y_TOUCH ="/input/y/touch";
        public static final String MENU_CLICK ="/input/menu/click";
    }
    public static class ComponentPathsRight{
        public static final String A_CLICK ="/input/a/click";
        public static final String A_TOUCH ="/input/a/touch";
        public static final String B_CLICK ="/input/b/click";
        public static final String B_TOUCH ="/input/b/touch";
        /**
         * May not be available for application use
         */
        public static final String SYSTEM_CLICK ="/input/system/click";
    }
    public static class ComponentPathsEitherHand{
        public static final String SQUEEZE ="/input/squeeze/value";
        public static final String TRIGGER_VALUE ="/input/trigger/value";
        public static final String TRIGGER_TOUCH ="/input/trigger/touch";

        public static final String THUMB_STICK ="/input/thumbstick";
        public static final String THUMB_STICK_X ="/input/thumbstick/x";
        public static final String THUMB_STICK_Y ="/input/thumbstick/y";
        public static final String THUMB_STICK_CLICK ="/input/thumbstick/click";
        public static final String THUMB_STICK_TOUCH ="/input/thumbstick/touch";

        /**
         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
         */
        public static final String THUMB_DPAD_UP ="/input/thumbstick/dpad_up";

        /**
         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
         */
        public static final String THUMB_DPAD_DOWN ="/input/thumbstick/dpad_down";

        /**
         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
         */
        public static final String THUMB_DPAD_LEFT ="/input/thumbstick/dpad_left";

        /**
         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
         */
        public static final String THUMB_DPAD_RIGHT ="/input/thumbstick/dpad_right";

        public static final String THUMB_BREST_TOUCH ="/input/thumbrest/touch";

        public static final String GRIP_POSE ="/input/grip/pose";
        public static final String AIM_POSE ="/input/aim/pose";
        public static final String HAPTIC ="/output/haptic";
    }

    public static BindingPathBuilder pathBuilder(){
        return new BindingPathBuilder();
    }

    public static class BindingPathBuilder{
        public BindingPathBuilderLeftHand leftHand(){
            return new BindingPathBuilderLeftHand();
        }
        public BindingPathBuilderRightHand rightHand(){
            return new BindingPathBuilderRightHand();
        }
    }

    @SuppressWarnings("unused")
    public static class BindingPathBuilderLeftHand extends BindingPathBuilderCommon{
        public BindingPathBuilderLeftHand(){
            super(InteractionProfiles.LEFT_HAND);
        }
        public String xClick(){
            return handPart + ComponentPathsLeft.X_CLICK;
        }
        public String xTouch(){
            return handPart + ComponentPathsLeft.X_TOUCH;
        }
        public String yClick(){
            return handPart + ComponentPathsLeft.Y_CLICK;
        }
        public String yTouch(){
            return handPart + ComponentPathsLeft.Y_TOUCH;
        }
        /**
         * May not be available for application use
         */
        public String menuClick(){
            return handPart + ComponentPathsLeft.MENU_CLICK;
        }
    }

    @SuppressWarnings("unused")
    public static class BindingPathBuilderRightHand extends BindingPathBuilderCommon{

        public BindingPathBuilderRightHand(){
            super(InteractionProfiles.RIGHT_HAND);
        }

        public String aClick(){
            return handPart + ComponentPathsRight.A_CLICK;
        }
        public String aTouch(){
            return handPart + ComponentPathsRight.A_TOUCH;
        }
        public String bClick(){
            return handPart + ComponentPathsRight.B_CLICK;
        }
        public String bTouch(){
            return handPart + ComponentPathsRight.B_TOUCH;
        }
        /**
         * May not be available for application use
         */
        public String systemClick(){
            return handPart + ComponentPathsRight.SYSTEM_CLICK;
        }
    }

    @SuppressWarnings("unused")
    private static class BindingPathBuilderCommon{

        String handPart;

        public BindingPathBuilderCommon(String handPart){
            this.handPart = handPart;
        }

        public String squeeze(){
            return handPart + ComponentPathsEitherHand.SQUEEZE;
        }
        public String triggerValue(){
            return handPart + ComponentPathsEitherHand.TRIGGER_VALUE;
        }

        public String triggerTouch(){
            return handPart + ComponentPathsEitherHand.TRIGGER_TOUCH;
        }

        public String thumbStick(){
            return handPart + ComponentPathsEitherHand.THUMB_STICK;
        }
        public String thumbStickX(){
            return handPart + ComponentPathsEitherHand.THUMB_STICK_X;
        }
        public String thumbStickY(){
            return handPart + ComponentPathsEitherHand.THUMB_STICK_Y;
        }
        public String thumbStickClick(){
            return handPart + ComponentPathsEitherHand.THUMB_STICK_CLICK;
        }
        public String thumbStickTouch(){
            return handPart + ComponentPathsEitherHand.THUMB_STICK_TOUCH;
        }

//these  can be used once JME upgrades to LWJGL 3.3.3 or higher
//        /**
//         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String thumbDpadUp(){
//            return handPart + ComponentPathsEitherHand.THUMB_DPAD_UP;
//        }
//        /**
//         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String thumbDpadDown(){
//            return handPart + ComponentPathsEitherHand.THUMB_DPAD_DOWN;
//        }
//        /**
//         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String thumbDpadLeft(){
//            return handPart + ComponentPathsEitherHand.THUMB_DPAD_LEFT;
//        }
//        /**
//         * Treats the thumbstick as a dpad. This is provided by the extension XR_EXT_dpad_binding
//         */
//        public String thumbDpadRight(){
//            return handPart + ComponentPathsEitherHand.THUMB_DPAD_RIGHT;
//        }

        public String thumbBrestTouch(){
            return handPart + ComponentPathsEitherHand.THUMB_BREST_TOUCH;
        }
        public String gripPose(){
            return handPart + ComponentPathsEitherHand.GRIP_POSE;
        }
        public String aimPose(){
            return handPart + ComponentPathsEitherHand.AIM_POSE;
        }
        public String haptic(){
            return handPart + ComponentPathsEitherHand.HAPTIC;
        }
    }
}

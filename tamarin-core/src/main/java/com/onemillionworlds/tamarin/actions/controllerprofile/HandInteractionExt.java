package com.onemillionworlds.tamarin.actions.controllerprofile;

/**
 * This is not a controller, it is the cross-vendor profile for bare hands (hand tracking), provided by the
 * XR_EXT_hand_interaction extension (which Tamarin requests by default). It provides the gestures the runtime
 * recognises (pinch, grasp and aim activate) as inputs that actions can be bound to, just like controller buttons.
 * <p>
 *     Each gesture has a value (0 to 1) and a "ready" boolean. Ready means the hand is in a pose where the gesture is
 *     being prepared for (e.g. the thumb and index finger are approaching each other for a pinch). Ready can be
 *     used to show the user that a gesture is about to happen, which helps avoid accidental activations.
 * </p>
 * <p>
 *     There is no haptic output (hands can't vibrate).
 * </p>
 * <p>
 *     Note that on Quest the application must also declare hand tracking support in its AndroidManifest.xml:
 * </p>
 * <pre>{@code
 * <uses-permission android:name="com.oculus.permission.HAND_TRACKING" />
 * <uses-feature android:name="oculus.software.handtracking" android:required="false" />
 * }</pre>
 */
public class HandInteractionExt{
    public static final String PROFILE = "/interaction_profiles/ext/hand_interaction_ext";

    /**
     * The extension that must be loaded for this profile to be used.
     */
    public static final String REQUIRED_EXTENSION = "XR_EXT_hand_interaction";

    public static class InteractionProfiles{
        public static final String LEFT_HAND = "/user/hand/left";
        public static final String RIGHT_HAND = "/user/hand/right";
    }

    public static class ComponentPaths{
        /**
         * The thumb and index finger pinching together. 1 is a full pinch.
         */
        public static final String PINCH_VALUE = "/input/pinch_ext/value";
        public static final String PINCH_READY = "/input/pinch_ext/ready_ext";

        /**
         * A pinch-like "click" made while pointing (i.e. along the aim pose's ray), intended for selecting far away
         * things. 1 is fully activated.
         */
        public static final String AIM_ACTIVATE_VALUE = "/input/aim_activate_ext/value";
        public static final String AIM_ACTIVATE_READY = "/input/aim_activate_ext/ready_ext";

        /**
         * The hand closing into a grab/fist. 1 is fully grasped.
         */
        public static final String GRASP_VALUE = "/input/grasp_ext/value";
        public static final String GRASP_READY = "/input/grasp_ext/ready_ext";

        public static final String AIM_POSE = "/input/aim/pose";
        public static final String GRIP_POSE = "/input/grip/pose";
        /**
         * A pose at the point where the thumb and index finger meet when pinching
         */
        public static final String PINCH_POSE = "/input/pinch_ext/pose";
        /**
         * A pose at the tip of the index finger, for poking things
         */
        public static final String POKE_POSE = "/input/poke_ext/pose";
    }

    public static BindingPathBuilder pathBuilder(){
        return new BindingPathBuilder();
    }

    public static class BindingPathBuilder{
        public BindingPathBuilderHand leftHand(){
            return new BindingPathBuilderHand(InteractionProfiles.LEFT_HAND);
        }
        public BindingPathBuilderHand rightHand(){
            return new BindingPathBuilderHand(InteractionProfiles.RIGHT_HAND);
        }
    }

    public static class BindingPathBuilderHand{
        String handPart;

        public BindingPathBuilderHand(String handPart){
            this.handPart = handPart;
        }

        public String pinchValue(){
            return handPart + ComponentPaths.PINCH_VALUE;
        }
        public String pinchReady(){
            return handPart + ComponentPaths.PINCH_READY;
        }
        public String aimActivateValue(){
            return handPart + ComponentPaths.AIM_ACTIVATE_VALUE;
        }
        public String aimActivateReady(){
            return handPart + ComponentPaths.AIM_ACTIVATE_READY;
        }
        public String graspValue(){
            return handPart + ComponentPaths.GRASP_VALUE;
        }
        public String graspReady(){
            return handPart + ComponentPaths.GRASP_READY;
        }
        public String aimPose(){
            return handPart + ComponentPaths.AIM_POSE;
        }
        public String gripPose(){
            return handPart + ComponentPaths.GRIP_POSE;
        }
        public String pinchPose(){
            return handPart + ComponentPaths.PINCH_POSE;
        }
        public String pokePose(){
            return handPart + ComponentPaths.POKE_POSE;
        }
    }
}

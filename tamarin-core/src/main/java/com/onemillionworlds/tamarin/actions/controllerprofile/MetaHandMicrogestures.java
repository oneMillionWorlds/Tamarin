package com.onemillionworlds.tamarin.actions.controllerprofile;

import com.onemillionworlds.tamarin.actions.state.BooleanActionState;

/**
 * <b>META ONLY.</b> Thumb "microgestures" for bare hands (hand tracking), provided by the
 * XR_META_hand_tracking_microgestures extension (which Tamarin requests by default). At the time of writing only Meta
 * runtimes (Quest, and Quest Link) support this extension. On other runtimes these bindings are silently skipped
 * (so they are safe to suggest), but the actions bound to them will never activate. Applications should therefore
 * always provide another way to do the same thing.
 * <p>
 *     This is not a separate interaction profile, the extension adds extra inputs to the
 *     {@link HandInteractionExt} profile. That is why {@link #PROFILE} is the same as {@link HandInteractionExt#PROFILE}.
 *     They are kept in a separate class to make it clear that they are Meta only.
 * </p>
 * <p>
 *     The gestures are all made with the thumb against the side of the index finger. The user starts with their thumb
 *     raised above the index finger (with the other fingers slightly curled) then:
 * </p>
 * <ul>
 *     <li>Tap: touches the middle segment of the index finger with the thumb, then lifts it.</li>
 *     <li>Swipe left/right/forward/backward: touches the middle segment of the index finger, slides the thumb in that
 *     direction, then lifts it. Note that left and right are mirrored between the hands; on the right hand a left
 *     swipe is towards the index fingertip, on the left hand it is away from the index fingertip.</li>
 * </ul>
 * <p>
 *     All the inputs are booleans which only become true once the gesture has been completed and recognised (they are
 *     not true while the gesture is in progress). They are discrete events, so they should be bound to boolean actions
 *     and checked using {@link BooleanActionState#hasChanged()} ()}
 *     (or simply by checking the state every frame), similar to a d-pad.
 * </p>
 * <p>
 *     Intended usage is:
 * </p>
 * <pre>{@code
 * withSuggestedBinding(MetaHandMicrogestures.PROFILE, MetaHandMicrogestures.pathBuilder().rightHand().tapThumb())
 * }</pre>
 * <p>
 *     The extension depends on XR_EXT_hand_interaction, if that is removed from the {@link com.onemillionworlds.tamarin.openxr.XrSettings}
 *     then XR_META_hand_tracking_microgestures must be removed too.
 * </p>
 * <p>
 *     As with all hand tracking on Quest the application must also declare hand tracking support in its
 *     AndroidManifest.xml (see {@link HandInteractionExt}).
 * </p>
 */
public class MetaHandMicrogestures{
    /**
     * The same as {@link HandInteractionExt#PROFILE}, the microgestures extension adds inputs to that profile.
     */
    public static final String PROFILE = HandInteractionExt.PROFILE;

    /**
     * The extension that must be loaded for these bindings to be used (in addition to
     * {@link HandInteractionExt#REQUIRED_EXTENSION}).
     */
    public static final String REQUIRED_EXTENSION = "XR_META_hand_tracking_microgestures";

    public static class InteractionProfiles{
        public static final String LEFT_HAND = "/user/hand/left";
        public static final String RIGHT_HAND = "/user/hand/right";
    }

    public static class ComponentPaths{
        /**
         * The thumb sliding along the index finger, left (from the user's perspective, when looking at the back of the hand).
         * On the right hand this is towards the index fingertip, on the left hand it is away from it.
         */
        public static final String SWIPE_LEFT = "/input/swipe_left_meta/click";
        /**
         * The thumb sliding along the index finger, right (from the user's perspective, when looking at the back of the hand).
         * On the right hand this is away from the index fingertip, on the left hand it is towards it.
         */
        public static final String SWIPE_RIGHT = "/input/swipe_right_meta/click";
        /**
         * The thumb sliding forward across the index finger.
         */
        public static final String SWIPE_FORWARD = "/input/swipe_forward_meta/click";
        /**
         * The thumb sliding backward (downward) across the index finger.
         */
        public static final String SWIPE_BACKWARD = "/input/swipe_backward_meta/click";
        /**
         * The thumb tapping the middle segment of the index finger, then lifting.
         */
        public static final String TAP_THUMB = "/input/tap_thumb_meta/click";
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

        /**
         * META ONLY. See {@link ComponentPaths#SWIPE_LEFT}.
         */
        public String swipeLeft(){
            return handPart + ComponentPaths.SWIPE_LEFT;
        }
        /**
         * META ONLY. See {@link ComponentPaths#SWIPE_RIGHT}.
         */
        public String swipeRight(){
            return handPart + ComponentPaths.SWIPE_RIGHT;
        }
        /**
         * META ONLY. See {@link ComponentPaths#SWIPE_FORWARD}.
         */
        public String swipeForward(){
            return handPart + ComponentPaths.SWIPE_FORWARD;
        }
        /**
         * META ONLY. See {@link ComponentPaths#SWIPE_BACKWARD}.
         */
        public String swipeBackward(){
            return handPart + ComponentPaths.SWIPE_BACKWARD;
        }
        /**
         * META ONLY. See {@link ComponentPaths#TAP_THUMB}.
         */
        public String tapThumb(){
            return handPart + ComponentPaths.TAP_THUMB;
        }
    }
}

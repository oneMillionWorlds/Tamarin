package com.onemillionworlds.tamarin.actions.controllerprofile;

/**
 * Note that this is the headset of the vive. The controllers are seperately listed in {@link HtcViveController}
 */
public class HtcProVive{
    public static final String PROFILE = "/interaction_profiles/htc/vive_pro";

    public static class InteractionProfiles {
        public static final String HEAD = "/user/head";
    }

    public static class ComponentPaths {
        public static final String SYSTEM_CLICK = "/input/system/click";
        public static final String VOLUME_UP_CLICK = "/input/volume_up/click";
        public static final String VOLUME_DOWN_CLICK = "/input/volume_down/click";
        public static final String MUTE_MIC_CLICK = "/input/mute_mic/click";
    }

    public static BindingPathBuilder pathBuilder(){
        return new BindingPathBuilder();
    }

    public static class BindingPathBuilder {
        public BindingPathBuilderHead head() {
            return new BindingPathBuilderHead();
        }
    }

    @SuppressWarnings("unused")
    public static class BindingPathBuilderHead {
        String headPart = InteractionProfiles.HEAD;

        /**
         * May not be available for application use
         */
        public String systemClick(){
            return headPart + ComponentPaths.SYSTEM_CLICK;
        }

        public String volumeUpClick() {
            return headPart + ComponentPaths.VOLUME_UP_CLICK;
        }

        public String volumeDownClick() {
            return headPart + ComponentPaths.VOLUME_DOWN_CLICK;
        }

        public String muteMicClick() {
            return headPart + ComponentPaths.MUTE_MIC_CLICK;
        }
    }
}
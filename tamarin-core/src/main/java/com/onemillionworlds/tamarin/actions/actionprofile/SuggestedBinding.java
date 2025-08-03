package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;

public class SuggestedBinding{
    /**
     * A string representing the device. E.g. {@link OculusTouchController#PROFILE}
     */
    String profile;
    /**
     * A binding string for the physical item (such as a button).
     * E.g. /user/hand/left/input/x/click
     * <p>
     * There are convenience methods for this such as:
     * <pre>
     * {@code
     *  OculusTouchController.pathBuilder().leftHand().xClick()
     * }
     * </pre>
     */
    String binding;

    public SuggestedBinding(String profile, String binding){
        this.profile = profile;
        this.binding = binding;
    }

    public String getProfile(){
        return profile;
    }

    public String getBinding(){
        return binding;
    }

    public static SuggestedBindingBuilder builder(){
        return new SuggestedBindingBuilder();
    }

    public static class SuggestedBindingBuilder{
        private String profile;
        private String binding;

        public SuggestedBindingBuilder profile(String profile){
            this.profile = profile;
            return this;
        }

        public SuggestedBindingBuilder binding(String binding){
            this.binding = binding;
            return this;
        }

        public SuggestedBinding build(){
            if(profile == null){
                throw new IllegalArgumentException("profile cannot be null");
            }
            if(binding == null){
                throw new IllegalArgumentException("binding cannot be null");
            }
            return new SuggestedBinding(profile, binding);
        }

    }
}

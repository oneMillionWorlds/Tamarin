package com.onemillionworlds.tamarin.actions.controllerprofile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionProfileExtensionsTest{

    @Test
    void microgestureBindingsNeedBothTheHandInteractionAndMicrogestureExtensions(){
        for(String binding : List.of(
                MetaHandMicrogestures.pathBuilder().leftHand().tapThumb(),
                MetaHandMicrogestures.pathBuilder().leftHand().swipeLeft(),
                MetaHandMicrogestures.pathBuilder().rightHand().swipeRight(),
                MetaHandMicrogestures.pathBuilder().rightHand().swipeForward(),
                MetaHandMicrogestures.pathBuilder().rightHand().swipeBackward())){
            assertEquals(
                    List.of(HandInteractionExt.REQUIRED_EXTENSION, MetaHandMicrogestures.REQUIRED_EXTENSION),
                    InteractionProfileExtensions.requiredExtensionsForBinding(MetaHandMicrogestures.PROFILE, binding),
                    binding);
        }
    }

    @Test
    void standardHandInteractionBindingsOnlyNeedTheHandInteractionExtension(){
        assertEquals(
                List.of(HandInteractionExt.REQUIRED_EXTENSION),
                InteractionProfileExtensions.requiredExtensionsForBinding(HandInteractionExt.PROFILE, HandInteractionExt.pathBuilder().leftHand().pinchValue()));
    }

    @Test
    void controllerBindingsNeedNoExtensions(){
        assertTrue(InteractionProfileExtensions.requiredExtensionsForBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().triggerValue()).isEmpty());
    }
}

package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;
import com.onemillionworlds.tamarin.actions.controllerprofile.MetaHandMicrogestures;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestedBindingsProfileViewTest{

    @Test
    void profileWithoutRequiredExtensionsIsAlwaysSuggested(){
        SuggestedBindingsProfileView view = new SuggestedBindingsProfileView(OculusTouchController.PROFILE);
        view.addSuggestion("set", "trigger", OculusTouchController.pathBuilder().leftHand().triggerValue());

        assertEquals(1, view.getBindingsWithLoadedExtensions(extension -> false).size());
    }

    @Test
    void wholeProfileIsSkippedIfItsExtensionIsNotLoaded(){
        SuggestedBindingsProfileView view = handProfileWithPinchAndTap();

        assertTrue(view.getBindingsWithLoadedExtensions(extension -> false).isEmpty());
    }

    @Test
    void microgestureBindingsAreSkippedIfTheMicrogestureExtensionIsNotLoaded(){
        SuggestedBindingsProfileView view = handProfileWithPinchAndTap();

        Set<String> bindings = bindings(view.getBindingsWithLoadedExtensions(HandInteractionExt.REQUIRED_EXTENSION::equals));

        assertEquals(Set.of(HandInteractionExt.pathBuilder().rightHand().pinchValue()), bindings);
    }

    @Test
    void microgestureBindingsAreSuggestedIfBothExtensionsAreLoaded(){
        SuggestedBindingsProfileView view = handProfileWithPinchAndTap();

        Set<String> bindings = bindings(view.getBindingsWithLoadedExtensions(
                extension -> extension.equals(HandInteractionExt.REQUIRED_EXTENSION) || extension.equals(MetaHandMicrogestures.REQUIRED_EXTENSION)));

        assertEquals(Set.of(
                HandInteractionExt.pathBuilder().rightHand().pinchValue(),
                MetaHandMicrogestures.pathBuilder().rightHand().tapThumb()
        ), bindings);
    }

    private static SuggestedBindingsProfileView handProfileWithPinchAndTap(){
        SuggestedBindingsProfileView view = new SuggestedBindingsProfileView(HandInteractionExt.PROFILE);
        view.addSuggestion("set", "select", HandInteractionExt.pathBuilder().rightHand().pinchValue());
        view.addSuggestion("set", "tap", MetaHandMicrogestures.pathBuilder().rightHand().tapThumb());
        return view;
    }

    private static Set<String> bindings(List<Map.Entry<SuggestedBindingsProfileView.ActionData, String>> actionsAndBindings){
        return actionsAndBindings.stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }
}

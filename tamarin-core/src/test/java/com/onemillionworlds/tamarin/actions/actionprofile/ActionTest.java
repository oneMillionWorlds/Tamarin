package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;
import com.onemillionworlds.tamarin.handgestures.Finger;
import com.onemillionworlds.tamarin.handgestures.HandGestures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionTest{

    private static final ActionHandle HANDLE = new ActionHandle("main", "reset");

    @Test
    void anActionCanHaveOnlyGestureBindings(){
        Action action = Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.BOOLEAN)
                .withGestureBinding(HandSide.LEFT, HandGestures.thumbsUp())
                .build();
        assertEquals(1, action.getGestureBindings().size());
        assertEquals(HandSide.LEFT, action.getGestureBindings().get(0).getHandSide());
    }

    @Test
    void anIndexPinchGestureAlsoSuggestsTheHandInteractionPinch(){
        Action action = Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.FLOAT)
                .withGestureBinding(HandSide.LEFT, HandGestures.pinch(Finger.INDEX))
                .withGestureBinding(HandSide.RIGHT, HandGestures.pinch(Finger.INDEX))
                .build();
        assertEquals(List.of(
                HandInteractionExt.PROFILE + " " + HandInteractionExt.pathBuilder().leftHand().pinchValue(),
                HandInteractionExt.PROFILE + " " + HandInteractionExt.pathBuilder().rightHand().pinchValue()
        ), describe(action.getSuggestedBindings()));
    }

    @Test
    void aRuntimeEquivalentAlreadySuggestedIsNotDuplicated(){
        Action action = Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.FLOAT)
                .withSuggestedBinding(HandInteractionExt.PROFILE, HandInteractionExt.pathBuilder().leftHand().pinchValue())
                .withGestureBinding(HandSide.LEFT, HandGestures.pinch(Finger.INDEX))
                .build();
        assertEquals(1, action.getSuggestedBindings().size());
    }

    @Test
    void gesturesWithoutARuntimeEquivalentDoNotAddSuggestedBindings(){
        Action action = Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.BOOLEAN)
                .withGestureBinding(HandSide.LEFT, HandGestures.pinch(Finger.MIDDLE))
                .withGestureBinding(HandSide.LEFT, HandGestures.fist())
                .build();
        assertTrue(action.getSuggestedBindings().isEmpty());
    }

    private static List<String> describe(List<SuggestedBinding> bindings){
        List<String> descriptions = new ArrayList<>();
        for(SuggestedBinding binding : bindings){
            descriptions.add(binding.getProfile() + " " + binding.getBinding());
        }
        return descriptions;
    }

    @Test
    void anActionWithNoBindingsAtAllIsRejected(){
        assertThrows(IllegalArgumentException.class, () -> Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.BOOLEAN)
                .build());
    }

    @Test
    void gestureBindingsAreRejectedOnNonBooleanOrFloatActions(){
        assertThrows(IllegalArgumentException.class, () -> Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.VECTOR2F)
                .withGestureBinding(HandSide.LEFT, HandGestures.fist())
                .build());
    }

    @Test
    void gestureBindingsMustBeForASupportedSubActionPath(){
        assertThrows(IllegalArgumentException.class, () -> Action.builder()
                .actionHandle(HANDLE)
                .actionType(ActionType.BOOLEAN)
                .overrideSupportedSubActionPaths(List.of(HandSide.RIGHT.restrictToInputString))
                .withGestureBinding(HandSide.LEFT, HandGestures.fist())
                .build());
    }
}

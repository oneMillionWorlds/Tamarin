package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.handgestures.HandGestures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.handgestures.HandGestureSource;
import com.onemillionworlds.tamarin.handgestures.HandGestureState;
import com.onemillionworlds.tamarin.handgestures.HandGestures;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticGestureBindingsTest{

    private static final ActionHandle RESET = new ActionHandle("main", "reset");
    private static final ActionHandle SQUEEZE = new ActionHandle("main", "squeeze");
    private static final ActionHandle UNBOUND = new ActionHandle("main", "unbound");
    private static final ActionHandle OTHER_SET_ACTION = new ActionHandle("other", "other_reset");

    private final SyntheticGestureBindings bindings = new SyntheticGestureBindings(manifest());

    private final Map<HandSide, Optional<HandGestureState>> handStates = new EnumMap<>(HandSide.class);

    @Test
    void aGestureTurnsABooleanActionOnAndReportsTheChangeOnce(){
        update(thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));
        ActionSourceState state = bindings.getBooleanState(RESET, HandSide.LEFT);
        assertTrue(state.getBooleanValue());
        assertTrue(state.isChangedSinceLastSync());
        assertTrue(state.isActive());

        update(thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));
        state = bindings.getBooleanState(RESET, HandSide.LEFT);
        assertTrue(state.getBooleanValue());
        assertFalse(state.isChangedSinceLastSync());

        update(thumbsUp(HandSide.LEFT, false), thumbsUp(HandSide.RIGHT, false));
        state = bindings.getBooleanState(RESET, HandSide.LEFT);
        assertFalse(state.getBooleanValue());
        assertTrue(state.isChangedSinceLastSync());
    }

    @Test
    void eachHandHasItsOwnState(){
        update(thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));
        assertTrue(bindings.getBooleanState(RESET, HandSide.LEFT).getBooleanValue());
        assertFalse(bindings.getBooleanState(RESET, HandSide.RIGHT).getBooleanValue());
    }

    @Test
    void anUntrackedHandIsInactive(){
        update(thumbsUp(HandSide.LEFT, true), Optional.empty());
        update(Optional.empty(), Optional.empty());

        ActionSourceState state = bindings.getBooleanState(RESET, HandSide.LEFT);
        assertFalse(state.isActive());
        assertFalse(state.getBooleanValue());
        assertTrue(state.isChangedSinceLastSync(), "it was on before the hand was lost");
    }

    @Test
    void theSystemGestureMakesTheHandInactive(){
        HandGestureState systemGesture = HandGestureState.builder(HandSide.LEFT, HandGestureSource.RUNTIME)
                .thumbsUp(1, true)
                .runtimeFlags(true, false, false)
                .build();
        update(Optional.of(systemGesture), Optional.empty());

        ActionSourceState state = bindings.getBooleanState(RESET, HandSide.LEFT);
        assertFalse(state.isActive());
        assertFalse(state.getBooleanValue());
    }

    @Test
    void gesturesInInactiveActionSetsAreInactive(){
        handStates.put(HandSide.LEFT, thumbsUp(HandSide.LEFT, true));
        handStates.put(HandSide.RIGHT, Optional.empty());
        bindings.update(this::stateFor, List.of("main"));

        assertFalse(bindings.getBooleanState(OTHER_SET_ACTION, HandSide.LEFT).isActive());
        assertTrue(bindings.getBooleanState(RESET, HandSide.LEFT).getBooleanValue());
    }

    @Test
    void aHandWithoutGestureBindingsIsInactive(){
        update(fist(HandSide.LEFT, 1), fist(HandSide.RIGHT, 1));
        // squeeze only has a left hand gesture binding
        assertFalse(bindings.getFloatState(SQUEEZE, HandSide.RIGHT).isActive());
        assertFalse(bindings.getBooleanState(UNBOUND, HandSide.LEFT).isActive());
        assertFalse(bindings.hasGestureBindings(UNBOUND));
        assertTrue(bindings.hasGestureBindings(SQUEEZE));
    }

    @Test
    void floatActionsUseTheGestureStrength(){
        update(fist(HandSide.LEFT, 0.6f), Optional.empty());
        ActionSourceState state = bindings.getFloatState(SQUEEZE, HandSide.LEFT);
        assertEquals(0.6f, state.getValue());
        assertTrue(state.isChangedSinceLastSync());

        update(fist(HandSide.LEFT, 0.6f), Optional.empty());
        assertFalse(bindings.getFloatState(SQUEEZE, HandSide.LEFT).isChangedSinceLastSync());
    }

    @Test
    void gestureBindingsAreDescribed(){
        assertEquals(List.of("Left Hand Thumbs up (gesture)", "Right Hand Thumbs up (gesture)"), bindings.describe(RESET));
    }

    private void update(Optional<HandGestureState> left, Optional<HandGestureState> right){
        handStates.put(HandSide.LEFT, left);
        handStates.put(HandSide.RIGHT, right);
        bindings.update(this::stateFor, List.of("main", "other"));
    }

    private Optional<HandGestureState> stateFor(HandSide handSide){
        return handStates.getOrDefault(handSide, Optional.empty());
    }

    private static Optional<HandGestureState> thumbsUp(HandSide handSide, boolean thumbsUp){
        return Optional.of(HandGestureState.builder(handSide, HandGestureSource.TAMARIN).thumbsUp(thumbsUp ? 1 : 0, thumbsUp).build());
    }

    private static Optional<HandGestureState> fist(HandSide handSide, float strength){
        return Optional.of(HandGestureState.builder(handSide, HandGestureSource.TAMARIN).fist(strength, false).build());
    }

    private static ActionManifest manifest(){
        return ActionManifest.builder()
                .withActionSet(ActionSet.builder()
                        .name("main")
                        .translatedName("Main")
                        .priority(1)
                        .withAction(Action.builder()
                                .actionHandle(RESET)
                                .actionType(ActionType.BOOLEAN)
                                .withGestureBinding(HandSide.LEFT, HandGestures.thumbsUp())
                                .withGestureBinding(HandSide.RIGHT, HandGestures.thumbsUp())
                                .build())
                        .withAction(Action.builder()
                                .actionHandle(SQUEEZE)
                                .actionType(ActionType.FLOAT)
                                .withGestureBinding(HandSide.LEFT, HandGestures.fist())
                                .build())
                        .withAction(Action.builder()
                                .actionHandle(UNBOUND)
                                .actionType(ActionType.BOOLEAN)
                                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().rightHand().aClick())
                                .build())
                        .build())
                .withActionSet(ActionSet.builder()
                        .name("other")
                        .translatedName("Other")
                        .priority(1)
                        .withAction(Action.builder()
                                .actionHandle(OTHER_SET_ACTION)
                                .actionType(ActionType.BOOLEAN)
                                .withGestureBinding(HandSide.LEFT, HandGestures.thumbsUp())
                                .build())
                        .build())
                .build();
    }
}

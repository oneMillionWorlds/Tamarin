package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSource;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;
import com.onemillionworlds.tamarin.handgestures.HandGestureState;
import com.onemillionworlds.tamarin.handgestures.HandGestures;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticGestureBindingsTest{

    private static final ActionHandle RESET = new ActionHandle("main", "reset");
    private static final ActionHandle SQUEEZE = new ActionHandle("main", "squeeze");
    private static final ActionHandle UNBOUND = new ActionHandle("main", "unbound");
    private static final ActionHandle OTHER_SET_ACTION = new ActionHandle("other", "other_reset");

    private static final String LEFT = HandSide.LEFT.restrictToInputString;
    private static final String RIGHT = HandSide.RIGHT.restrictToInputString;

    /**
     * Nothing bound in the runtime is available (e.g. the action is only bound to controller buttons and hands are in use)
     */
    private static final RuntimeActionState RUNTIME_INACTIVE = RuntimeActionState.ofBoolean(false, false, false);
    private static final RuntimeActionState RUNTIME_ACTIVE_OFF = RuntimeActionState.ofBoolean(false, false, true);
    private static final RuntimeActionState RUNTIME_ACTIVE_ON = RuntimeActionState.ofBoolean(true, false, true);
    private static final RuntimeActionState RUNTIME_ACTIVE_JUST_ON = RuntimeActionState.ofBoolean(true, true, true);

    private final SyntheticGestureBindings bindings = new SyntheticGestureBindings(manifest());

    private final Map<HandSide, Optional<HandGestureState>> handStates = new EnumMap<>(HandSide.class);

    @Test
    void aGestureTurnsABooleanActionOnAndReportsTheChangeOnce(){
        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));
        BooleanActionState state = bindings.combineBoolean(RESET, null, allHands(RUNTIME_INACTIVE));
        assertTrue(state.getState());
        assertTrue(state.hasChangedToTrue());

        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));
        state = bindings.combineBoolean(RESET, null, allHands(RUNTIME_INACTIVE));
        assertTrue(state.getState());
        assertFalse(state.hasChanged());

        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, false), thumbsUp(HandSide.RIGHT, false));
        assertTrue(bindings.combineBoolean(RESET, null, allHands(RUNTIME_INACTIVE)).hasChangedToFalse());
    }

    @Test
    void automaticCombinesTheRuntimeAndTheGesture(){
        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), Optional.empty());
        assertTrue(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_OFF)).getState(), "the gesture is on");

        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, false), Optional.empty());
        assertTrue(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_ON)).getState(), "the runtime is on");
    }

    @Test
    void automaticRuntimeTurningOnWhileTheGestureIsAlreadyOnIsNotAChange(){
        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), Optional.empty());
        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), Optional.empty());

        BooleanActionState state = bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_JUST_ON));
        assertTrue(state.getState());
        assertFalse(state.hasChanged());
    }

    @Test
    void runtimeOnlyGivesAnActiveRuntimeBindingPrecedence(){
        update(HandGestureSourcePreference.RUNTIME_ONLY, thumbsUp(HandSide.LEFT, true), Optional.empty());
        assertFalse(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_OFF)).getState());
    }

    @Test
    void runtimeOnlyUsesTheGestureIfTheRuntimeIsInactive(){
        update(HandGestureSourcePreference.RUNTIME_ONLY, thumbsUp(HandSide.LEFT, true), Optional.empty());
        assertTrue(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_INACTIVE)).getState());
    }

    @Test
    void tamarinOnlyGivesTheGesturePrecedenceWhenTheHandIsTracked(){
        update(HandGestureSourcePreference.TAMARIN_ONLY, thumbsUp(HandSide.LEFT, false), Optional.empty());
        assertFalse(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_ON)).getState());
    }

    @Test
    void controllersStillWorkWhateverThePreference(){
        for(HandGestureSourcePreference preference : HandGestureSourcePreference.values()){
            // the hand isn't tracked as a bare hand, it is holding a controller
            update(preference, Optional.empty(), Optional.empty());
            assertTrue(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_ACTIVE_ON)).getState(), preference.name());
        }
    }

    @Test
    void theSystemGestureLeavesTheHandToTheRuntime(){
        HandGestureState systemGesture = HandGestureState.builder(HandSide.LEFT, HandGestureSource.RUNTIME)
                .thumbsUp(1, true)
                .runtimeFlags(true, false, false)
                .build();
        update(HandGestureSourcePreference.TAMARIN_ONLY, Optional.of(systemGesture), Optional.empty());
        assertFalse(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_INACTIVE)).getState());
    }

    @Test
    void readingBothHandsCombinesEachHandsResult(){
        // left: Tamarin precedence, gesture on. Right: not tracked, so the runtime (off)
        update(HandGestureSourcePreference.TAMARIN_ONLY, thumbsUp(HandSide.LEFT, true), Optional.empty());
        Map<String, RuntimeActionState> runtime = new HashMap<>();
        runtime.put(LEFT, RUNTIME_ACTIVE_OFF);
        runtime.put(RIGHT, RUNTIME_ACTIVE_OFF);
        assertTrue(bindings.combineBoolean(RESET, null, runtime::get).getState());
        assertFalse(bindings.combineBoolean(RESET, RIGHT, runtime::get).getState());
    }

    @Test
    void gestureBindingsOnlyApplyToTheirOwnHand(){
        update(HandGestureSourcePreference.AUTOMATIC, thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, false));

        assertTrue(bindings.combineBoolean(RESET, LEFT, allHands(RUNTIME_INACTIVE)).getState());
        assertFalse(bindings.combineBoolean(RESET, RIGHT, allHands(RUNTIME_INACTIVE)).getState());
    }

    @Test
    void gesturesInInactiveActionSetsAreIgnored(){
        handStates.put(HandSide.LEFT, thumbsUp(HandSide.LEFT, true));
        handStates.put(HandSide.RIGHT, Optional.empty());
        bindings.update(this::stateFor, List.of("main"), HandGestureSourcePreference.AUTOMATIC);

        assertFalse(bindings.combineBoolean(OTHER_SET_ACTION, null, allHands(RUNTIME_INACTIVE)).getState());
        assertTrue(bindings.combineBoolean(RESET, null, allHands(RUNTIME_INACTIVE)).getState());
    }

    @Test
    void floatActionsTakeTheLargerValueInAutomatic(){
        update(HandGestureSourcePreference.AUTOMATIC, fist(HandSide.LEFT, 0.6f), Optional.empty());

        FloatActionState state = bindings.combineFloat(SQUEEZE, LEFT, allHands(new RuntimeActionState(0.2f, false, true)));
        assertEquals(0.6f, state.getState());
        assertTrue(state.hasChanged());

        assertEquals(0.9f, bindings.combineFloat(SQUEEZE, LEFT, allHands(new RuntimeActionState(0.9f, false, true))).getState());
    }

    @Test
    void floatActionsUseTheRuntimeValueWhenItHasPrecedence(){
        update(HandGestureSourcePreference.RUNTIME_ONLY, fist(HandSide.LEFT, 0.6f), Optional.empty());
        assertEquals(0.2f, bindings.combineFloat(SQUEEZE, LEFT, allHands(new RuntimeActionState(0.2f, false, true))).getState());
    }

    @Test
    void actionsWithoutGestureBindingsAreTheRuntimeState(){
        update(HandGestureSourcePreference.TAMARIN_ONLY, thumbsUp(HandSide.LEFT, true), thumbsUp(HandSide.RIGHT, true));
        BooleanActionState state = bindings.combineBoolean(UNBOUND, null, allHands(RuntimeActionState.ofBoolean(false, true, true)));
        assertFalse(state.getState());
        assertTrue(state.hasChangedToFalse());
    }

    @Test
    void gestureBindingsAreDescribed(){
        assertEquals(List.of("Left Hand Thumbs up (gesture)", "Right Hand Thumbs up (gesture)"), bindings.describe(RESET));
    }

    private void update(HandGestureSourcePreference preference, Optional<HandGestureState> left, Optional<HandGestureState> right){
        handStates.put(HandSide.LEFT, left);
        handStates.put(HandSide.RIGHT, right);
        bindings.update(this::stateFor, List.of("main", "other"), preference);
    }

    private Optional<HandGestureState> stateFor(HandSide handSide){
        return handStates.getOrDefault(handSide, Optional.empty());
    }

    private static Function<String, RuntimeActionState> allHands(RuntimeActionState state){
        return subActionPath -> state;
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

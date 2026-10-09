package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TamarinGestureActionsTest{

    private static final ActionHandle FIST_ACTION = new ActionHandle("test_set", "fist");
    private static final ActionHandle PLAIN_ACTION = new ActionHandle("test_set", "plain");

    private static final BooleanActionState RUNTIME_OFF = new BooleanActionState(false, false);

    private static final Predicate<String> NO_EXTENSIONS = extension -> false;

    private static final ActionHandle PINCH_ACTION = new ActionHandle("test_set", "pinch");

    @Test
    void gestureTheRuntimeProvidesIsNotDetectedByTamarin(){
        TamarinGestureActions gestures = new TamarinGestureActions(pinchManifest(), HandInteractionExt.REQUIRED_EXTENSION::equals);
        gestures.update(HandSide.LEFT, Optional.of(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.01f).joints()));

        assertFalse(gestures.hasGestures());
        assertFalse(gestures.combine(PINCH_ACTION, null, RUNTIME_OFF).getState());
    }

    @Test
    void gestureIsDetectedByTamarinIfTheRuntimeExtensionIsMissing(){
        TamarinGestureActions gestures = new TamarinGestureActions(pinchManifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(SyntheticHand.openHand().fingerTipNearThumbTip(Finger.INDEX, 0.01f).joints()));

        assertTrue(gestures.hasGestures());
        assertTrue(gestures.combine(PINCH_ACTION, null, RUNTIME_OFF).getState());
    }

    private static ActionManifest pinchManifest(){
        return ActionManifest.builder()
                .withActionSet(ActionSet.builder()
                        .name("test_set")
                        .translatedName("Test set")
                        .priority(0)
                        .withAction(Action.builder()
                                .actionHandle(PINCH_ACTION)
                                .translatedName("Pinch")
                                .actionType(ActionType.BOOLEAN)
                                .withGestureBinding(HandSide.LEFT, Gesture.PINCH_INDEX)))
                .build();
    }

    @Test
    void manifestWithoutGesturesNeedsNoGestureDetection(){
        assertFalse(new TamarinGestureActions(new ActionManifest(List.of()), NO_EXTENSIONS).hasGestures());
        assertTrue(new TamarinGestureActions(manifest(), NO_EXTENSIONS).hasGestures());
    }

    @Test
    void actionWithoutGesturesIsJustTheRuntimeState(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));
        BooleanActionState runtimeState = new BooleanActionState(true, true);

        assertSame(runtimeState, gestures.combine(PLAIN_ACTION, null, runtimeState));
    }

    @Test
    void gestureActivatesTheActionAndReportsTheChangeOnce(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);

        gestures.update(HandSide.LEFT, Optional.of(SyntheticHand.openHand().joints()));
        assertState(false, false, gestures.combine(FIST_ACTION, null, RUNTIME_OFF));

        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));
        assertState(true, true, gestures.combine(FIST_ACTION, null, RUNTIME_OFF));

        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));
        assertState(true, false, gestures.combine(FIST_ACTION, null, RUNTIME_OFF));
    }

    @Test
    void untrackedHandDeactivatesTheAction(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));

        gestures.update(HandSide.LEFT, Optional.empty());

        assertState(false, true, gestures.combine(FIST_ACTION, null, RUNTIME_OFF));
        assertFalse(gestures.getState(HandSide.LEFT).isPresent());
    }

    @Test
    void gestureOnlyCountsForTheHandItIsBoundTo(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.RIGHT, Optional.of(HandGestureRulesTest.fist().joints()));

        assertFalse(gestures.combine(FIST_ACTION, null, RUNTIME_OFF).getState());
    }

    @Test
    void restrictingToTheOtherHandIgnoresTheGesture(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));

        assertTrue(gestures.combine(FIST_ACTION, HandSide.LEFT.restrictToInputString, RUNTIME_OFF).getState());
        assertFalse(gestures.combine(FIST_ACTION, HandSide.RIGHT.restrictToInputString, RUNTIME_OFF).getState());
    }

    @Test
    void runtimeTurningOnWhileGestureAlreadyOnIsNotAChange(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));

        assertState(true, false, gestures.combine(FIST_ACTION, null, new BooleanActionState(true, true)));
    }

    @Test
    void runtimeTurningOffWhileGestureStillOnIsNotAChange(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));
        gestures.update(HandSide.LEFT, Optional.of(HandGestureRulesTest.fist().joints()));

        assertState(true, false, gestures.combine(FIST_ACTION, null, new BooleanActionState(false, true)));
    }

    @Test
    void runtimeChangeIsPassedThroughWhenNoGestureIsActive(){
        TamarinGestureActions gestures = new TamarinGestureActions(manifest(), NO_EXTENSIONS);
        gestures.update(HandSide.LEFT, Optional.of(SyntheticHand.openHand().joints()));

        assertState(true, true, gestures.combine(FIST_ACTION, null, new BooleanActionState(true, true)));
    }

    private static void assertState(boolean expectedState, boolean expectedChanged, BooleanActionState actual){
        assertEquals(expectedState, actual.getState(), "state");
        assertEquals(expectedChanged, actual.hasChanged(), "changed");
    }

    private static ActionManifest manifest(){
        return ActionManifest.builder()
                .withActionSet(ActionSet.builder()
                        .name("test_set")
                        .translatedName("Test set")
                        .priority(0)
                        .withAction(Action.builder()
                                .actionHandle(FIST_ACTION)
                                .translatedName("Fist")
                                .actionType(ActionType.BOOLEAN)
                                .withGestureBinding(HandSide.LEFT, Gesture.FIST))
                        .withAction(Action.builder()
                                .actionHandle(PLAIN_ACTION)
                                .translatedName("Plain")
                                .actionType(ActionType.BOOLEAN)
                                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().xClick())))
                .build();
    }
}

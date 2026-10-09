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

import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestureBindingLayerTest{

    private static final ActionHandle FIST_ACTION = new ActionHandle("test_set", "fist");
    private static final ActionHandle PINCH_INDEX_ACTION = new ActionHandle("test_set", "pinch_index");
    private static final ActionHandle PINCH_MIDDLE_ACTION = new ActionHandle("test_set", "pinch_middle");
    private static final ActionHandle PLAIN_ACTION = new ActionHandle("test_set", "plain");

    private static final BooleanActionState RUNTIME_OFF = new BooleanActionState(false, false);

    private static final Predicate<String> NO_EXTENSIONS = extension -> false;
    private static final Predicate<String> ALL_EXTENSIONS = extension -> true;

    private static final HandGestureTamarinProvidedState FIST = HandGestureTamarinProvidedState.builder().fist(1, true).build();
    private static final HandGestureTamarinProvidedState OPEN = HandGestureTamarinProvidedState.builder().build();

    private static final HandTrackingAimState AIM_MIDDLE_PINCHING = new HandTrackingAimState(HandTrackingAimState.VALID_BIT | HandTrackingAimState.MIDDLE_PINCHING_BIT, 0, 1, 0, 0);
    private static final HandTrackingAimState AIM_NOT_PINCHING = new HandTrackingAimState(HandTrackingAimState.VALID_BIT, 0, 0, 0, 0);

    @Test
    void layerWithoutGesturesNeedsNoUpdates(){
        assertFalse(GestureBindingLayer.none().hasGestures());
        assertTrue(GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS).hasGestures());
    }

    @Test
    void actionWithoutGesturesIsJustTheStateBelow(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(FIST));
        BooleanActionState runtimeState = new BooleanActionState(true, true);

        assertSame(runtimeState, layer.combine(PLAIN_ACTION, null, runtimeState));
    }

    @Test
    void gestureActivatesTheActionAndReportsTheChangeOnce(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);

        layer.update(HandSide.LEFT, Optional.of(OPEN));
        assertState(false, false, layer.combine(FIST_ACTION, null, RUNTIME_OFF));

        layer.update(HandSide.LEFT, Optional.of(FIST));
        assertState(true, true, layer.combine(FIST_ACTION, null, RUNTIME_OFF));

        layer.update(HandSide.LEFT, Optional.of(FIST));
        assertState(true, false, layer.combine(FIST_ACTION, null, RUNTIME_OFF));
    }

    @Test
    void unavailableStateDeactivatesTheAction(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(FIST));

        layer.update(HandSide.LEFT, Optional.empty());

        assertState(false, true, layer.combine(FIST_ACTION, null, RUNTIME_OFF));
        assertFalse(layer.getState(HandSide.LEFT).isPresent());
    }

    @Test
    void gestureOnlyCountsForTheHandItIsBoundTo(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.RIGHT, Optional.of(FIST));

        assertFalse(layer.combine(FIST_ACTION, null, RUNTIME_OFF).getState());
    }

    @Test
    void restrictingToTheOtherHandIgnoresTheGesture(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(FIST));

        assertTrue(layer.combine(FIST_ACTION, HandSide.LEFT.restrictToInputString, RUNTIME_OFF).getState());
        assertFalse(layer.combine(FIST_ACTION, HandSide.RIGHT.restrictToInputString, RUNTIME_OFF).getState());
    }

    @Test
    void stateBelowTurningOnWhileGestureAlreadyOnIsNotAChange(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(FIST));
        layer.update(HandSide.LEFT, Optional.of(FIST));

        assertState(true, false, layer.combine(FIST_ACTION, null, new BooleanActionState(true, true)));
    }

    @Test
    void stateBelowTurningOffWhileGestureStillOnIsNotAChange(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(FIST));
        layer.update(HandSide.LEFT, Optional.of(FIST));

        assertState(true, false, layer.combine(FIST_ACTION, null, new BooleanActionState(false, true)));
    }

    @Test
    void stateBelowChangeIsPassedThroughWhenNoGestureIsActive(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(OPEN));

        assertState(true, true, layer.combine(FIST_ACTION, null, new BooleanActionState(true, true)));
    }

    @Test
    void tamarinDoesNotDetectGesturesTheRuntimeProvides(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), HandInteractionExt.REQUIRED_EXTENSION::equals);
        layer.update(HandSide.LEFT, Optional.of(HandGestureTamarinProvidedState.builder().pinch(Finger.INDEX, 1, true).build()));

        assertFalse(layer.combine(PINCH_INDEX_ACTION, null, RUNTIME_OFF).getState());
    }

    @Test
    void tamarinDetectsGesturesIfTheRuntimeExtensionIsMissing(){
        GestureBindingLayer<HandGestureTamarinProvidedState> layer = GestureBindingLayer.tamarinDetected(manifest(), NO_EXTENSIONS);
        layer.update(HandSide.LEFT, Optional.of(HandGestureTamarinProvidedState.builder().pinch(Finger.INDEX, 1, true).build()));

        assertTrue(layer.combine(PINCH_INDEX_ACTION, null, RUNTIME_OFF).getState());
    }

    @Test
    void aimStateLayerOnlyHasGesturesIfItsExtensionIsLoaded(){
        assertFalse(GestureBindingLayer.aimStateDetected(manifest(), NO_EXTENSIONS).hasGestures());
        assertTrue(GestureBindingLayer.aimStateDetected(manifest(), HandTrackingAimState.REQUIRED_EXTENSION::equals).hasGestures());
    }

    @Test
    void aimStateLayerProvidesTheGesturesTamarinNoLongerDoes(){
        Predicate<String> aimLoaded = HandTrackingAimState.REQUIRED_EXTENSION::equals;
        GestureBindingLayer<HandGestureTamarinProvidedState> tamarinLayer = GestureBindingLayer.tamarinDetected(manifest(), aimLoaded);
        GestureBindingLayer<HandTrackingAimState> aimLayer = GestureBindingLayer.aimStateDetected(manifest(), aimLoaded);

        // Tamarin thinks the middle finger is pinching, but this gesture belongs to the aim layer now
        tamarinLayer.update(HandSide.LEFT, Optional.of(HandGestureTamarinProvidedState.builder().pinch(Finger.MIDDLE, 1, true).build()));
        aimLayer.update(HandSide.LEFT, Optional.of(AIM_NOT_PINCHING));
        assertFalse(aimLayer.combine(PINCH_MIDDLE_ACTION, null, tamarinLayer.combine(PINCH_MIDDLE_ACTION, null, RUNTIME_OFF)).getState());

        aimLayer.update(HandSide.LEFT, Optional.of(AIM_MIDDLE_PINCHING));
        assertState(true, true, aimLayer.combine(PINCH_MIDDLE_ACTION, null, tamarinLayer.combine(PINCH_MIDDLE_ACTION, null, RUNTIME_OFF)));
    }

    @Test
    void aimStateLayerIgnoresGesturesItCannotProvide(){
        GestureBindingLayer<HandTrackingAimState> aimLayer = GestureBindingLayer.aimStateDetected(manifest(), ALL_EXTENSIONS);
        aimLayer.update(HandSide.LEFT, Optional.of(AIM_MIDDLE_PINCHING));

        assertSame(RUNTIME_OFF, aimLayer.combine(FIST_ACTION, null, RUNTIME_OFF));
        assertSame(RUNTIME_OFF, aimLayer.combine(PINCH_INDEX_ACTION, null, RUNTIME_OFF));
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
                        .withAction(gestureAction(FIST_ACTION, Gesture.FIST))
                        .withAction(gestureAction(PINCH_INDEX_ACTION, Gesture.PINCH_INDEX))
                        .withAction(gestureAction(PINCH_MIDDLE_ACTION, Gesture.PINCH_MIDDLE))
                        .withAction(Action.builder()
                                .actionHandle(PLAIN_ACTION)
                                .translatedName("Plain")
                                .actionType(ActionType.BOOLEAN)
                                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().xClick())))
                .build();
    }

    private static Action.ActionBuilder gestureAction(ActionHandle handle, Gesture gesture){
        return Action.builder()
                .actionHandle(handle)
                .translatedName(handle.actionName())
                .actionType(ActionType.BOOLEAN)
                .withGestureBinding(HandSide.LEFT, gesture);
    }
}

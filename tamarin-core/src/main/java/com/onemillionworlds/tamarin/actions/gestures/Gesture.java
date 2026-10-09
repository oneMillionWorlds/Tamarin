package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.SuggestedBinding;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum Gesture {

    PINCH_INDEX(
            "Pinch Thumb and index finger",
            state -> state.isPinching(Finger.INDEX),
            state -> state.getPinchStrength(Finger.INDEX),
            Map.of(
                    HandSide.LEFT, new SuggestedBinding(HandInteractionExt.PROFILE, HandInteractionExt.pathBuilder().leftHand().pinchValue()),
                    HandSide.RIGHT, new SuggestedBinding(HandInteractionExt.PROFILE, HandInteractionExt.pathBuilder().rightHand().pinchValue())
            ),
            HandInteractionExt.REQUIRED_EXTENSION
    ),
    PINCH_MIDDLE(
            "Pinch Thumb and middle finger",
            state -> state.isPinching(Finger.MIDDLE),
            state -> state.getPinchStrength(Finger.MIDDLE),
            null,
            "XR_FB_hand_tracking_aim"
    ),
    PINCH_RING(
            "Pinch Thumb and ring finger",
            state -> state.isPinching(Finger.RING),
            state -> state.getPinchStrength(Finger.RING),
            null,
            "XR_FB_hand_tracking_aim"
    ),
    PINCH_LITTLE_FINGER(
            "Pinch Thumb and little finger",
            state -> state.isPinching(Finger.LITTLE),
            state -> state.getPinchStrength(Finger.LITTLE),
            null,
            "XR_FB_hand_tracking_aim"
    ),

    FIST("Fist",
            HandGestureTamarinProvidedState::isFist,
            HandGestureTamarinProvidedState::getFistStrength
            ),

    POINT(
            "Pointing",
            HandGestureTamarinProvidedState::isPointing,
            HandGestureTamarinProvidedState::getPointStrength
    ),

    THUMBS_UP("Thumbs up",
            HandGestureTamarinProvidedState::isThumbsUp,
            HandGestureTamarinProvidedState::getThumbsUpStrength)
    ;

    public final String description;
    public final Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator;

    public final Function<HandGestureTamarinProvidedState, Float> floatEvaluator;

    final Optional<Map<HandSide, SuggestedBinding>> runtimeEquivalentBinding;

    final Optional<String> runtimeEquivalentRequiredExtension;

    Gesture(String description, Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator, Function<HandGestureTamarinProvidedState, Float> floatEvaluator) {
        this.booleanEvaluator = booleanEvaluator;
        this.description = description;
        this.floatEvaluator = floatEvaluator;
        this.runtimeEquivalentBinding = Optional.empty();
        this.runtimeEquivalentRequiredExtension = Optional.empty();
    }

    Gesture(String description, Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator, Function<HandGestureTamarinProvidedState, Float> floatEvaluator, Map<HandSide, SuggestedBinding> runtimeEquivalentBinding, String runtimeEquivalentRequiredExtension) {
        this.description = description;
        this.booleanEvaluator = booleanEvaluator;
        this.floatEvaluator = floatEvaluator;
        this.runtimeEquivalentBinding = Optional.ofNullable(runtimeEquivalentBinding);
        this.runtimeEquivalentRequiredExtension = Optional.ofNullable(runtimeEquivalentRequiredExtension);
    }

    public Optional<SuggestedBinding> getRuntimeEquivalentBinding(HandSide handSide){
        return runtimeEquivalentBinding.map(m ->m.get(handSide));
    }
}

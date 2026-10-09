package com.onemillionworlds.tamarin.actions.gestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.SuggestedBinding;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
            aimState -> aimState.isPinching(Finger.MIDDLE)
    ),
    PINCH_RING(
            "Pinch Thumb and ring finger",
            state -> state.isPinching(Finger.RING),
            state -> state.getPinchStrength(Finger.RING),
            aimState -> aimState.isPinching(Finger.RING)
    ),
    PINCH_LITTLE_FINGER(
            "Pinch Thumb and little finger",
            state -> state.isPinching(Finger.LITTLE),
            state -> state.getPinchStrength(Finger.LITTLE),
            aimState -> aimState.isPinching(Finger.LITTLE)
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

    /**
     * How to read this gesture from the runtime's {@link HandTrackingAimState} (if the runtime equivalent is
     * XR_FB_hand_tracking_aim).
     */
    final Optional<Function<HandTrackingAimState, Boolean>> aimStateBooleanEvaluator;

    final Optional<String> runtimeEquivalentRequiredExtension;

    /**
     * A gesture that only Tamarin detects.
     */
    Gesture(String description, Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator, Function<HandGestureTamarinProvidedState, Float> floatEvaluator) {
        this.booleanEvaluator = booleanEvaluator;
        this.description = description;
        this.floatEvaluator = floatEvaluator;
        this.runtimeEquivalentBinding = Optional.empty();
        this.aimStateBooleanEvaluator = Optional.empty();
        this.runtimeEquivalentRequiredExtension = Optional.empty();
    }

    /**
     * A gesture the runtime can detect via a suggested binding (if the extension is loaded).
     */
    Gesture(String description, Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator, Function<HandGestureTamarinProvidedState, Float> floatEvaluator, Map<HandSide, SuggestedBinding> runtimeEquivalentBinding, String runtimeEquivalentRequiredExtension) {
        this.description = description;
        this.booleanEvaluator = booleanEvaluator;
        this.floatEvaluator = floatEvaluator;
        this.runtimeEquivalentBinding = Optional.of(runtimeEquivalentBinding);
        this.aimStateBooleanEvaluator = Optional.empty();
        this.runtimeEquivalentRequiredExtension = Optional.of(runtimeEquivalentRequiredExtension);
    }

    /**
     * A gesture the runtime can detect via XR_FB_hand_tracking_aim (if the extension is loaded).
     */
    Gesture(String description, Function<HandGestureTamarinProvidedState, Boolean> booleanEvaluator, Function<HandGestureTamarinProvidedState, Float> floatEvaluator, Function<HandTrackingAimState, Boolean> aimStateBooleanEvaluator) {
        this.description = description;
        this.booleanEvaluator = booleanEvaluator;
        this.floatEvaluator = floatEvaluator;
        this.runtimeEquivalentBinding = Optional.empty();
        this.aimStateBooleanEvaluator = Optional.of(aimStateBooleanEvaluator);
        this.runtimeEquivalentRequiredExtension = Optional.of(HandTrackingAimState.REQUIRED_EXTENSION);
    }

    public Optional<SuggestedBinding> getRuntimeEquivalentBinding(HandSide handSide){
        return runtimeEquivalentBinding.map(m ->m.get(handSide));
    }

    /**
     * If Tamarin should detect this gesture itself, which it does unless the runtime can.
     */
    boolean isDetectedByTamarin(Predicate<String> isExtensionLoaded){
        return !runtimeEquivalentRequiredExtension.filter(isExtensionLoaded).isPresent();
    }

    /**
     * If this gesture should be read from the runtime's {@link HandTrackingAimState}.
     */
    boolean isDetectedByAimState(Predicate<String> isExtensionLoaded){
        return aimStateBooleanEvaluator.isPresent() && isExtensionLoaded.test(HandTrackingAimState.REQUIRED_EXTENSION);
    }
}

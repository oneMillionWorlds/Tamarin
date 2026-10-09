package com.onemillionworlds.tamarin.handgestures;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.SuggestedBinding;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The built-in {@link HandGesture}s, for binding to actions with
 * {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}.
 * <p>
 *     Where a gesture has a runtime equivalent (currently only the index finger pinch, which is
 *     {@link HandInteractionExt}'s pinch) binding the gesture also suggests the runtime binding, and the runtime's
 *     version is used whenever the runtime supplies it. So applications just bind the gesture and get the best version
 *     available.
 * </p>
 */
public final class HandGestures{

    private static final Function<HandSide, List<SuggestedBinding>> NO_RUNTIME_EQUIVALENT = handSide -> List.of();

    private HandGestures(){}

    /**
     * The thumb pinching the given finger. Uses the runtime's pinch detection if available (see
     * {@link HandGestureSourcePreference}), otherwise Tamarin's.
     * <p>
     *     For the index finger this also suggests the {@link HandInteractionExt} pinch binding, which the runtime will
     *     then supply on runtimes that support it.
     * </p>
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public static HandGesture pinch(Finger finger){
        Finger.assertCanPinch(finger);
        return new BuiltInGesture("Thumb and " + finger.name().toLowerCase(Locale.ROOT) + " finger pinch",
                state -> state.getPinchStrength(finger),
                state -> state.isPinching(finger),
                handSide -> finger == Finger.INDEX ? List.of(indexPinchRuntimeBinding(handSide)) : List.of());
    }

    /**
     * The index finger pinch is in the cross-vendor hand interaction profile (other fingers aren't)
     */
    private static SuggestedBinding indexPinchRuntimeBinding(HandSide handSide){
        HandInteractionExt.BindingPathBuilderHand hand = handSide == HandSide.LEFT
                ? HandInteractionExt.pathBuilder().leftHand()
                : HandInteractionExt.pathBuilder().rightHand();
        return new SuggestedBinding(HandInteractionExt.PROFILE, hand.pinchValue());
    }

    /**
     * A closed fist (all four fingers curled with the thumb wrapped around them).
     */
    public static HandGesture fist(){
        return new BuiltInGesture("Fist", HandGestureState::getFistStrength, HandGestureState::isFist, NO_RUNTIME_EQUIVALENT);
    }

    /**
     * Pointing (index finger straight, the other fingers curled).
     */
    public static HandGesture point(){
        return new BuiltInGesture("Point", HandGestureState::getPointStrength, HandGestureState::isPointing, NO_RUNTIME_EQUIVALENT);
    }

    /**
     * A thumbs up (four fingers curled, thumb straight and pointing roughly up).
     */
    public static HandGesture thumbsUp(){
        return new BuiltInGesture("Thumbs up", HandGestureState::getThumbsUpStrength, HandGestureState::isThumbsUp, NO_RUNTIME_EQUIVALENT);
    }

    /**
     * The finger (or thumb) being curled. Active when it is mostly curled (default hysteresis thresholds).
     */
    public static HandGesture fingerCurl(Finger finger){
        return custom(finger.name().charAt(0) + finger.name().substring(1).toLowerCase(Locale.ROOT) + " curl",
                state -> state.getFingerCurl(finger), 0.8f, 0.6f);
    }

    /**
     * A custom gesture.
     *
     * @param description a short human-readable description (e.g. "Ok sign")
     * @param evaluator returns how strongly the gesture is being made, 0 to 1
     * @param activateThreshold the value at which the gesture turns on
     * @param deactivateThreshold the value below which the gesture turns off again
     */
    public static HandGesture custom(String description, Function<HandGestureState, Float> evaluator, float activateThreshold, float deactivateThreshold){
        if (deactivateThreshold > activateThreshold){
            throw new IllegalArgumentException("The deactivate threshold must not be greater than the activate threshold");
        }
        return new HandGesture(){
            @Override
            public float evaluate(HandGestureState state){
                return evaluator.apply(state);
            }

            @Override
            public float activateThreshold(){
                return activateThreshold;
            }

            @Override
            public float deactivateThreshold(){
                return deactivateThreshold;
            }

            @Override
            public String getDescription(){
                return description;
            }

            @Override
            public String toString(){
                return description;
            }
        };
    }

    /**
     * A gesture whose on/off state is already worked out (with hysteresis) in the {@link HandGestureState}.
     */
    private static final class BuiltInGesture implements HandGesture{
        private final String description;
        private final Function<HandGestureState, Float> strength;
        private final Function<HandGestureState, Boolean> active;
        private final Function<HandSide, List<SuggestedBinding>> runtimeEquivalents;

        private BuiltInGesture(String description, Function<HandGestureState, Float> strength, Function<HandGestureState, Boolean> active, Function<HandSide, List<SuggestedBinding>> runtimeEquivalents){
            this.description = description;
            this.strength = strength;
            this.active = active;
            this.runtimeEquivalents = runtimeEquivalents;
        }

        @Override
        public List<SuggestedBinding> getRuntimeEquivalentBindings(HandSide handSide){
            return runtimeEquivalents.apply(handSide);
        }

        @Override
        public float evaluate(HandGestureState state){
            return strength.apply(state);
        }

        @Override
        public boolean isActive(HandGestureState state, float value, boolean wasActive){
            return active.apply(state);
        }

        @Override
        public String getDescription(){
            return description;
        }

        @Override
        public String toString(){
            return description;
        }
    }
}

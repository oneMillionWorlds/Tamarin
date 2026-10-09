package com.onemillionworlds.tamarin.handgestures;

import java.util.Locale;
import java.util.function.Function;

/**
 * The built-in {@link HandGesture}s, for binding to actions with
 * {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}.
 * <p>
 *     Prefer the runtime's own bindings where one exists (e.g. {@link com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt}
 *     pinch and grasp), as they appear in the runtime's rebinding UI and are well tuned. These are for gestures the
 *     action system can't express.
 * </p>
 */
public final class HandGestures{

    private HandGestures(){}

    /**
     * The thumb pinching the given finger. Uses the runtime's pinch detection if available (see
     * {@link HandGestureSourcePreference}), otherwise Tamarin's.
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public static HandGesture pinch(Finger finger){
        Finger.assertCanPinch(finger);
        return new BuiltInGesture("Thumb and " + finger.name().toLowerCase(Locale.ROOT) + " finger pinch",
                state -> state.getPinchStrength(finger),
                (state) -> state.isPinching(finger));
    }

    /**
     * A closed fist (all four fingers curled with the thumb wrapped around them).
     */
    public static HandGesture fist(){
        return new BuiltInGesture("Fist", HandGestureState::getFistStrength, HandGestureState::isFist);
    }

    /**
     * Pointing (index finger straight, the other fingers curled).
     */
    public static HandGesture point(){
        return new BuiltInGesture("Point", HandGestureState::getPointStrength, HandGestureState::isPointing);
    }

    /**
     * A thumbs up (four fingers curled, thumb straight and pointing roughly up).
     */
    public static HandGesture thumbsUp(){
        return new BuiltInGesture("Thumbs up", HandGestureState::getThumbsUpStrength, HandGestureState::isThumbsUp);
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

        private BuiltInGesture(String description, Function<HandGestureState, Float> strength, Function<HandGestureState, Boolean> active){
            this.description = description;
            this.strength = strength;
            this.active = active;
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

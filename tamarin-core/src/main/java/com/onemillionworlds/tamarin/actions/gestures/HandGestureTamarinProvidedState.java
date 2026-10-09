package com.onemillionworlds.tamarin.actions.gestures;

/**
 * The gestures a hand is making, as detected by Tamarin from the hand's joint positions. Each gesture has a strength
 * (0 to 1, how close the hand is to making it) and an on/off state (with hysteresis, so it doesn't flicker).
 */
public final class HandGestureTamarinProvidedState {

    /*
     * indexed by Finger ordinal - 1 (the thumb can't pinch itself)
     */
    private final float[] pinchStrengths;
    private final boolean[] pinching;

    private final float fistStrength;
    private final boolean fist;
    private final float pointStrength;
    private final boolean pointing;
    private final float thumbsUpStrength;
    private final boolean thumbsUp;

    private HandGestureTamarinProvidedState(Builder builder){
        this.pinchStrengths = builder.pinchStrengths.clone();
        this.pinching = builder.pinching.clone();
        this.fistStrength = builder.fistStrength;
        this.fist = builder.fist;
        this.pointStrength = builder.pointStrength;
        this.pointing = builder.pointing;
        this.thumbsUpStrength = builder.thumbsUpStrength;
        this.thumbsUp = builder.thumbsUp;
    }

    /**
     * How strongly the thumb is pinching the given finger. Range 0 to 1, with 1 meaning the finger and thumb are
     * touching.
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public float getPinchStrength(Finger finger){
        Finger.assertCanPinch(finger);
        return pinchStrengths[finger.ordinal() - 1];
    }

    /**
     * If the thumb is pinching the given finger (on/off, with hysteresis). Prefer this over applying your own
     * threshold to {@link #getPinchStrength}.
     * <p>
     *     Pinches are exclusive: a finger is only pinching if no other finger is pinching or nearly pinching. If two
     *     are (e.g. all the fingers bunched together against the thumb) neither counts. The strengths are not
     *     exclusive.
     * </p>
     * <p>
     *     Note that pinching the ring and little fingers is physically awkward, prefer the index and middle fingers
     *     for anything common.
     * </p>
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public boolean isPinching(Finger finger){
        Finger.assertCanPinch(finger);
        return pinching[finger.ordinal() - 1];
    }

    /**
     * How much the hand is a closed fist (all four fingers curled with the thumb folded over them), 0 to 1.
     */
    public float getFistStrength(){
        return fistStrength;
    }

    /**
     * If the hand is a closed fist (on/off, with hysteresis).
     */
    public boolean isFist(){
        return fist;
    }

    /**
     * How much the hand is pointing (index finger straight, the middle, ring and little fingers curled. The thumb is
     * ignored), 0 to 1.
     */
    public float getPointStrength(){
        return pointStrength;
    }

    /**
     * If the hand is pointing (on/off, with hysteresis).
     */
    public boolean isPointing(){
        return pointing;
    }

    /**
     * How much the hand is giving a thumbs up (four fingers curled, thumb straight and pointing roughly up), 0 to 1.
     * Up is the user's real world up (the stage's up), not the game world's, so this still works if the observer has
     * been tilted.
     */
    public float getThumbsUpStrength(){
        return thumbsUpStrength;
    }

    /**
     * If the hand is giving a thumbs up (on/off, with hysteresis).
     */
    public boolean isThumbsUp(){
        return thumbsUp;
    }

    /**
     * Primarily for internal use (and testing). Anything not set defaults to zero/false.
     */
    public static Builder builder(){
        return new Builder();
    }

    /**
     * Primarily for internal use (and testing). Anything not set defaults to zero/false.
     */
    public static final class Builder{
        private final float[] pinchStrengths = new float[Finger.PINCHING_FINGERS.length];
        private final boolean[] pinching = new boolean[Finger.PINCHING_FINGERS.length];
        private float fistStrength;
        private boolean fist;
        private float pointStrength;
        private boolean pointing;
        private float thumbsUpStrength;
        private boolean thumbsUp;

        private Builder(){
        }

        public Builder pinch(Finger finger, float strength, boolean isPinching){
            Finger.assertCanPinch(finger);
            pinchStrengths[finger.ordinal() - 1] = strength;
            pinching[finger.ordinal() - 1] = isPinching;
            return this;
        }

        public Builder fist(float strength, boolean isFist){
            this.fistStrength = strength;
            this.fist = isFist;
            return this;
        }

        public Builder point(float strength, boolean isPointing){
            this.pointStrength = strength;
            this.pointing = isPointing;
            return this;
        }

        public Builder thumbsUp(float strength, boolean isThumbsUp){
            this.thumbsUpStrength = strength;
            this.thumbsUp = isThumbsUp;
            return this;
        }

        public HandGestureTamarinProvidedState build(){
            return new HandGestureTamarinProvidedState(this);
        }
    }

}

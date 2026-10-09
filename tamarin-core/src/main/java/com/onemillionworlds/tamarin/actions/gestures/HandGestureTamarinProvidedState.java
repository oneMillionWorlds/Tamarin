package com.onemillionworlds.tamarin.actions.gestures;


import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Map;

/**
 * Using Tamarins own data determines the strength of inupts to a gesture
 * <p>
 *     All positions and directions are in world coordinates.
 * </p>
 */
public final class HandGestureTamarinProvidedState {

    private final HandSide handSide;
    /*
     * indexed by Finger ordinal - 1 (the thumb can't pinch itself)
     */
    private final float[] pinchStrengths;
    private final boolean[] pinching;

    private final boolean systemGestureActive;
    private final boolean menuPressed;
    private final boolean dominantHand;

    /*
     * indexed by Finger ordinal
     */
    private final float[] fingerCurls;

    private final float fistStrength;
    private final boolean fist;
    private final float pointStrength;
    private final boolean pointing;
    private final float thumbsUpStrength;
    private final boolean thumbsUp;

    private final Vector3f palmPosition;
    private final Vector3f palmNormal;

    private final Map<HandJoint, BonePose> joints;

    private HandGestureTamarinProvidedState(Builder builder){
        this.handSide = builder.handSide;
        this.pinchStrengths = builder.pinchStrengths.clone();
        this.pinching = builder.pinching.clone();
        this.systemGestureActive = builder.systemGestureActive;
        this.menuPressed = builder.menuPressed;
        this.dominantHand = builder.dominantHand;
        this.fingerCurls = builder.fingerCurls.clone();
        this.fistStrength = builder.fistStrength;
        this.fist = builder.fist;
        this.pointStrength = builder.pointStrength;
        this.pointing = builder.pointing;
        this.thumbsUpStrength = builder.thumbsUpStrength;
        this.thumbsUp = builder.thumbsUp;
        this.palmPosition = builder.palmPosition;
        this.palmNormal = builder.palmNormal;
        this.joints = builder.joints;
    }

    public HandSide getHandSide(){
        return handSide;
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
     *     Note that pinching the ring and little fingers is physically awkward, and the runtime (on Quest) only
     *     recognises them with a very clear pinch. Prefer the index and middle fingers for anything common.
     * </p>
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public boolean isPinching(Finger finger){
        Finger.assertCanPinch(finger);
        return pinching[finger.ordinal() - 1];
    }

    /**
     * RUNTIME SOURCE ONLY (always false otherwise). If the system gesture (on Quest: looking at the palm and pinching)
     * is in progress. While it is, applications should probably ignore the other gestures on this hand.
     */
    public boolean isSystemGestureActive(){
        return systemGestureActive;
    }

    /**
     * RUNTIME SOURCE ONLY (always false otherwise). If the system menu gesture is active (the hand tracking
     * equivalent of a controller's menu button).
     */
    public boolean isMenuPressed(){
        return menuPressed;
    }

    /**
     * RUNTIME SOURCE ONLY (always false otherwise). If the system considers this hand to be the user's dominant hand.
     */
    public boolean isDominantHand(){
        return dominantHand;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). How curled the finger (or thumb) is. 0 is straight, 1 is fully curled
     * (as in a fist).
     */
    public float getFingerCurl(Finger finger){
        return fingerCurls[finger.ordinal()];
    }

    /**
     * TAMARIN DETECTED (on all runtimes). How much the hand is a closed fist (all four fingers curled with the thumb
     * wrapped around them), 0 to 1.
     */
    public float getFistStrength(){
        return fistStrength;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). If the hand is a closed fist (on/off, with hysteresis).
     */
    public boolean isFist(){
        return fist;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). How much the hand is pointing (index finger straight, the middle, ring and
     * little fingers curled. The thumb is ignored), 0 to 1.
     */
    public float getPointStrength(){
        return pointStrength;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). If the hand is pointing (on/off, with hysteresis).
     */
    public boolean isPointing(){
        return pointing;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). How much the hand is giving a thumbs up (four fingers curled, thumb straight
     * and pointing roughly up in the world), 0 to 1.
     */
    public float getThumbsUpStrength(){
        return thumbsUpStrength;
    }

    /**
     * TAMARIN DETECTED (on all runtimes). If the hand is giving a thumbs up (on/off, with hysteresis).
     */
    public boolean isThumbsUp(){
        return thumbsUp;
    }

    /**
     * The centre of the palm (in world coordinates).
     */
    public Vector3f getPalmPosition(){
        return palmPosition;
    }

    /**
     * A unit vector pointing out of the palm (in world coordinates).
     */
    public Vector3f getPalmNormal(){
        return palmNormal;
    }

    /**
     * How much the palm is facing in the given direction. 1 is facing exactly that way, 0 is at right angles to it
     * or facing away.
     * <p>
     *     For example, to see if the user is looking at their palm (e.g. to open a wrist menu) use
     *     {@code palmFacingAmount(headPosition.subtract(getPalmPosition()))}.
     * </p>
     *
     * @param direction the direction (in world coordinates), doesn't need to be normalised
     */
    public float palmFacingAmount(Vector3f direction){
        if (direction.lengthSquared() == 0){
            return 0;
        }
        return FastMath.clamp(palmNormal.dot(direction.normalize()), 0, 1);
    }

    /**
     * The positions (in world coordinates) of all the hand's joints, for custom gesture recognition.
     */
    public Map<HandJoint, BonePose> getJoints(){
        return joints;
    }


    /**
     * Primarily for internal use (and testing). Anything not set defaults to zero/false/empty.
     */
    public static final class Builder{
        private final HandSide handSide;
        private final float[] pinchStrengths = new float[Finger.PINCHING_FINGERS.length];
        private final boolean[] pinching = new boolean[Finger.PINCHING_FINGERS.length];
        private boolean systemGestureActive;
        private boolean menuPressed;
        private boolean dominantHand;
        private final float[] fingerCurls = new float[Finger.values().length];
        private float fistStrength;
        private boolean fist;
        private float pointStrength;
        private boolean pointing;
        private float thumbsUpStrength;
        private boolean thumbsUp;
        private Vector3f palmPosition = new Vector3f();
        private Vector3f palmNormal = new Vector3f(0, 1, 0);
        private Map<HandJoint, BonePose> joints = Map.of();

        private Builder(HandSide handSide){
            this.handSide = handSide;
        }

        public Builder pinch(Finger finger, float strength, boolean isPinching){
            Finger.assertCanPinch(finger);
            pinchStrengths[finger.ordinal() - 1] = strength;
            pinching[finger.ordinal() - 1] = isPinching;
            return this;
        }

        public Builder runtimeFlags(boolean systemGestureActive, boolean menuPressed, boolean dominantHand){
            this.systemGestureActive = systemGestureActive;
            this.menuPressed = menuPressed;
            this.dominantHand = dominantHand;
            return this;
        }

        public Builder fingerCurl(Finger finger, float curl){
            fingerCurls[finger.ordinal()] = curl;
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

        public Builder palm(Vector3f position, Vector3f normal){
            this.palmPosition = position;
            this.palmNormal = normal;
            return this;
        }

        public Builder joints(Map<HandJoint, BonePose> joints){
            this.joints = joints;
            return this;
        }

        public HandGestureTamarinProvidedState build(){
            return new HandGestureTamarinProvidedState(this);
        }
    }

}
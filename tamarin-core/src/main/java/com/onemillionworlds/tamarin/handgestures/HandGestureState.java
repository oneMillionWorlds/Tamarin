package com.onemillionworlds.tamarin.handgestures;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.util.Map;

/**
 * The gestures a tracked bare hand (hand tracking, not a controller) is making this frame. Obtain it from
 * {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getHandGestureState(HandSide)} each frame.
 * <p>
 *     The same API is used whichever runtime the application is on:
 * </p>
 * <ul>
 *     <li><b>Pinches and aim</b> come from the runtime's own gesture recognition if it is available (currently
 *     Meta runtimes, via XR_FB_hand_tracking_aim), otherwise Tamarin works them out from the hand joints. See
 *     {@link #getSource()} and {@link HandGestureSourcePreference}.</li>
 *     <li><b>Everything else</b> (finger curl, fist, point, thumbs up, palm normal) is always worked out by Tamarin
 *     from the hand joints, as no runtime provides them.</li>
 *     <li>{@link #isSystemGestureActive()}, {@link #isMenuPressed()} and {@link #isDominantHand()} are only known
 *     when the source is {@link HandGestureSource#RUNTIME}, otherwise they are always false.</li>
 * </ul>
 * <p>
 *     Each gesture has a continuous "strength" from 0 to 1 (good for driving things smoothly, or for showing the user
 *     that a gesture is nearly complete) and an on/off "is" version (good for triggering things). The on/off versions
 *     have hysteresis so they don't flicker. For edge detection ("just started pinching") either compare with the
 *     previous frame or bind the gesture to an action (see
 *     {@link com.onemillionworlds.tamarin.actions.actionprofile.Action.ActionBuilder#withGestureBinding}) and use
 *     {@link com.onemillionworlds.tamarin.actions.state.BooleanActionState#hasChangedToTrue()}.
 * </p>
 * <p>
 *     All positions and directions are in world coordinates.
 * </p>
 */
public final class HandGestureState{

    private final HandSide handSide;
    private final HandGestureSource source;

    /*
     * indexed by Finger ordinal - 1 (the thumb can't pinch itself)
     */
    private final float[] pinchStrengths;
    private final boolean[] pinching;

    private final boolean aimValid;
    private final Vector3f aimPosition;
    private final Quaternion aimOrientation;

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

    private HandGestureState(Builder builder){
        this.handSide = builder.handSide;
        this.source = builder.source;
        this.pinchStrengths = builder.pinchStrengths.clone();
        this.pinching = builder.pinching.clone();
        this.aimValid = builder.aimValid;
        this.aimPosition = builder.aimPosition;
        this.aimOrientation = builder.aimOrientation;
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
     * Where the pinch and aim information came from (the runtime, or Tamarin's own detection from the hand joints).
     */
    public HandGestureSource getSource(){
        return source;
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
     *     Pinches are exclusive: at most one finger is pinching at a time, and if it is ambiguous which finger the
     *     thumb is pinching (e.g. all the fingers bunched together against the thumb) none are. The strengths are not
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
     * If {@link #getAimPosition()} and {@link #getAimOrientation()} are valid. If false they should not be used.
     */
    public boolean isAimValid(){
        return aimValid;
    }

    /**
     * The start of a stabilised pointing ray for the hand (in world coordinates). With a {@link HandGestureSource#RUNTIME}
     * source this is the runtime's own aim, with {@link HandGestureSource#TAMARIN} it is estimated as a ray from the
     * shoulder through the base of the index finger. Only meaningful if {@link #isAimValid()}.
     */
    public Vector3f getAimPosition(){
        return aimPosition;
    }

    /**
     * The orientation of the stabilised pointing ray (in world coordinates). The ray points along the negative z axis
     * of this rotation (the OpenXR convention). Only meaningful if {@link #isAimValid()}.
     */
    public Quaternion getAimOrientation(){
        return aimOrientation;
    }

    /**
     * The direction of the stabilised pointing ray (in world coordinates). Only meaningful if {@link #isAimValid()}.
     */
    public Vector3f getAimDirection(){
        return aimOrientation.mult(Vector3f.UNIT_Z.negate());
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
     * Primarily for internal use (and testing).
     */
    public static Builder builder(HandSide handSide, HandGestureSource source){
        return new Builder(handSide, source);
    }

    /**
     * Primarily for internal use (and testing). Anything not set defaults to zero/false/empty.
     */
    public static final class Builder{
        private final HandSide handSide;
        private final HandGestureSource source;
        private final float[] pinchStrengths = new float[Finger.PINCHING_FINGERS.length];
        private final boolean[] pinching = new boolean[Finger.PINCHING_FINGERS.length];
        private boolean aimValid = false;
        private Vector3f aimPosition = new Vector3f();
        private Quaternion aimOrientation = new Quaternion();
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

        private Builder(HandSide handSide, HandGestureSource source){
            this.handSide = handSide;
            this.source = source;
        }

        public Builder pinch(Finger finger, float strength, boolean isPinching){
            Finger.assertCanPinch(finger);
            pinchStrengths[finger.ordinal() - 1] = strength;
            pinching[finger.ordinal() - 1] = isPinching;
            return this;
        }

        public Builder aim(Vector3f position, Quaternion orientation){
            this.aimValid = true;
            this.aimPosition = position;
            this.aimOrientation = orientation;
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

        public HandGestureState build(){
            return new HandGestureState(this);
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("HandGestureState{" + handSide + ", source=" + source);
        for(Finger finger : Finger.PINCHING_FINGERS){
            sb.append(", ").append(finger).append(" pinch=").append(getPinchStrength(finger)).append(isPinching(finger) ? " (pinching)" : "");
        }
        sb.append(", fist=").append(fistStrength).append(fist ? " (fist)" : "");
        sb.append(", point=").append(pointStrength).append(pointing ? " (pointing)" : "");
        sb.append(", thumbsUp=").append(thumbsUpStrength).append(thumbsUp ? " (thumbs up)" : "");
        return sb.append('}').toString();
    }
}

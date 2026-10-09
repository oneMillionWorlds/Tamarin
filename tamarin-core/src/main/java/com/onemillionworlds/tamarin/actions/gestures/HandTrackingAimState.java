package com.onemillionworlds.tamarin.actions.gestures;

/**
 * The runtime's own gesture recognition for a hand, from the XR_FB_hand_tracking_aim extension (Meta runtimes only).
 * Obtained from {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#getHandTrackingAimState}.
 */
public class HandTrackingAimState{

    public static final String REQUIRED_EXTENSION = "XR_FB_hand_tracking_aim";

    /*
     * These are the XrHandTrackingAimFlagBitsFB values from the OpenXR spec.
     */
    static final long VALID_BIT = 0x00000002L;
    static final long INDEX_PINCHING_BIT = 0x00000004L;
    static final long MIDDLE_PINCHING_BIT = 0x00000008L;
    static final long RING_PINCHING_BIT = 0x00000010L;
    static final long LITTLE_PINCHING_BIT = 0x00000020L;
    static final long SYSTEM_GESTURE_BIT = 0x00000040L;
    static final long DOMINANT_HAND_BIT = 0x00000080L;
    static final long MENU_PRESSED_BIT = 0x00000100L;

    private final long status;

    private final float pinchStrengthIndex;
    private final float pinchStrengthMiddle;
    private final float pinchStrengthRing;
    private final float pinchStrengthLittle;

    /**
     * Primarily for internal use (or for testing).
     *
     * @param status the raw XrHandTrackingAimFlagsFB bitmask
     */
    public HandTrackingAimState(long status, float pinchStrengthIndex, float pinchStrengthMiddle, float pinchStrengthRing, float pinchStrengthLittle){
        this.status = status;
        this.pinchStrengthIndex = pinchStrengthIndex;
        this.pinchStrengthMiddle = pinchStrengthMiddle;
        this.pinchStrengthRing = pinchStrengthRing;
        this.pinchStrengthLittle = pinchStrengthLittle;
    }

    /**
     * Primarily for internal use. If the runtime says this state is valid (if not the other values should be
     * ignored).
     */
    public boolean isValid(){
        return hasFlag(VALID_BIT);
    }

    /**
     * How strongly the thumb is pinching the given finger. Range 0 to 1, with 1 meaning the finger and thumb
     * are fully touching.
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public float getPinchStrength(Finger finger){
        switch(finger){
            case INDEX: return pinchStrengthIndex;
            case MIDDLE: return pinchStrengthMiddle;
            case RING: return pinchStrengthRing;
            case LITTLE: return pinchStrengthLittle;
            default: throw new IllegalArgumentException("The thumb can't pinch against itself, use INDEX, MIDDLE, RING or LITTLE");
        }
    }

    /**
     * The runtime's own discrete (on/off) decision on whether the thumb is pinching the given finger. Prefer this
     * over applying a threshold to {@link #getPinchStrength} because the runtime tunes it (and handles hysteresis).
     * Note that (in testing on a Quest) the ring and little finger flags need a very clear, deliberate pinch with the
     * other fingers held out of the way.
     *
     * @param finger INDEX, MIDDLE, RING or LITTLE (not THUMB)
     */
    public boolean isPinching(Finger finger){
        switch(finger){
            case INDEX: return hasFlag(INDEX_PINCHING_BIT);
            case MIDDLE: return hasFlag(MIDDLE_PINCHING_BIT);
            case RING: return hasFlag(RING_PINCHING_BIT);
            case LITTLE: return hasFlag(LITTLE_PINCHING_BIT);
            default: throw new IllegalArgumentException("The thumb can't pinch against itself, use INDEX, MIDDLE, RING or LITTLE");
        }
    }

    /**
     * If the system gesture (on Quest: looking at the palm and pinching) is in progress. While it is applications
     * should probably ignore the other gestures on this hand.
     */
    public boolean isSystemGestureActive(){
        return hasFlag(SYSTEM_GESTURE_BIT);
    }

    /**
     * If the system considers this hand to be the user's dominant hand.
     */
    public boolean isDominantHand(){
        return hasFlag(DOMINANT_HAND_BIT);
    }

    /**
     * If the system menu gesture is active (the hand tracking equivalent of a controller's menu button).
     */
    public boolean isMenuPressed(){
        return hasFlag(MENU_PRESSED_BIT);
    }

    private boolean hasFlag(long flag){
        return (status & flag) != 0;
    }

    @Override
    public String toString(){
        return "HandTrackingAimState{" +
                "status=" + Long.toBinaryString(status) +
                ", pinchStrengthIndex=" + pinchStrengthIndex +
                ", pinchStrengthMiddle=" + pinchStrengthMiddle +
                ", pinchStrengthRing=" + pinchStrengthRing +
                ", pinchStrengthLittle=" + pinchStrengthLittle +
                '}';
    }
}

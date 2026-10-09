package com.onemillionworlds.tamarin.actions.gestures;

/**
 * Turns a continuous value (e.g. a gesture strength from 0 to 1) into an on/off state without flickering. It turns on
 * when the value reaches the activate threshold, and only turns off again when the value drops below the (lower)
 * deactivate threshold.
 * <p>
 *     Keep one instance per thing being tracked and call {@link #update(float)} once per frame.
 * </p>
 */
public class Hysteresis {

    private final float activateThreshold;
    private final float deactivateThreshold;

    private boolean active = false;

    /**
     * @param activateThreshold the value at (or above) which the state becomes active
     * @param deactivateThreshold the value below which the state becomes inactive again. Must not be greater than
     *                            the activate threshold
     */
    public Hysteresis(float activateThreshold, float deactivateThreshold){
        if (deactivateThreshold > activateThreshold){
            throw new IllegalArgumentException("The deactivate threshold (" + deactivateThreshold + ") must not be greater than the activate threshold (" + activateThreshold + ")");
        }
        this.activateThreshold = activateThreshold;
        this.deactivateThreshold = deactivateThreshold;
    }

    /**
     * @param value the latest value
     * @return if the state is now active
     */
    public boolean update(float value){
        active = isActive(value, active, activateThreshold, deactivateThreshold);
        return active;
    }

    /**
     * Forces the state to be inactive (e.g. because the hand has stopped being tracked).
     */
    public void reset(){
        active = false;
    }

    public boolean isActive(){
        return active;
    }

    public float getActivateThreshold(){
        return activateThreshold;
    }

    public float getDeactivateThreshold(){
        return deactivateThreshold;
    }

    /**
     * The stateless version of the hysteresis rule.
     */
    public static boolean isActive(float value, boolean wasActive, float activateThreshold, float deactivateThreshold){
        return wasActive ? value >= deactivateThreshold : value >= activateThreshold;
    }
}

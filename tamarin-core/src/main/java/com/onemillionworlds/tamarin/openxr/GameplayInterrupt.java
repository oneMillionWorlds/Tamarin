package com.onemillionworlds.tamarin.openxr;

import com.onemillionworlds.tamarin.actions.HandSide;

import java.util.Objects;
import java.util.Optional;

/**
 * Something that has interrupted (or stopped interrupting) the user's gameplay, e.g. they've opened the system menu,
 * taken the headset off or a controller's battery has died. See {@link XrBaseAppState#subscribeToGameplayInterrupts()}.
 * <p>
 *     Each interrupt is reported once when it {@link Phase#STARTED starts} and once when it {@link Phase#ENDED ends}
 *     (e.g. the headset is put back on). The intention is that the application pauses on a STARTED interrupt and lets
 *     the user decide when to resume (e.g. via a "Continue" button), optionally using ENDED interrupts to update what it shows.
 * </p>
 */
public final class GameplayInterrupt{

    public enum Type{
        /**
         * The session has lost focus, so the application isn't receiving input. Typically because the user has
         * opened the system menu (e.g. the Quest universal menu or the SteamVR dashboard).
         */
        SESSION_NOT_FOCUSED,
        /**
         * The user has taken off the headset (requires the XR_EXT_user_presence extension, see
         * {@link XrBaseAppState#isUserPresent()}).
         */
        HEADSET_REMOVED,
        /**
         * A controller that was in use has gone, e.g. its battery has died, it has been turned off, or (if the
         * application doesn't support hand tracking) the user has put it down. {@link #getHandSide()} says which.
         * See {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#subscribeToControllerLost()} for details.
         */
        CONTROLLER_LOST
    }

    public enum Phase{
        /**
         * The interrupt has begun (e.g. the headset has been taken off)
         */
        STARTED,
        /**
         * The cause of the interrupt has gone away (e.g. the headset has been put back on, the lost controller has
         * come back). Note that the application may want to keep the game paused until the user chooses to resume.
         */
        ENDED
    }

    private final Type type;
    private final Phase phase;
    private final HandSide handSide;

    private GameplayInterrupt(Type type, Phase phase, HandSide handSide){
        this.type = Objects.requireNonNull(type);
        this.phase = Objects.requireNonNull(phase);
        this.handSide = handSide;
    }

    public static GameplayInterrupt sessionNotFocused(Phase phase){
        return new GameplayInterrupt(Type.SESSION_NOT_FOCUSED, phase, null);
    }

    public static GameplayInterrupt headsetRemoved(Phase phase){
        return new GameplayInterrupt(Type.HEADSET_REMOVED, phase, null);
    }

    public static GameplayInterrupt controllerLost(Phase phase, HandSide handSide){
        return new GameplayInterrupt(Type.CONTROLLER_LOST, phase, Objects.requireNonNull(handSide));
    }

    public Type getType(){
        return type;
    }

    public Phase getPhase(){
        return phase;
    }

    public boolean isStarted(){
        return phase == Phase.STARTED;
    }

    public boolean isEnded(){
        return phase == Phase.ENDED;
    }

    /**
     * The hand the interrupt relates to. Only present for {@link Type#CONTROLLER_LOST}.
     */
    public Optional<HandSide> getHandSide(){
        return Optional.ofNullable(handSide);
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameplayInterrupt that = (GameplayInterrupt) o;
        return type == that.type && phase == that.phase && handSide == that.handSide;
    }

    @Override
    public int hashCode(){
        return Objects.hash(type, phase, handSide);
    }

    @Override
    public String toString(){
        return type + (handSide == null ? "" : "(" + handSide + ")") + " " + phase;
    }
}

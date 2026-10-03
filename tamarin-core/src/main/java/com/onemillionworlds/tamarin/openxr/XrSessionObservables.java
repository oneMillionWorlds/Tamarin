package com.onemillionworlds.tamarin.openxr;

import com.onemillionworlds.tamarin.observable.ObservableDataEvent;
import com.onemillionworlds.tamarin.observable.ObservableDataEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableValue;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Holds the observable state that an OpenXR session manager reports to the rest of Tamarin (and the application).
 * <p>
 *     This is shared between the desktop and android session managers so they report events in the same way.
 * </p>
 */
public class XrSessionObservables{

    private static final Logger LOGGER = Logger.getLogger(XrSessionObservables.class.getName());

    private final ObservableValue<SessionState> sessionState = new ObservableValue<>(SessionState.UNKNOWN);

    private final ObservableValue<Boolean> sessionFocused = new ObservableValue<>(false);

    /**
     * Assumed true unless the runtime (via XR_EXT_user_presence) tells us otherwise.
     */
    private final ObservableValue<Boolean> userPresent = new ObservableValue<>(true);

    /**
     * True when the session isn't focused or the user isn't present.
     */
    private final ObservableValue<Boolean> shouldPause = new ObservableValue<>(true);

    private final ObservableEvent referenceSpaceChangePending = new ObservableEvent();

    private final ObservableEvent interactionProfileChanged = new ObservableEvent();

    private final ObservableDataEvent<GameplayInterrupt> gameplayInterrupts = new ObservableDataEvent<>();

    /**
     * Focus changes are only interrupts once the session has been focused at least once (start up isn't an interrupt)
     */
    private boolean hasEverBeenFocused = false;

    public SessionState getSessionState(){
        return sessionState.get();
    }

    public boolean isSessionFocused(){
        return sessionFocused.get();
    }

    public boolean isUserPresent(){
        return userPresent.get();
    }

    public boolean shouldPause(){
        return shouldPause.get();
    }

    /**
     * Updates the session state (and the derived focus and should pause states). Subscribers are only notified if the
     * value actually changes.
     */
    public void setSessionState(SessionState newState){
        setIfChanged(sessionState, newState);
        boolean focused = newState == SessionState.FOCUSED;
        if (setIfChanged(sessionFocused, focused)){
            if (focused && !hasEverBeenFocused){
                // the session becoming focused at start up isn't the end of an interrupt
                hasEverBeenFocused = true;
            } else if (hasEverBeenFocused){
                fireGameplayInterrupt(GameplayInterrupt.sessionNotFocused(focused ? GameplayInterrupt.Phase.ENDED : GameplayInterrupt.Phase.STARTED));
            }
        }
        updateShouldPause();
    }

    /**
     * Updates whether the user is present (i.e. wearing the headset). Subscribers are only notified if the value
     * actually changes.
     */
    public void setUserPresent(boolean present){
        if (setIfChanged(userPresent, present)){
            fireGameplayInterrupt(GameplayInterrupt.headsetRemoved(present ? GameplayInterrupt.Phase.ENDED : GameplayInterrupt.Phase.STARTED));
        }
        updateShouldPause();
    }

    /**
     * Reports a gameplay interrupt to subscribers of {@link #subscribeToGameplayInterrupts()}. Session and presence
     * interrupts are fired automatically by this class, controller interrupts are reported by the action state.
     */
    public void fireGameplayInterrupt(GameplayInterrupt interrupt){
        LOGGER.info("Gameplay interrupt: " + interrupt);
        gameplayInterrupts.fireEvent(interrupt);
    }

    public ObservableDataEventSubscription<GameplayInterrupt> subscribeToGameplayInterrupts(){
        return gameplayInterrupts.subscribe();
    }

    private void updateShouldPause(){
        setIfChanged(shouldPause, !(sessionFocused.get() && userPresent.get()));
    }

    /**
     * @return true if the value changed
     */
    private static <T> boolean setIfChanged(ObservableValue<T> observableValue, T newValue){
        if (!Objects.equals(observableValue.get(), newValue)){
            observableValue.set(newValue);
            return true;
        }
        return false;
    }

    public void fireReferenceSpaceChangePending(){
        referenceSpaceChangePending.fireEvent();
    }

    public void fireInteractionProfileChanged(){
        interactionProfileChanged.fireEvent();
    }

    public ObservableValueSubscription<SessionState> subscribeToSessionState(){
        return sessionState.subscribe();
    }

    public ObservableValueSubscription<Boolean> subscribeToSessionFocused(){
        return sessionFocused.subscribe();
    }

    public ObservableValueSubscription<Boolean> subscribeToUserPresent(){
        return userPresent.subscribe();
    }

    public ObservableValueSubscription<Boolean> subscribeToShouldPause(){
        return shouldPause.subscribe();
    }

    public ObservableEventSubscription subscribeToReferenceSpaceChangePending(){
        return referenceSpaceChangePending.subscribe();
    }

    /**
     * Note that this is a raw notification that the runtime has changed the interaction profile. Applications
     * probably want {@link com.onemillionworlds.tamarin.actions.XrActionBaseAppState#subscribeToInteractionProfileChanges()}
     * which fires after the new profiles have been fetched.
     */
    public ObservableEventSubscription subscribeToInteractionProfileChanged(){
        return interactionProfileChanged.subscribe();
    }
}

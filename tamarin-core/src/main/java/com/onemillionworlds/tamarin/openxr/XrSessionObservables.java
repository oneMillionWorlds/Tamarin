package com.onemillionworlds.tamarin.openxr;

import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableValue;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;

import java.util.Objects;

/**
 * Holds the observable state that an OpenXR session manager reports to the rest of Tamarin (and the application).
 * <p>
 *     This is shared between the desktop and android session managers so they report events in the same way.
 * </p>
 */
public class XrSessionObservables{

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
        setIfChanged(sessionFocused, newState == SessionState.FOCUSED);
        updateShouldPause();
    }

    /**
     * Updates whether the user is present (i.e. wearing the headset). Subscribers are only notified if the value
     * actually changes.
     */
    public void setUserPresent(boolean present){
        setIfChanged(userPresent, present);
        updateShouldPause();
    }

    private void updateShouldPause(){
        setIfChanged(shouldPause, !(sessionFocused.get() && userPresent.get()));
    }

    private static <T> void setIfChanged(ObservableValue<T> observableValue, T newValue){
        if (!Objects.equals(observableValue.get(), newValue)){
            observableValue.set(newValue);
        }
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

package com.onemillionworlds.tamarin.openxr;

import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableValue;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;

/**
 * Holds the observable state that an OpenXR session manager reports to the rest of Tamarin (and the application).
 * <p>
 *     This is shared between the desktop and android session managers so they report events in the same way.
 * </p>
 */
public class XrSessionObservables{

    private final ObservableValue<SessionState> sessionState = new ObservableValue<>(SessionState.UNKNOWN);

    private final ObservableValue<Boolean> sessionFocused = new ObservableValue<>(false);

    private final ObservableEvent referenceSpaceChangePending = new ObservableEvent();

    private final ObservableEvent interactionProfileChanged = new ObservableEvent();

    public SessionState getSessionState(){
        return sessionState.get();
    }

    /**
     * Updates the session state (and the derived focus state). Subscribers are only notified if the value actually
     * changes.
     */
    public void setSessionState(SessionState newState){
        if (sessionState.get() != newState){
            sessionState.set(newState);
        }
        boolean focused = newState == SessionState.FOCUSED;
        if (sessionFocused.get() != focused){
            sessionFocused.set(focused);
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

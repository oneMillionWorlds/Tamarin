package com.onemillionworlds.tamarin.openxr;

import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XrSessionObservablesTest{

    @Test
    void focusChangesOnlyReportedWhenFocusActuallyChanges(){
        XrSessionObservables observables = new XrSessionObservables();
        ObservableValueSubscription<Boolean> focusSubscription = observables.subscribeToSessionFocused();
        ObservableValueSubscription<SessionState> stateSubscription = observables.subscribeToSessionState();

        assertFalse(focusSubscription.get());
        assertEquals(SessionState.UNKNOWN, stateSubscription.get());

        observables.setSessionState(SessionState.READY);
        assertFalse(focusSubscription.checkHasChanged(), "Still unfocused, so no focus change");
        assertTrue(stateSubscription.checkHasChanged());

        observables.setSessionState(SessionState.FOCUSED);
        assertTrue(focusSubscription.checkHasChanged());
        assertTrue(focusSubscription.get());

        observables.setSessionState(SessionState.FOCUSED);
        assertFalse(focusSubscription.checkHasChanged());
        assertTrue(stateSubscription.checkHasChanged()); // READY -> FOCUSED
        assertFalse(stateSubscription.checkHasChanged(), "Setting the same state again is not a change");

        observables.setSessionState(SessionState.VISIBLE);
        assertTrue(focusSubscription.checkHasChanged());
        assertFalse(focusSubscription.get());
        assertEquals(SessionState.VISIBLE, observables.getSessionState());
    }

    @Test
    void shouldPauseCombinesFocusAndUserPresence(){
        XrSessionObservables observables = new XrSessionObservables();
        ObservableValueSubscription<Boolean> shouldPauseSubscription = observables.subscribeToShouldPause();
        ObservableValueSubscription<Boolean> presentSubscription = observables.subscribeToUserPresent();

        assertTrue(observables.isUserPresent(), "User should be assumed present until told otherwise");
        assertTrue(shouldPauseSubscription.get(), "Not yet focused, so should pause");

        observables.setSessionState(SessionState.FOCUSED);
        assertTrue(shouldPauseSubscription.checkHasChanged());
        assertFalse(shouldPauseSubscription.get());

        observables.setUserPresent(false);
        assertTrue(presentSubscription.checkHasChanged());
        assertTrue(shouldPauseSubscription.checkHasChanged());
        assertTrue(shouldPauseSubscription.get());
        assertTrue(observables.isSessionFocused(), "Taking the headset off doesn't by itself change focus");

        // losing focus while already paused is not a change in should pause
        observables.setSessionState(SessionState.VISIBLE);
        assertFalse(shouldPauseSubscription.checkHasChanged());

        observables.setUserPresent(true);
        assertFalse(shouldPauseSubscription.checkHasChanged(), "Still unfocused, so still paused");

        observables.setSessionState(SessionState.FOCUSED);
        assertTrue(shouldPauseSubscription.checkHasChanged());
        assertFalse(shouldPauseSubscription.get());
    }

    @Test
    void referenceSpaceChangeEventsAreReported(){
        XrSessionObservables observables = new XrSessionObservables();
        var subscription = observables.subscribeToReferenceSpaceChangePending();
        assertFalse(subscription.checkHasChanged());
        observables.fireReferenceSpaceChangePending();
        assertTrue(subscription.checkHasChanged());
        assertFalse(subscription.checkHasChanged());
    }
}

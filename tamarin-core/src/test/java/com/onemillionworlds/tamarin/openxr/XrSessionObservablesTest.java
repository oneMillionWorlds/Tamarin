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
    void referenceSpaceChangeEventsAreReported(){
        XrSessionObservables observables = new XrSessionObservables();
        var subscription = observables.subscribeToReferenceSpaceChangePending();
        assertFalse(subscription.checkHasChanged());
        observables.fireReferenceSpaceChangePending();
        assertTrue(subscription.checkHasChanged());
        assertFalse(subscription.checkHasChanged());
    }
}

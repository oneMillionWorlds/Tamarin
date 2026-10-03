package com.onemillionworlds.tamarin.observable;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObservableDataEventTest{

    @Test
    void subscriptionsSeeOnlyEventsAfterTheySubscribedAndEachOnlyOnce(){
        ObservableDataEvent<String> event = new ObservableDataEvent<>();
        event.fireEvent("before");

        ObservableDataEventSubscription<String> subscription = event.subscribe();
        assertFalse(subscription.peakHasChanged());
        assertEquals(List.of(), subscription.pollEvents());

        event.fireEvent("a");
        event.fireEvent("b");
        assertTrue(subscription.peakHasChanged());
        assertEquals(List.of("a", "b"), subscription.pollEvents());
        assertEquals(List.of(), subscription.pollEvents());
    }

    @Test
    void subscriptionsAreIndependent(){
        ObservableDataEvent<String> event = new ObservableDataEvent<>();
        ObservableDataEventSubscription<String> first = event.subscribe();
        ObservableDataEventSubscription<String> second = event.subscribe();

        event.fireEvent("a");
        assertEquals(List.of("a"), first.pollEvents());
        event.fireEvent("b");
        assertEquals(List.of("b"), first.pollEvents());
        assertEquals(List.of("a", "b"), second.pollEvents());
    }

    @Test
    void veryOldEventsAreDropped(){
        ObservableDataEvent<Integer> event = new ObservableDataEvent<>();
        ObservableDataEventSubscription<Integer> subscription = event.subscribe();
        for(int i = 0; i < ObservableDataEvent.HISTORY_SIZE + 10; i++){
            event.fireEvent(i);
        }
        List<Integer> events = subscription.pollEvents();
        assertEquals(ObservableDataEvent.HISTORY_SIZE, events.size());
        assertEquals(10, events.get(0));
    }
}

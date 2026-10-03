package com.onemillionworlds.tamarin.observable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ObservableDataEventSubscription<T>{
    private final ObservableDataEvent<T> underlyingEvent;

    long observedGeneration;

    ObservableDataEventSubscription(ObservableDataEvent<T> underlyingEvent){
        this.underlyingEvent = underlyingEvent;
        this.observedGeneration = underlyingEvent.generation;
    }

    /**
     * @return true if there are events that have not yet been polled (does not mark them as seen)
     */
    public boolean peakHasChanged(){
        return observedGeneration != underlyingEvent.generation;
    }

    /**
     * Returns the data for all events fired since the last time this method was called (oldest first), and marks
     * them as seen. Returns an empty list if there are no new events.
     */
    public List<T> pollEvents(){
        if (observedGeneration == underlyingEvent.generation){
            return List.of();
        }
        List<T> newEvents = new ArrayList<>(1);
        for(ObservableDataEvent.FiredEvent<T> event : underlyingEvent.history){
            if (event.generation > observedGeneration){
                newEvents.add(event.data);
            }
        }
        observedGeneration = underlyingEvent.generation;
        return newEvents;
    }
}

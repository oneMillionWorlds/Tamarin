package com.onemillionworlds.tamarin.observable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Like {@link ObservableEvent} but each firing carries some data (e.g. which hand an event relates to).
 * <p>
 *     Subscriptions are polled (see {@link ObservableDataEventSubscription#pollEvents()}). A limited history is kept
 *     so a subscription that isn't polled for a very long time (more than {@link #HISTORY_SIZE} events) will miss the
 *     oldest events.
 * </p>
 * @param <T> the type of data carried by each event
 */
public class ObservableDataEvent<T>{

    public static final int HISTORY_SIZE = 64;

    long generation;

    final Deque<FiredEvent<T>> history = new ArrayDeque<>();

    public void fireEvent(T data){
        generation++;
        history.addLast(new FiredEvent<>(generation, data));
        if (history.size() > HISTORY_SIZE){
            history.removeFirst();
        }
    }

    public ObservableDataEventSubscription<T> subscribe(){
        return new ObservableDataEventSubscription<>(this);
    }

    static final class FiredEvent<T>{
        final long generation;
        final T data;

        FiredEvent(long generation, T data){
            this.generation = generation;
            this.data = data;
        }
    }
}

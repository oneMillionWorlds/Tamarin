package com.onemillionworlds.tamarin.observable;

public class ObservableValue<T>{

    T value;
    long generation;

    public ObservableValue(){
        this(null);
    }

    public ObservableValue(T initialValue){
        value = initialValue;
        generation = 0;
    }

    public T get(){
        return value;
    }

    public void set(T newValue){
        value = newValue;
        generation++;
    }

    /**
     * Obtains a subscription to be able to independantly determine if the value has changed.
     */
    public ObservableValueSubscription<T> subscribe(){
        return new ObservableValueSubscription<>(this);
    }
}

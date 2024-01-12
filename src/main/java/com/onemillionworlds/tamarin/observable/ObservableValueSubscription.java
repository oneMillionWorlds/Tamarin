package com.onemillionworlds.tamarin.observable;

@SuppressWarnings("unused")
public class ObservableValueSubscription<T>{
    ObservableValue<T> underlyingValue;

    long lastObservedGeneration = -1;

    protected ObservableValueSubscription(ObservableValue<T> underlyingValue){
        this.underlyingValue = underlyingValue;
    }

    protected ObservableValueSubscription(ObservableValueSubscription<T> other){
        this.underlyingValue = other.underlyingValue;
        this.lastObservedGeneration = other.lastObservedGeneration;
    }

    public boolean peakHasChanged(){
        return lastObservedGeneration != underlyingValue.generation;
    }

    /**
     * Note this method checks if the value has changed and then records if it has. Meaning the two
     * successive calls to this method may return different values (as the second time the check has been made).
     *
     * @return true if the value has changed since the last time this method was called.
     */
    public boolean checkHasChanged(){
        if(lastObservedGeneration != underlyingValue.generation){
            lastObservedGeneration = underlyingValue.generation;
            return true;
        }
        return false;
    }

    public T get(){
        return underlyingValue.get();
    }

    public ObservableValueSubscription<T> cloneToIndependentReference(){
        return new ObservableValueSubscription<>(this);
    }
}


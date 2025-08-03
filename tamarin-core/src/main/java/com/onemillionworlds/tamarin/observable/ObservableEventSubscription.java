package com.onemillionworlds.tamarin.observable;

@SuppressWarnings("unused")
public class ObservableEventSubscription{
    private final ObservableEvent underlyingValue;

    long observedGeneration;

    public ObservableEventSubscription(ObservableEvent underlyingValue){
        this.underlyingValue = underlyingValue;
        this.observedGeneration = underlyingValue.generation;
    }

    public ObservableEventSubscription(ObservableEventSubscription other){
        this.underlyingValue = other.underlyingValue;
        this.observedGeneration = other.observedGeneration;
    }

    public ObservableEventSubscription cloneToIndependentReference(){
        return new ObservableEventSubscription(this);
    }

    public boolean peakHasChanged(){
        return observedGeneration != underlyingValue.generation;
    }

    /**
     * Note this method checks if the event has fired and then records if it has. Meaning the two
     * successive calls to this method may return different values (as the second time the check has been made).
     *
     * @return true if the event has fired since the last time this method was called.
     */
    public boolean checkHasChanged(){
        if(observedGeneration != underlyingValue.generation){
            observedGeneration = underlyingValue.generation;
            return true;
        }
        return false;
    }
}

package com.onemillionworlds.tamarin.observable;

public class ObservableEvent{

    long generation;

    public ObservableEvent(){
    }


    public void fireEvent(){
        generation++;
    }


    public ObservableEventSubscription subscribe(){
        return new ObservableEventSubscription(this);
    }
}

package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.ClimbingPointGrabControl;
import lombok.Getter;
import lombok.Setter;

/**
 * This is a function that is just here to keep track of climb grab events, it doesn't do anything
 * beyond that. It is coupled with the {@link ClimbingPointGrabControl} to allow for geometries to be climbed.
 *
 * Where possible the observer will be moved (but never rotated) to keep the hand at the grab start position.
 *
 * If two hands are grabbing at once then an averaging occures
 */
public class ClimbSupport implements BoundHandFunction{

    @Setter
    @Getter
    private Vector3f grabStartPosition;

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){}

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){}

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){}
}

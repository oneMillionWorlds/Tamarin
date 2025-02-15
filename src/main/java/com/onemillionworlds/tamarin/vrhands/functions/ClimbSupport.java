package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.ClimbingPointGrabControl;

/**
 * This is a function that is just here to keep track of climb grab events, it doesn't do anything
 * beyond that. It is coupled with the {@link ClimbingPointGrabControl} to allow for geometries to be climbed.
 * <p>
 * Where possible the observer will be moved (but never rotated) to keep the hand at the grab start position.
 * <p>
 * If two hands are grabbing at once then an averaging occures
 */
public class ClimbSupport implements BoundHandFunction{

    private Vector3f grabStartPosition;

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
    }

    public Vector3f getGrabStartPosition(){
        return this.grabStartPosition;
    }

    public void setGrabStartPosition(Vector3f grabStartPosition){
        this.grabStartPosition = grabStartPosition;
    }
}

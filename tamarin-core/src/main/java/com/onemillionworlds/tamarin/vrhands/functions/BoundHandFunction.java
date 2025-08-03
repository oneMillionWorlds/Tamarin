package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

public interface BoundHandFunction{

    /**
     * Called when the function is first attached
     * @param boundHand the hand it's attached to
     * @param stateManager the application AppStateManager, for convenience
     */
    void onBind(BoundHand boundHand, AppStateManager stateManager);

    /**
     * Called when the function is detached
     * @param boundHand the hand it's attached to
     * @param stateManager the application AppStateManager, for convenience
     */
    void onUnbind(BoundHand boundHand, AppStateManager stateManager);

    /**
     * Called when the hand is first attached
     * @param boundHand the hand it's attached to
     * @param stateManager the application AppStateManager, for convenience
     */
    void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager);
}

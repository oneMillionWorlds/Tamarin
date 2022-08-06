package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.event.LemurProtectedSupport;
import com.simsilica.lemur.event.PickEventSession;

import java.util.Iterator;

/**
 * This is like {@link LemurPressFunction} but it is physically
 * touching buttons with the finger (when the hand is in pointing mode).
 *
 * It is not quite as integrated with Lemur as {@link LemurPressFunction} and
 * doesn't support as many interactions (basically only buttons, things with click
 * event listeners and text boxes)
 */
public class LemurPressFunction implements BoundHandFunction{

    Node pickAgainstNode;

    boolean wasPressingLastUpdate = false;

    private Camera syntheticCamera;
    private ViewPort syntheticViewport;

    private VrLemurAppState mouseAppState;
    private PickEventSession lemurSession;

    private ActionBasedOpenVrState actionBasedOpenVrState;
    private VRHandsAppState vrHandsAppState;

    public LemurPressFunction(Node pickAgainstNode){
        this.pickAgainstNode = pickAgainstNode;
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
        this.mouseAppState = stateManager.getState(VrLemurAppState.class);
        this.vrHandsAppState = stateManager.getState(VRHandsAppState.class);
        lemurSession = LemurProtectedSupport.getSession(this.mouseAppState);

    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){

    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        if (boundHand.isHandPointing()){

            CollisionResults results = boundHand.pickIndexFingerTip(pickAgainstNode);

            CollisionResults filteredToPressedByFinger = new CollisionResults();
            for(CollisionResult next : results){
                if(next.getDistance() < BoundHand.PICK_INDEX_FINGER_STANDOFF_DISTANCE){
                    filteredToPressedByFinger.addCollision(next);
                }
            }
            if (filteredToPressedByFinger.size()>0 && !wasPressingLastUpdate){
                LemurSupport.clickThroughFullHandling(pickAgainstNode, filteredToPressedByFinger, stateManager);
                wasPressingLastUpdate = true;
            }else if (filteredToPressedByFinger.size() == 0 && wasPressingLastUpdate){
                wasPressingLastUpdate = false;
            }
        }
    }
}

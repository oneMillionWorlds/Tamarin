package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.compatibility.WrongActionTypeException;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import lombok.Setter;

public class LemurClickFunction implements BoundHandFunction{

    private final Node pickAgainstNode;
    private final String clickAction;

    /**
     * When an analog action is being used then this is its minimum value to actually cause a trigger event
     */
    @Setter
    private float minTriggerToClick = 0.5f;

    private float lastTriggerPressure = 0;

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;

    private boolean clickActionIsAnalog = true;

    public LemurClickFunction(String clickAction, Node pickAgainstNode){
        this.pickAgainstNode = pickAgainstNode;
        this.clickAction = clickAction;
    }


    public void click(){
        BoundHand.assertLemurAvailable();
        CollisionResults results = this.boundHand.pickBulkHand(pickAgainstNode);
        LemurSupport.clickThroughCollisionResults(pickAgainstNode, results, actionBasedOpenVrState.getStateManager());
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){

    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        float triggerPressure = getClickActionPressure(clickAction);
        if (triggerPressure>minTriggerToClick && lastTriggerPressure<minTriggerToClick){
            click();
        }

        lastTriggerPressure = triggerPressure;
    }

    private float getClickActionPressure(String action){
        try{
            if (clickActionIsAnalog){
                AnalogActionState grabActionState = actionBasedOpenVrState.getAnalogActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.x;
            }else{
                DigitalActionState grabActionState = actionBasedOpenVrState.getDigitalActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.state?1:0;
            }
        }catch(WrongActionTypeException wrongActionTypeException){
            //its the opposite type of action, switch automatically, on the next update the correct type will be used
            clickActionIsAnalog = !clickActionIsAnalog;
            return 0;
        }
    }
}

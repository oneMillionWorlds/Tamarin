package com.onemillionworlds.tamarin.vrhands.functions;

import com.onemillionworlds.tamarin.actions.XrActionBaseAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * Function that normalises out whether a button or a trigger has been used to bind grabbing
 * (obviously a trigger is getter)
 */
public class GrabActionNormaliser{

    private boolean grabActionIsAnalog = true;

    public float getGripActionPressure(BoundHand boundHand, ActionHandle action){
        try{
            if (grabActionIsAnalog){
                FloatActionState grabActionState = boundHand.getFloatActionState(action);
                return grabActionState.getState();
            }else{
                BooleanActionState grabActionState = boundHand.getBooleanActionState(action);
                return grabActionState.getState()?1:0;
            }
        }catch(XrActionBaseAppState.IncorrectActionTypeException booleanActionState){
            // self-heal
            grabActionIsAnalog = !grabActionIsAnalog;
            return 0;
        }
    }
}

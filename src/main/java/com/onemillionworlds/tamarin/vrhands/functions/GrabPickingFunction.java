package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.compatibility.WrongActionTypeException;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import lombok.Setter;

import java.util.Optional;

public class GrabPickingFunction implements BoundHandFunction{

    private final String grabAction;

    private final Node nodeToGrabPickAgainst;

    private float timeSinceGrabbed;

    /**
     * How much time to let pass between the picking events that trigger grabs (and releases)
     */
    @Setter
    private float grabEvery;

    /**
     * Allows the amount of pressure required to pick something up to be changed.
     * A value between 0 and 1
     */
    @Setter
    private float minimumGripToTrigger = 0.5f;

    private boolean grabActionIsAnalog = true;

    private float lastGripPressure;

    Optional<AbstractGrabControl> currentlyGrabbed = Optional.empty();

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;

    public GrabPickingFunction(String grabAction, Node nodeToGrabPickAgainst){
        this.grabAction = grabAction;
        this.nodeToGrabPickAgainst = nodeToGrabPickAgainst;
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.ID, ActionBasedOpenVrState.class);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        currentlyGrabbed.ifPresent(grabbed -> grabbed.onRelease(boundHand));
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        timeSinceGrabbed+=timeSlice;
        if (timeSinceGrabbed>grabEvery){
            timeSinceGrabbed = 0;
            float gripPressure =  getGripActionPressure(boundHand, grabAction);

            //the lastGripPressure stuff is so that a clenched fist isn't constantly trying to grab things
            if (gripPressure>minimumGripToTrigger && lastGripPressure<minimumGripToTrigger && currentlyGrabbed.isEmpty()){
                CollisionResults results = boundHand.pickGrab(nodeToGrabPickAgainst);
                Optional<AbstractGrabControl> grabControl = TamarinUtilities.findAllControlsInResults(AbstractGrabControl.class, results).stream().findFirst();
                if(grabControl.isPresent()){
                    currentlyGrabbed = grabControl;
                    grabControl.get().onGrab(boundHand);
                }
            }else if (gripPressure<minimumGripToTrigger && currentlyGrabbed.isPresent()){
                //drop current item
                currentlyGrabbed.get().onRelease(boundHand);
                currentlyGrabbed = Optional.empty();
            }
            lastGripPressure = gripPressure;
        }
    }

    public boolean isCurrentlyHoldingSomething(){
        return currentlyGrabbed.isPresent();
    }

    /**
     * Usually grabbing handles itself. However sometimes you may want to give an already clenched hand an item that has
     * been freshly created. An example of this would be when you are building and every time you clench your hand a
     * new item is "magicked up" in that hand (and then the user places that item). This method can be used in that case.
     * <p>
     * Note that if the user isn't currently clenching the hand then they will immediately drop the item.
     */
    public void manuallyGiveControlToHold(AbstractGrabControl grabControl){
        currentlyGrabbed.ifPresent(abstractGrabControl -> abstractGrabControl.onRelease(boundHand));
        currentlyGrabbed = Optional.of(grabControl);
        grabControl.onGrab(boundHand);
    }

    private float getGripActionPressure(BoundHand boundHand, String action){
        try{
            if (grabActionIsAnalog){
                AnalogActionState grabActionState = actionBasedOpenVrState.getAnalogActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.x;
            }else{
                DigitalActionState grabActionState = actionBasedOpenVrState.getDigitalActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.state?1:0;
            }
        }catch(WrongActionTypeException wrongActionTypeException){
            //it's the opposite type of action, switch automatically, on the next update the correct type will be used
            grabActionIsAnalog = !grabActionIsAnalog;
            return 0;
        }
    }

}

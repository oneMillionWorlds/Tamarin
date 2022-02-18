package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.compatibility.WrongActionTypeException;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.simsilica.lemur.event.BasePickState;
import com.simsilica.lemur.event.MouseAppState;
import com.simsilica.lemur.event.PickEventSession;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

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

    private AppStateManager stateManager;
    private boolean dominant = false;

    private Camera syntheticCamera;

    private MouseAppState mouseAppState;
    private PickEventSession lemurSession;
    private Method dispatchMouseEvent;


    public LemurClickFunction(String clickAction, Node pickAgainstNode){
        this.pickAgainstNode = pickAgainstNode;
        this.clickAction = clickAction;
    }


    public void clickSpecialSupport(){
        BoundHand.assertLemurAvailable();
        CollisionResults results = this.boundHand.pickBulkHand(pickAgainstNode);
        LemurSupport.clickThroughCollisionResultsForSpecialHandling(pickAgainstNode, results, actionBasedOpenVrState.getStateManager());
    }

    @SneakyThrows
    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
        this.stateManager = stateManager;
        this.mouseAppState = this.stateManager.getState(MouseAppState.class);

        Method retrieveItems = BasePickState.class.getDeclaredMethod("getSession");
        retrieveItems.setAccessible(true);
        lemurSession = (PickEventSession)retrieveItems.invoke(this.mouseAppState);

        dispatchMouseEvent = mouseAppState.getClass().getDeclaredMethod("dispatch", MouseButtonEvent.class);
        dispatchMouseEvent.setAccessible(true);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){

    }

    @SneakyThrows
    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        float triggerPressure = getClickActionPressure(clickAction);
        if (triggerPressure>minTriggerToClick && lastTriggerPressure<minTriggerToClick){
            if (!dominant){
                becomeDominant();

            }
        }

        if (dominant){
            syntheticCamera.setLocation(boundHand.getHandNode_zPointing().getWorldTranslation());
            syntheticCamera.setRotation(boundHand.getHandNode_zPointing().getWorldRotation());
            lemurSession.cursorMoved(500,500);//the exact middle of the 1000 by 1000 synthetic camera

            if(triggerPressure > minTriggerToClick && lastTriggerPressure < minTriggerToClick){
                dispatchMouseEvent.invoke(mouseAppState, new MouseButtonEvent(0, true, 500, 500));
                clickSpecialSupport();
            }
            if(triggerPressure < minTriggerToClick && lastTriggerPressure > minTriggerToClick){
                dispatchMouseEvent.invoke(mouseAppState, new MouseButtonEvent(0, false, 500, 500));
            }
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

    /**
     * The dominant hand is the one that most recently clicked. It constantly updates lemur with its mouse position
     * and has its synthetic viewport be the viewport thats used for
     */
    private void becomeDominant(){
        //really, want to remove all collision roots
        mouseAppState.removeCollisionRoot(stateManager.getApplication().getViewPort());
        mouseAppState.removeCollisionRoot(stateManager.getApplication().getGuiViewPort());
        syntheticCamera = new Camera(1000,1000);
        ViewPort syntheticViewport = new ViewPort("tamarinHandSyntheticViewport", syntheticCamera);
        syntheticViewport.attachScene(pickAgainstNode);
        mouseAppState.addCollisionRoot(syntheticViewport);

        dominant = true;
    }
}

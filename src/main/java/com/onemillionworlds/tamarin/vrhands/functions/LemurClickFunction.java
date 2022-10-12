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
import com.onemillionworlds.tamarin.lemursupport.SpecialHandlingClickThroughResult;
import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.event.LemurProtectedSupport;
import com.simsilica.lemur.event.PickEventSession;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;

public class LemurClickFunction implements BoundHandFunction{

    /**
     * These are global settings intended as an advanced option. Controls the closest thing that can be picked with
     * a lemur click
     */
    public static float PICK_MINIMUM = 0.01f;

    /**
     * These are global settings intended as an advanced option. Controls the futhest thing that can be picked with
     * a lemur click
     */
    public static float PICK_MAXIMUM = 500f;

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
    private VRHandsAppState vrHandsAppState;

    private boolean clickActionIsAnalog = true;

    private AppStateManager stateManager;
    private boolean dominant = false;

    private Camera syntheticCamera;
    private ViewPort syntheticViewport;

    private VrLemurAppState mouseAppState;
    private PickEventSession lemurSession;

    Optional<LemurKeyboard> openKeyboard = Optional.empty();

    public LemurClickFunction(String clickAction, Node pickAgainstNode){
        this.pickAgainstNode = pickAgainstNode;
        this.clickAction = clickAction;
    }

    public void clickSpecialSupport(){
        BoundHand.assertLemurAvailable();
        CollisionResults results = this.boundHand.pickBulkHand(pickAgainstNode);
        SpecialHandlingClickThroughResult specialHandlingClickThroughResult = LemurSupport.clickThroughCollisionResultsForSpecialHandling(pickAgainstNode, results, actionBasedOpenVrState.getStateManager(), false, this::handleNewKeyboardOpening);
        if (openKeyboard.isPresent() && (specialHandlingClickThroughResult == SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS)){
            closeOpenKeyboard();
        }
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
        this.stateManager = stateManager;
        this.mouseAppState = this.stateManager.getState(VrLemurAppState.class);
        this.vrHandsAppState = this.stateManager.getState(VRHandsAppState.class);
        lemurSession = LemurProtectedSupport.getSession(this.mouseAppState);

    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        if (syntheticViewport!=null){
            mouseAppState.removeCollisionRoot(syntheticViewport);
        }
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
                mouseAppState.dispatch( new MouseButtonEvent(0, true, 500, 500));
                clickSpecialSupport();
            }
            if(triggerPressure < minTriggerToClick && lastTriggerPressure > minTriggerToClick){
                mouseAppState.dispatch( new MouseButtonEvent(0, false, 500, 500));
            }
        }
        lastTriggerPressure = triggerPressure;
    }

    private void handleNewKeyboardOpening(LemurKeyboard keyboard){
        //may have already self detached, that's fine if so
        closeOpenKeyboard();
        openKeyboard = Optional.of(keyboard);
    }

    private void closeOpenKeyboard(){
        openKeyboard.ifPresent(k -> this.stateManager.detach(k));
        openKeyboard = Optional.empty();
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
     * and has its synthetic viewport be the viewport that's used for
     */
    private void becomeDominant(){
        List<PickEventSession.RootEntry> pickRoots = LemurProtectedSupport.getPickRoots(lemurSession);

        pickRoots.forEach(root -> {
            mouseAppState.removeCollisionRoot(root.viewport);
        });

        this.vrHandsAppState.getHandControls().forEach(h -> {
            LemurClickFunction lemurClick = h.getFunction(LemurClickFunction.class);
            if (lemurClick!=null){
                lemurClick.dominant = false;
            }
        });

        syntheticCamera = new Camera(1000,1000);
        syntheticCamera.setFrustumNear(PICK_MINIMUM);
        syntheticCamera.setFrustumFar(PICK_MAXIMUM);
        syntheticViewport = new ViewPort("tamarinHandSyntheticViewport", syntheticCamera);
        syntheticViewport.attachScene(pickAgainstNode);
        mouseAppState.addCollisionRoot(syntheticViewport);

        dominant = true;
    }
}

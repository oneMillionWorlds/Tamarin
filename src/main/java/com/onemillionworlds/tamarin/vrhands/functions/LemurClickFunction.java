package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.compatibility.WrongActionTypeException;
import com.onemillionworlds.tamarin.lemursupport.SelectorPopUp;
import com.onemillionworlds.tamarin.lemursupport.SpecialHandlingClickThroughResult;
import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.HandSide;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.event.LemurProtectedSupport;
import com.simsilica.lemur.event.PickEventSession;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

    private final List<Node> pickAgainstNodes;
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
    Optional<SelectorPopUp<?>> openDropdown = Optional.empty();

    private Consumer<HandSide> clickOnNothingConsumer = (handSide) -> {};

    public LemurClickFunction(String clickAction, Node... pickAgainstNodes){
        this.pickAgainstNodes = Arrays.asList(pickAgainstNodes);
        this.clickAction = clickAction;
    }

    public boolean clickSpecialSupport(){
        BoundHand.assertLemurAvailable();
        boolean anyAction = false;
        for(Node node : pickAgainstNodes){
            CollisionResults results = this.boundHand.pickBulkHand(node);
            SpecialHandlingClickThroughResult specialHandlingClickThroughResult = LemurSupport.clickThroughCollisionResultsForSpecialHandling(node, results, actionBasedOpenVrState.getStateManager(), false, this::handleNewKeyboardOpening, this::handleNewDropdownOpening);
            if(openKeyboard.isPresent() && (specialHandlingClickThroughResult != SpecialHandlingClickThroughResult.OPENED_LEMUR_KEYBOARD && specialHandlingClickThroughResult != SpecialHandlingClickThroughResult.CLICK_ON_LEMUR_KEYBOARD)){
                closeOpenKeyboard();
                anyAction = true;
            }
            if(openDropdown.isPresent() && (specialHandlingClickThroughResult != SpecialHandlingClickThroughResult.OPENED_DROPDOWN && specialHandlingClickThroughResult != SpecialHandlingClickThroughResult.CLICKED_ON_DROPDOWN_POPUP)){
                closeDropDown();
                anyAction = true;
            }
        }
        return anyAction;
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

            //left and right hands represented as left and right mouse buttons
            int btnIndex = boundHand.getHandSide() == HandSide.LEFT ? MouseInput.BUTTON_LEFT : MouseInput.BUTTON_RIGHT;
            if(triggerPressure > minTriggerToClick && lastTriggerPressure < minTriggerToClick){
                MouseButtonEvent event = new MouseButtonEvent(btnIndex, true, 500, 500);
                mouseAppState.dispatch(event);
                boolean anySpecialAction = clickSpecialSupport();

                if (!event.isConsumed() && !anySpecialAction){
                    clickOnNothingConsumer.accept(boundHand.getHandSide());
                }
            }
            if(triggerPressure < minTriggerToClick && lastTriggerPressure > minTriggerToClick){
                mouseAppState.dispatch( new MouseButtonEvent(btnIndex, false, 500, 500) );
            }
        }
        lastTriggerPressure = triggerPressure;
    }

    /**
     * If a click occurs but no event gets consumed this consumer is called (and told which hand this is)
     * <p>
     * Note; it's very important to make sure your click events consume the event if you want to make use of this functionality
     * (Or else everything will be interpreted as "clicking on nothing")
     */
    public void setClickOnNothingConsumer(Consumer<HandSide> onClickOnNothing){
        this.clickOnNothingConsumer = onClickOnNothing;
    }

    private void handleNewKeyboardOpening(LemurKeyboard keyboard){
        //may have already self detached, that's fine if so
        closeOpenKeyboard();
        openKeyboard = Optional.of(keyboard);
    }

    private void handleNewDropdownOpening(SelectorPopUp<?> newSelectorPopup){
        closeDropDown();
        openDropdown = Optional.of(newSelectorPopup);
    }

    private void closeOpenKeyboard(){
        openKeyboard.ifPresent(k -> this.stateManager.detach(k));
        openKeyboard = Optional.empty();
    }

    private void closeDropDown(){
        openDropdown.ifPresent(k -> this.stateManager.detach(k));
        openDropdown = Optional.empty();
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

        pickRoots.forEach(root ->
            mouseAppState.removeCollisionRoot(root.viewport)
        );

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
        for(Node node : pickAgainstNodes){
            syntheticViewport.attachScene(node);
        }
        mouseAppState.addCollisionRoot(syntheticViewport);

        dominant = true;
    }
}

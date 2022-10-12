package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.lemursupport.FullHandlingClickThroughResult;
import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import com.onemillionworlds.tamarin.lemursupport.SpecialHandlingClickThroughResult;
import com.onemillionworlds.tamarin.lemursupport.VrLemurAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.touching.AbstractTouchControl;
import com.simsilica.lemur.event.LemurProtectedSupport;
import com.simsilica.lemur.event.PickEventSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This will use finger tip picking and will look for both lemur controls (optional dependant) and
 * Tamarin {@link AbstractTouchControl}
 *
 * It is not quite as integrated with Lemur as {@link LemurClickFunction} and
 * doesn't support as many interactions (basically only buttons, things with click
 * event listeners and text boxes)
 */
public class PressFunction implements BoundHandFunction{

    /**
     * If there are multiple touches within this time all but the first are suppressed
     * This is to avoid annoying double touches (especially caused by the vibrate)
     */
    public static float TOUCH_SUPPRESSION_TIME = 0.2f;

    Node pickAgainstNode;
    boolean requireFingerPointing;

    boolean lemurTouchedLastUpdate = false;
    Optional<String> vibrateActionOnTouch;
    float vibrateOnTouchIntensity;

    private Camera syntheticCamera;
    private ViewPort syntheticViewport;

    private ActionBasedOpenVrState actionBasedOpenVrState;

    private AppStateManager stateManager;

    Collection<AbstractTouchControl> currentlyTouching = new ArrayList<>(1);

    float timeSinceTouched = TOUCH_SUPPRESSION_TIME;

    Optional<LemurKeyboard> openKeyboard = Optional.empty();

    /**
     *
     * @param pickAgainstNode the note to scan for contact with the fingertip)
     * @param requireFingerPointing if the scan should only occure if the hand is in a pointing arrangement
     *                              (index finger outstretched, other fingers curled)
     * @param vibrateActionOnTouch the action name of the vibration binding (e.g. "/actions/main/out/haptic"). Can be null for no vibrate
     * @param vibrateOnTouchIntensity how hard the vibration response is. Should be between 0 and 1
     */
    public PressFunction(Node pickAgainstNode, boolean requireFingerPointing, String vibrateActionOnTouch, float vibrateOnTouchIntensity){
        this.pickAgainstNode = pickAgainstNode;
        this.requireFingerPointing = requireFingerPointing;
        this.vibrateActionOnTouch = Optional.ofNullable(vibrateActionOnTouch);
        this.vibrateOnTouchIntensity = vibrateOnTouchIntensity;
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.stateManager = stateManager;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
        VrLemurAppState mouseAppState = stateManager.getState(VrLemurAppState.class);
        if (BoundHand.isLemurAvailable()){
            PickEventSession lemurSession = LemurProtectedSupport.getSession(mouseAppState);
        }

    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){

    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        timeSinceTouched+=timeSlice;
        if ( timeSinceTouched>TOUCH_SUPPRESSION_TIME && (!requireFingerPointing || boundHand.isHandPointing())){

            CollisionResults results = boundHand.pickIndexFingerTip(pickAgainstNode);
            boolean shouldTriggerHaptic = false;
            if (BoundHand.isLemurAvailable()){
                boolean dryRun = lemurTouchedLastUpdate;
                FullHandlingClickThroughResult clickResult = LemurSupport.clickThroughFullHandling(pickAgainstNode, results, stateManager, dryRun, this::handleNewKeyboardOpening);
                boolean lemurTouchedThisUpdate = clickResult.isClickRegularHandled() || clickResult.getSpecialHandlingClickThroughResult() == SpecialHandlingClickThroughResult.OPENED_LEMUR_KEYBOARD;
                shouldTriggerHaptic = !lemurTouchedLastUpdate && lemurTouchedThisUpdate;
                lemurTouchedLastUpdate = lemurTouchedThisUpdate;

                if (results.size()>0 && !dryRun && openKeyboard.isPresent() && (clickResult.getSpecialHandlingClickThroughResult() == SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS)){
                    closeOpenKeyboard();
                }
            }

            //go looking for tamarin touch controls
            Collection<AbstractTouchControl> touchedControls = TamarinUtilities.findAllControlsInResults(AbstractTouchControl.class, results);

            if (!touchedControls.isEmpty() || !currentlyTouching.isEmpty()){
                List<AbstractTouchControl> newItems = new ArrayList<>(touchedControls);
                newItems.removeAll(currentlyTouching);

                List<AbstractTouchControl> noLongerTouchedItems = new ArrayList<>(currentlyTouching);
                noLongerTouchedItems.removeAll(touchedControls);

                for(AbstractTouchControl newTouch : newItems){
                    shouldTriggerHaptic = true;
                    newTouch.onTouch(boundHand);
                }
                for(AbstractTouchControl removedTouch : noLongerTouchedItems){
                    removedTouch.onStopTouch(boundHand);
                }
            }
            currentlyTouching = touchedControls;

            if (shouldTriggerHaptic){
                timeSinceTouched=0;
            }

            if (shouldTriggerHaptic && vibrateActionOnTouch.isPresent()){
                actionBasedOpenVrState.triggerHapticAction(vibrateActionOnTouch.get(),0.02f, 60, vibrateOnTouchIntensity, boundHand.getHandSide().restrictToInputString );
            }

        }
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
}

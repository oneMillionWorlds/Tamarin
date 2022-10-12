package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards.SimpleQwertyStyle;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.function.Consumer;

import static com.onemillionworlds.tamarin.vrhands.BoundHand.NO_PICK;

public class LemurSupport{

    public static final String TAMARIN_STOP_BUBBLING = "TAMARIN_STOP_BUBBLING";
    public static final String LEMUR_TAMARIN_KEYBOARD = "LEMUR_TAMARIN_KEYBOARD";

    public static KeyboardStyle keyboardStyle = new SimpleQwertyStyle();

    /**
     * Normally it's preferable to dispatch lemur click events but in some cases (like a physical button push) it can
     * be better to special handle it. This will look for handlable lemur "stuff" and do a beset effort of simulating a
     * click on it. It will also handle keyboard popping as normal.
     *
     * Will return true if it found anything that triggered an event (or would have if it wasn't a dry run)
     *
     * @param dryRun If true then scans for things that would trigger, but doesn't actually trigger then. Used for
     *               only triggering clicks on first touch, but then allowing a reset when nothing is touched
     * @param newKeyboards New keyboards are given to this consumer. The intention is that the caller can close that keyboard (by detaching the state) if another is opened etc
     */
    public static FullHandlingClickThroughResult clickThroughFullHandling(Node nodePickedAgainst, CollisionResults results, AppStateManager stateManager, boolean dryRun, Consumer<LemurKeyboard> newKeyboards){
        SpecialHandlingClickThroughResult specialClickResult = clickThroughCollisionResultsForSpecialHandling(nodePickedAgainst, results, stateManager, dryRun, newKeyboards);

        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();
                while(processedSpatial!=null){
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(TAMARIN_STOP_BUBBLING))){
                        return new FullHandlingClickThroughResult(specialClickResult, false);
                    }
                    if (processedSpatial instanceof Button){
                        if (!dryRun){((Button)processedSpatial).click();}
                        return new FullHandlingClickThroughResult(specialClickResult, true);
                    }
                    MouseEventControl mec = processedSpatial.getControl(MouseEventControl.class);
                    if ( mec!=null ){
                        if (!dryRun){
                            mec.mouseButtonEvent(new MouseButtonEvent(0, true, 0, 0), processedSpatial, processedSpatial);
                        }
                        return new FullHandlingClickThroughResult(specialClickResult, true);
                    }
                    processedSpatial = processedSpatial.getParent();
                }
            }
        }
        return new FullHandlingClickThroughResult(specialClickResult, false);
    }

    /**
     * Given a set of collision results, looks through them for anything that needs to be handled in a non "traditional lemur" way, like opening keyboards
     * @param nodePickedAgainst the node that is picked against (Used for attaching keyboards to)
     * @param results the collision results to pick through
     * @param stateManager the stateManager
     * @param newKeyboards New keyboards are given to this consumer. The intention is that the caller can close that keyboard (by detaching the state) if another is opened etc
     * @return if it did (or would have if dry run) opened a keyboard or other special handing
     */
    public static SpecialHandlingClickThroughResult clickThroughCollisionResultsForSpecialHandling(Node nodePickedAgainst, CollisionResults results, AppStateManager stateManager, boolean dryRun, Consumer<LemurKeyboard> newKeyboards){
        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();
                while(processedSpatial!=null){
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(LEMUR_TAMARIN_KEYBOARD))){
                        return SpecialHandlingClickThroughResult.CLICK_ON_LEMUR_KEYBOARD;
                    }
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(TAMARIN_STOP_BUBBLING))){
                        return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
                    }

                    if ( processedSpatial instanceof TextField){
                        TextField textField = ((TextField)processedSpatial);
                        textField.getControl(GuiControl.class).focusGained();
                        LemurKeyboard keyboard = new LemurKeyboard(
                                nodePickedAgainst,
                                c -> textField.setText(textField.getText()+c),
                                (event,data) -> {
                                    String startingText = textField.getText();
                                    switch(event){
                                        case DELETE_CHAR:
                                            if (startingText.length()>0){
                                                textField.setText(startingText.substring(0, startingText.length()-1));
                                            }
                                            break;
                                        case DELETE_ALL:
                                            textField.setText("");
                                            break;
                                        case CLOSED_KEYBOARD:
                                            textField.getControl(GuiControl.class).focusLost();
                                            break;
                                    }
                                }, keyboardStyle
                                ,processedSpatial.getWorldTranslation(), processedSpatial.getWorldRotation(),
                                textField.getWorldScale().x);
                        if (!dryRun){
                            stateManager.attach(keyboard);
                            newKeyboards.accept(keyboard);
                        }
                        return SpecialHandlingClickThroughResult.OPENED_LEMUR_KEYBOARD;
                    }
                    processedSpatial = processedSpatial.getParent();
                }
                return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
            }

        }
        return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
    }
}


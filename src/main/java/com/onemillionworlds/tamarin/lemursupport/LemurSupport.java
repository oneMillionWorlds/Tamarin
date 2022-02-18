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

import static com.onemillionworlds.tamarin.vrhands.BoundHand.NO_PICK;

public class LemurSupport{

    public static final String TAMARIN_STOP_BUBBLING = "TAMARIN_STOP_BUBBLING";

    public static KeyboardStyle keyboardStyle = new SimpleQwertyStyle();

    /**
     * Given a set of collision results, looks through them for anything that needs to be handled in a non "traditional lemur" way, like opening keyboards
     * @param results the result of a pick that returns things that could be a lemur ui
     */
    public static void clickThroughCollisionResultsForSpecialHandling(Node nodePickedAgainst, CollisionResults results, AppStateManager stateManager){
        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();
                while(processedSpatial!=null){
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(TAMARIN_STOP_BUBBLING))){
                        return;
                    }

                    if ( processedSpatial instanceof TextField){
                        terminateAnyExistingKeyboards(stateManager);
                        TextField textField = ((TextField)processedSpatial);
                        textField.getControl(GuiControl.class).focusGained();
                        stateManager.attach(new LemurKeyboard(
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
                                ,processedSpatial.getWorldTranslation(), processedSpatial.getWorldRotation()));
                        return;
                    }
                    processedSpatial = processedSpatial.getParent();
                }
                //if we don't click on anything that cares then that closes the keyboard
                terminateAnyExistingKeyboards(stateManager);
                return;
            }

        }
        //if we don't click on anything that cares then that closes the keyboard
        terminateAnyExistingKeyboards(stateManager);
    }

    public static void terminateAnyExistingKeyboards(AppStateManager stateManager){
        LemurKeyboard keyboard = stateManager.getState(LemurKeyboard.class);

        if (keyboard!=null){
            stateManager.detach(keyboard);
        }
    }

}


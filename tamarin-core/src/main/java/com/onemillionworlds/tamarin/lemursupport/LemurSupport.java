package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardTextEdits;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards.SimpleQwertyStyle;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.GridPanel;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.Selector;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.function.Consumer;

import static com.onemillionworlds.tamarin.vrhands.BoundHand.NO_PICK;

public class LemurSupport{

    public static final String TAMARIN_STOP_BUBBLING = BoundHand.TAMARIN_STOP_BUBBLING;
    public static final String LEMUR_TAMARIN_KEYBOARD = "LEMUR_TAMARIN_KEYBOARD";
    public static final String LEMUR_SELECTOR_POPUP = "LEMUR_SELECTOR_POPUP";

    /**
     * The keyboard used for text fields that haven't been given their own (see {@link #setKeyboardStyle}).
     */
    public static KeyboardStyle keyboardStyle = new SimpleQwertyStyle();

    /**
     * Sets the keyboard that will open when this particular text field is clicked, overriding the global
     * {@link #keyboardStyle}. E.g. {@link com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards.NumericStyle#INSTANCE}
     * for a field that should only contain numbers.
     * <p>
     * Pass null to go back to using the global keyboard style.
     */
    public static void setKeyboardStyle(TextField textField, KeyboardStyle keyboardStyle){
        textField.removeControl(KeyboardStyleControl.class);
        if (keyboardStyle != null){
            textField.addControl(new KeyboardStyleControl(keyboardStyle));
        }
    }

    /**
     * Gets the keyboard that will open when this text field is clicked; its own one if set by {@link #setKeyboardStyle},
     * otherwise the global {@link #keyboardStyle}.
     */
    public static KeyboardStyle getKeyboardStyle(TextField textField){
        KeyboardStyleControl control = textField.getControl(KeyboardStyleControl.class);
        return control == null ? keyboardStyle : control.getKeyboardStyle();
    }

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
     * @param newSelectorPopups new SelectorPopups (things that get created when drop downs are clicked on). The intention is that the caller can close that popup (by detaching the state) if another is opened etc
     */
    public static FullHandlingClickThroughResult clickThroughFullHandling(Node nodePickedAgainst, CollisionResults results, AppStateManager stateManager, boolean dryRun, Consumer<LemurKeyboard> newKeyboards, Consumer<SelectorPopUp<?>> newSelectorPopups){
        SpecialHandlingClickThroughResult specialClickResult = clickThroughCollisionResultsForSpecialHandling(nodePickedAgainst, results, stateManager, dryRun, newKeyboards, newSelectorPopups);

        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();
                while(processedSpatial!=null){
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(TAMARIN_STOP_BUBBLING))){
                        return new FullHandlingClickThroughResult(specialClickResult, false);
                    }
                    if (clickListBoxCell(processedSpatial, dryRun)){
                        return new FullHandlingClickThroughResult(specialClickResult, true);
                    }
                    if (processedSpatial instanceof Button){
                        if (!dryRun){((Button)processedSpatial).click();}
                        return new FullHandlingClickThroughResult(specialClickResult, true);
                    }
                    MouseEventControl mec = processedSpatial.getControl(MouseEventControl.class);
                    if ( mec!=null && !(mec instanceof NoPressMouseEventControl)){
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
     * @param newSelectorPopups new SelectorPopups (things that get created when drop downs are clicked on). The intention is that the caller can close that popup (by detaching the state) if another is opened etc
     * @return if it did (or would have if dry run) opened a keyboard or other special handing
     */
    public static SpecialHandlingClickThroughResult clickThroughCollisionResultsForSpecialHandling(Node nodePickedAgainst, CollisionResults results, AppStateManager stateManager, boolean dryRun, Consumer<LemurKeyboard> newKeyboards, Consumer<SelectorPopUp<?>> newSelectorPopups){
        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();
                while(processedSpatial!=null){
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(LEMUR_TAMARIN_KEYBOARD))){
                        return SpecialHandlingClickThroughResult.CLICK_ON_LEMUR_KEYBOARD;
                    }
                    if (Boolean.TRUE.equals(processedSpatial.getUserData(LEMUR_SELECTOR_POPUP))){
                        return SpecialHandlingClickThroughResult.CLICKED_ON_DROPDOWN_POPUP;
                    }

                    if (Boolean.TRUE.equals(processedSpatial.getUserData(TAMARIN_STOP_BUBBLING))){
                        return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
                    }

                    if ( processedSpatial instanceof TextField){
                        TextField textField = ((TextField)processedSpatial);
                        textField.getControl(GuiControl.class).focusGained();
                        KeyboardStyle textFieldKeyboardStyle = getKeyboardStyle(textField);
                        LemurKeyboard keyboard = new LemurKeyboard(
                                nodePickedAgainst,
                                c -> {
                                    if (c == null || c.isEmpty()){
                                        return;
                                    }
                                    String proposedText = textField.getText() + c;
                                    if (textFieldKeyboardStyle.isAllowedText(proposedText)){
                                        textField.setText(proposedText);
                                    }
                                },
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
                                        case FLIP_SIGN:
                                            String flippedText = KeyboardTextEdits.flipSign(startingText);
                                            textField.setText(flippedText);
                                            break;
                                        case CLOSED_KEYBOARD:
                                            //its fairly normal to trigger actions off losing focus, so trigger that
                                            textField.getControl(GuiControl.class).focusGained();
                                            textField.getControl(GuiControl.class).focusLost();
                                            break;
                                    }
                                }, textFieldKeyboardStyle
                                ,textFieldWorldCentre(textField), processedSpatial.getWorldRotation(),
                                textField.getWorldScale().x);
                        if (!dryRun){
                            stateManager.attach(keyboard);
                            newKeyboards.accept(keyboard);
                        }
                        return SpecialHandlingClickThroughResult.OPENED_LEMUR_KEYBOARD;
                    }else if (processedSpatial instanceof Selector){
                        //the popup state used by Selector does not work in 3d. Handle it ourselves
                        Selector<?> selector = (Selector<?>)processedSpatial;
                        SelectorPopUp<?> popUp = new SelectorPopUp<>(nodePickedAgainst, selector);
                        if (!dryRun){
                            stateManager.attach(popUp);
                            newSelectorPopups.accept(popUp);
                        }
                        return SpecialHandlingClickThroughResult.OPENED_DROPDOWN;
                    }
                    processedSpatial = processedSpatial.getParent();
                }
                return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
            }

        }
        return SpecialHandlingClickThroughResult.NO_SPECIAL_INTERACTIONS;
    }

    /**
     * A ListBox selects items from cursor events on its cells rather than from button click commands (and some cells,
     * like those in a dropdown's popup, are plain labels), so clicking the cell does nothing. This selects the cell's
     * item instead.
     *
     * @return true if the spatial was a visible cell of a ListBox (whether or not it was selected, due to dryRun)
     */
    private static boolean clickListBoxCell(Spatial cell, boolean dryRun){
        if (!(cell.getParent() instanceof GridPanel)){
            return false;
        }
        GridPanel grid = (GridPanel)cell.getParent();
        Spatial ancestor = grid.getParent();
        while(ancestor != null && !(ancestor instanceof ListBox)){
            ancestor = ancestor.getParent();
        }
        if (ancestor == null || ((ListBox<?>)ancestor).getGridPanel() != grid){
            return false;
        }
        ListBox<?> listBox = (ListBox<?>)ancestor;
        int firstVisibleRow = grid.getRow();
        for(int i = 0; i < grid.getVisibleRows(); i++){
            if (grid.getCell(firstVisibleRow + i, 0) == cell){
                if (!dryRun){
                    listBox.getSelectionModel().setSelection(firstVisibleRow + i);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Lemur panels have their origin at the top left (extending right and down), so the world translation is a corner.
     * This gives the world position of the centre of the text field instead.
     */
    private static Vector3f textFieldWorldCentre(TextField textField){
        Vector3f size = textField.getSize();
        return textField.localToWorld(new Vector3f(size.x / 2, -size.y / 2, 0), null);
    }
}


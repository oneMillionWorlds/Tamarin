package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardStyle;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.event.MouseListener;
import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This is a free floating keyboard that is probably opened as a result of a click on a lemur text field.
 * <p>
 * The key inputs are reported back to the caller.
 * <p>
 * It is expected that the desired picking lines and pick markers are already turned on
 */
public class LemurKeyboard extends BaseAppState{

    public float keyboardScale;

    private final Node rootNodeDelegate = new Node("LemurKeyboardRootNodeDelegate");
    private final Node keyboardNode = new Node("LemurKeyboard");
    private final Consumer<String> textConsumer;
    private final BiConsumer<KeyboardEvent,Object> eventConsumer;
    private final KeyboardStyle keyboardStyle;

    @Getter
    private ShiftMode shiftMode = ShiftMode.UPPER;

    private final Vector3f ownerPosition;

    private final Quaternion ownerRotation;

    public enum ShiftMode{
        LOWER,UPPER,LOCK_UPPER;
    }

    /**
     *
     * @param textConsumer when a normal text letter is typed it is returned here
     * @param eventConsumer when any event other than typing occurs it is published here, the first item is the category, the second is an object that may contain extra details (or may be null) depending on the event type
     * @param keyboardStyle the specific keys that the keyboard should use
     * @param ownerPosition the thing that triggered the keyboard to show's position. The keyboard will appear slightly closer to the player than this and a little below (so eyeline is not blocked)
     * @param absoluteScale the scale the keyboard should be (the world scale will be this and the local scale will be adjusted such that
     */
    public LemurKeyboard(Node nodeToAttachTo, Consumer<String> textConsumer, BiConsumer<KeyboardEvent,Object> eventConsumer, KeyboardStyle keyboardStyle, Vector3f ownerPosition, Quaternion ownerRotation, float absoluteScale){
        nodeToAttachTo.attachChild(rootNodeDelegate);

        //the world transform includes scale, but we are dealing with that ourselves later so that's fine
        rootNodeDelegate.setLocalTransform(nodeToAttachTo.getWorldTransform().invert());

        this.textConsumer = textConsumer;
        this.eventConsumer = eventConsumer;
        this.keyboardStyle = keyboardStyle;
        this.ownerPosition= ownerPosition;
        this.ownerRotation = ownerRotation;
        this.keyboardScale = absoluteScale;
    }

    @Override
    protected void initialize(Application app){
        rootNodeDelegate.attachChild(keyboardNode);

        XrBaseAppState vrAppState = app.getStateManager().getState(XrBaseAppState.ID, XrBaseAppState.class);
        Vector3f cameraLocation = vrAppState.getVrCameraPosition();
        Vector3f toCameraDirection = cameraLocation.subtract(ownerPosition).normalizeLocal();
        float cameraDistance = cameraLocation.distance(ownerPosition);

        keyboardNode.setLocalTranslation(ownerPosition.add(toCameraDirection.mult(0.1f*cameraDistance)).add(0,-0.1f*cameraDistance, 0));
        keyboardNode.setLocalRotation(this.ownerRotation);
        refreshKeyboard();

    }

    public void setShiftMode(ShiftMode shiftMode){
        this.shiftMode = shiftMode;
        eventConsumer.accept(KeyboardEvent.CASE_CHANGE, shiftMode);
        refreshKeyboard();
    }

    private void refreshKeyboard(){
        keyboardNode.detachAllChildren();

        Container lemurWindow = new Container();
        lemurWindow.setUserData(LemurSupport.LEMUR_TAMARIN_KEYBOARD, true);
        lemurWindow.setUserData(LemurSupport.TAMARIN_STOP_BUBBLING, true); //so that clicking on the keyboard doesn't close the keyboard
        lemurWindow.addMouseListener(new MouseListener(){
            @Override public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){event.setConsumed();} //prevent "clicking through" the keyboard if a button is missed
            @Override public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture){}
            @Override public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture){}
            @Override public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture){}
        });


        lemurWindow.setLocalScale(keyboardScale); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)

        for(KeyboardButton[] row : keyboardStyle.getKeyboardKeys()){
            Container rowContainer = new Container();
            lemurWindow.addChild(rowContainer);
            int index = 0;
            for(KeyboardButton button : row){
                Button buttonEntry = rowContainer.addChild(new Button(button.render(shiftMode, this)), 0, index);
                buttonEntry.addClickCommands(
                        source -> {
                            String typedText = button.getStringToAddOnClick(shiftMode, this);
                            textConsumer.accept(typedText);
                            button.onClickEvent(eventConsumer, this);

                            if (typedText!=null && !typedText.isBlank() && shiftMode == ShiftMode.UPPER){
                                setShiftMode(ShiftMode.LOWER);
                            }
                        }
                );
                index++;
            }

        }
        keyboardNode.attachChild(lemurWindow);
    }


    @Override
    public void stateDetached(AppStateManager stateManager){
        super.stateDetached(stateManager);
        rootNodeDelegate.removeFromParent();
        eventConsumer.accept(KeyboardEvent.CLOSED_KEYBOARD, null);
    }

    @Override
    protected void cleanup(Application app){

    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }
}

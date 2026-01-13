package com.onemillionworlds.tamarin.debugwindow;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.StatsView;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Statistics;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.functions.PressFunction;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.ElementId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The debug window creates a ui element that will render debug information to a panel.
 * <p>
 * That panel will follow the player around but can be grabbed and moved.
 * <p>
 * Requires lemur
 */
public class DebugWindowState extends BaseAppState{

    private final Object lock = new Object();

    /**
     * Because of the need to use this state for during development debugging this INSTANCE allows
     * the state to be statically accessed from anywhere
     */
    public static Optional<DebugWindowState> INSTANCE = Optional.empty();

    Node debugWindowNode = new Node("Tamarin-debug-window-node");
    Container lemurWindow;

    /**
     * Stuff this window is displaying
     */
    Map<String, String> displayDataTable = new LinkedHashMap<>();
    Map<String, Integer> ticksSinceLastSet = new HashMap<>();

    /**
     * Stuff this window is providing a UI to enter
     */
    Map<String, Object> userEntryDataTable = new LinkedHashMap<>();
    Map<String, LineItem> newLineItems = new LinkedHashMap<>();
    Map<String, LineItem> currentLineItems = new HashMap<>();

    XrBaseAppState xrAppState;
    VRHandsAppState vrHandsAppState;
    StatsAppState statsAppState;

    protected long nanosOfCountStart = -1;
    protected int frameCounter = 0;

    boolean connectedToHands = false;
    List<FunctionRegistration> deregistrations = new ArrayList<>();

    /**
     * if the DebugWindow is further away than this is is drawn towards the player
     */
    private static final float MAX_DISTANCE_FROM_PLAYER = 2;

    double timeTillNextPositionCheck = 0;

    private final ActionHandle hapticAction;
    private final ActionHandle grabAction;

    private boolean requestInitialiseStats = false;

    private Optional<ElementId> topLevelContainerId = Optional.empty();

    private Statistics statistics;

    private boolean initialised;

    private long lastNanoTime = 0;

    public DebugWindowState(){
        this(null, null);
    }

    /**
     * @param hapticAction On touching a button on the debug window state this haptic will trigger (can be null)
     * @param grabAction action that allows the window to be grabbed and moved (can be null)
     */
    public DebugWindowState(ActionHandle hapticAction, ActionHandle grabAction){
        assert INSTANCE.isEmpty() : "Can only have 1 DebugWindowState";
        INSTANCE = Optional.of(this);
        this.hapticAction = hapticAction;
        this.grabAction = grabAction;
    }

    /**
     * Allows the top level lemur contain ID to be set (for styling).
     * <p>Must be called before initialisation in order to have any effect</p>
     * @param elementId the element ID to use as the top level container
     */
    public DebugWindowState setTopLevelContainerId(ElementId elementId){
        topLevelContainerId = Optional.of(elementId);
        return this;
    }

    @Override
    protected void initialize(Application app){
        initialise();
    }

    private void initialise(){
        if(getState(XrBaseAppState.ID, XrBaseAppState.class) ==null){
            return;
        }

        lastNanoTime = System.nanoTime();

        initialised = true;
        lemurWindow = topLevelContainerId.map(Container::new).orElseGet(Container::new);
        lemurWindow.addChild(new Label("Debug window"));
        debugWindowNode.attachChild(lemurWindow);
        lemurWindow.setLocalScale(0.0015f);

        lemurWindow.setLocalTranslation(-0.25f, -0.25f, 0); //a bit arbitrary, but likely to be about correct so a normal-sized window looks at the player right

        if(lemurWindow.getBackground()!=null && lemurWindow.getBackground() instanceof TbtQuadBackgroundComponent) {
            Material material = null;
            if(lemurWindow.getBackground() instanceof TbtQuadBackgroundComponent){
                material = ((TbtQuadBackgroundComponent) lemurWindow.getBackground()).getMaterial().getMaterial();
            }
            if(lemurWindow.getBackground() instanceof QuadBackgroundComponent){
                material = ((QuadBackgroundComponent) lemurWindow.getBackground()).getMaterial().getMaterial();
            }
            if(material!=null) {
                material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            }
        }

        AbstractGrabControl control = new RelativeMovingGrabControl();
        debugWindowNode.addControl(control);

        xrAppState = getState(XrBaseAppState.ID, XrBaseAppState.class);
        statsAppState = getState(StatsAppState.class);
        ((SimpleApplication)getApplication()).getRootNode().attachChild(debugWindowNode);

        //attach press functions to the bound hands so buttons are guaranteed to be pressable (If other mouse support
        //is on that can also be used)
        connectToHands();

        autoSelectPosition();
    }

    private void connectToHands(){
        vrHandsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
        if(vrHandsAppState == null){
            return;
        }

        List<BoundHand> handControls = vrHandsAppState.getHandControls();

        if (!handControls.isEmpty()){
            handControls.forEach(hand -> {
                deregistrations.add(hand.addFunction(new PressFunction(debugWindowNode, false, hapticAction, 0.5f)));
                if (this.grabAction != null){
                    deregistrations.add(hand.setGrabAction(this.grabAction, debugWindowNode));
                }

            });
            connectedToHands = true;
        }
    }

    private void autoSelectPosition(){
        Vector3f vrCameraPosition = getVrCameraPosition(xrAppState);
        debugWindowNode.setLocalTranslation(vrCameraPosition.add(new Vector3f(0.4f, 0, 0f)));
        debugWindowNode.lookAt(vrCameraPosition, Vector3f.UNIT_Y);
    }

    @Override
    protected void cleanup(Application app){
        debugWindowNode.removeFromParent();
        deregistrations.forEach(FunctionRegistration::endFunction);
        deregistrations.clear();
    }

    @Override
    protected void onEnable(){
        debugWindowNode.setCullHint(Spatial.CullHint.Inherit);
    }

    @Override
    protected void onDisable(){
        debugWindowNode.setCullHint(Spatial.CullHint.Always);
    }

    public DebugWindowState showFtps(){
        newLineItems.put("FPS", new NonFadingRenderText("FPS"));
        return this;
    }

    /**
     * Enable showing of the renderer statistics in the debug window.
     * When enabled, all labels from {@link Statistics#getLabels()} will be displayed as NonFadingRenderText
     * and refreshed once per second.
     */
    public DebugWindowState showStats(){
        requestInitialiseStats = true;
        return this;
    }

    private void initialiseStats(){
        requestInitialiseStats = false;
        if(statistics == null) {
            statistics = getApplication().getRenderer().getStatistics();
            statistics.setEnabled(true);

            for (String label : statistics.getLabels()) {
                NonFadingRenderText item = new NonFadingRenderText(label);
                newLineItems.put(label, item);
            }
        }
    }


    /**
     * This registers a single button that when clicked will call the event
     */
    public void registerButtonsWithCallbacks(String categoryLabel, Runnable event){
        registerButtonsWithCallbacks(categoryLabel, Map.of(categoryLabel, event));
    }

    /**
     * This registers buttons with call backs when they are clicked. It is perfectly safe to call this over and
     * over again with the same data, all but the first will be ignored.
     * <p>
     * Each time a button is clicked the appropriate callback will be called (and can do whatever you like).
     * <p>
     * If you want to control the order of the buttons consider a LinkedHashMap
     */
    public void registerButtonsWithCallbacks(String categoryLabel, Map<String,Runnable> buttonsAndEvents){
        if (!currentLineItems.containsKey(categoryLabel) && !newLineItems.containsKey(categoryLabel)){
            newLineItems.put(categoryLabel, new CallbackButtonBar(categoryLabel, buttonsAndEvents));
        }
    }

    /**
     * Gets the current state of the buttons set up for this enum. If no buttons are currently set up then a
     * new button bar will be set up and the defaultValue will be returned
     */
    public <U extends Enum<U>> U getData(String dataLabel, List<U> options, U defaultValue){
        if (!currentLineItems.containsKey(dataLabel) && !newLineItems.containsKey(dataLabel)){
            userEntryDataTable.put(dataLabel, defaultValue);
            newLineItems.put(dataLabel, new OptionButtonBar(dataLabel, options));
            return defaultValue;
        }else{
            //noinspection unchecked
            return (U) userEntryDataTable.get(dataLabel);
        }
    }

    public void setData(String dataLabel, Object data){
        setData(dataLabel, data.toString());
    }

    public void setData(String dataLabel, String data){
        synchronized(lock){
            if (!currentLineItems.containsKey(dataLabel) && !newLineItems.containsKey(dataLabel)){
                newLineItems.put(dataLabel, new FadingRenderText(dataLabel));
            }

            displayDataTable.put(dataLabel, data);
            ticksSinceLastSet.put(dataLabel, 0);
        }
    }

    /**
     * @param logMessage The log is a special data entry that only ever grows. Use judiciously to avoid it getting huge
     */
    public void log(String logMessage){
        String currentLog = displayDataTable.getOrDefault("log", "");

        if (!currentLog.isEmpty()){
            currentLog+="\n"+logMessage;
        }else{
            currentLog=logMessage;
        }
        setData("log", currentLog);
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        if(!initialised){
            initialise();
        }
        if(!initialised){
            return;
        }


        if (!connectedToHands){
            connectToHands();
        }

        if(requestInitialiseStats){
            initialiseStats();
        }

        frameCounter ++;
        if (frameCounter >= 60) {
            long timeNowNanos =  System.nanoTime();
            float secondCounter = (timeNowNanos - nanosOfCountStart) / 1000000000f;
            int fps = (int) (frameCounter / secondCounter);
            setData("FPS", ""+fps);
            nanosOfCountStart = timeNowNanos;
            frameCounter = 0;

            if(statistics !=null){
                String[] labels = statistics.getLabels();

                int[] data = new int[labels.length];
                statistics.getData(data);
                for (int i = 0; i < labels.length; i++) {
                    setData(labels[i], ""+data[i]);
                }
            }
        }

        synchronized(lock){
            if (!newLineItems.isEmpty()){
                newLineItems.forEach((label, lineItem) -> {
                    lemurWindow.addChild(lineItem.render());
                    currentLineItems.put(label, lineItem);
                });
                newLineItems.clear();
            }

            currentLineItems.forEach((label, lineItem) -> lineItem.update(tpf));
        }

        ticksSinceLastSet.replaceAll((key, ticks) -> ticks+1);

        timeTillNextPositionCheck-=tpf;
        if (timeTillNextPositionCheck<=0){
            Vector3f headPosition = getVrCameraPosition(xrAppState);

            Vector3f relativeDebugPosition = debugWindowNode.getWorldTranslation().subtract(headPosition);
            double distance = relativeDebugPosition.length();
            if (distance>2*MAX_DISTANCE_FROM_PLAYER){
                autoSelectPosition(); //probably a teleport
            }else if (distance>MAX_DISTANCE_FROM_PLAYER){
                //pull the window closer
                debugWindowNode.setLocalTranslation(headPosition.add(relativeDebugPosition.normalize().mult(0.5f*MAX_DISTANCE_FROM_PLAYER)));
                debugWindowNode.lookAt(headPosition, Vector3f.UNIT_Y);
            }

            timeTillNextPositionCheck = 1;
        }

    }

    private static Vector3f getVrCameraPosition(XrBaseAppState vrAppState){
        return vrAppState.getVrCameraPosition();
    }

    private class NonFadingRenderText extends LineItem{
        Label labelNode = new Label("");

        public NonFadingRenderText(String label){
            super(label);
        }

        @Override
        public Node render(){
            return labelNode;
        }

        @Override
        public void update(double timeslice){
            super.update(timeslice);
            String oldText = labelNode.getText();
            String newText = label + ": " + displayDataTable.get(label);
            if (!oldText.equals(newText)){
                labelNode.setText(label + ": " + displayDataTable.get(label));
            }
        }

        public void setLabelColourEfficiently(ColorRGBA newColor){
            if (!labelNode.getColor().equals(newColor)){
                //setting colour triggers an expensive recalculate, don't do it unnecessarily
                labelNode.setColor(newColor);
            }
        }
    }

    private static class CallbackButtonBar extends LineItem{

        Container container = new Container(new BoxLayout(Axis.X, FillMode.None));

        public CallbackButtonBar(String label, Map<String,Runnable> buttonsAndEvents){
            super(label);

            container.addChild(new Label(label));

            buttonsAndEvents.forEach((buttonName, callback) -> {
                Button button=new Button(buttonName);
                button.addClickCommands(event -> callback.run());
                container.addChild(button);
            });
        }
        @Override
        public Node render(){
            return container;
        }
    }

    private class OptionButtonBar<U> extends LineItem{
        Map<U, Button> buttonMap = new HashMap<>();
        Container container = new Container(new BoxLayout(Axis.X, FillMode.None));

        public OptionButtonBar(String label, List<U> enumOptions){
            super(label);

            container.addChild(new Label(label));
            for(U item : enumOptions){
                Button button=new Button("");
                buttonMap.put(item, button);
                container.addChild(button);

                button.addClickCommands(source -> {
                    userEntryDataTable.put(label, item);
                    refreshButtonStates();

                });
            }
            refreshButtonStates();
        }

        private void refreshButtonStates(){
            U selectedObject = (U)userEntryDataTable.get(label);
            buttonMap.forEach((object, buttonToUpdate) ->{
                boolean selectedState = object ==selectedObject;
                buttonToUpdate.setEnabled(!selectedState);
                buttonToUpdate.setColor(selectedState ? ColorRGBA.White : ColorRGBA.Gray);
                buttonToUpdate.setText(object.toString()+(selectedState?"(x)":"( )"));
            });
        }

        @Override
        public Node render(){
            return container;
        }
    }

    private class FadingRenderText extends NonFadingRenderText{
        public FadingRenderText(String label){
            super(label);
        }

        @Override
        public void update(double timeslice){
            super.update(timeslice);
            int noOfticksSinceLastSet = ticksSinceLastSet.get(label);

            if (noOfticksSinceLastSet<3){
                setLabelColourEfficiently(ColorRGBA.White);
                labelNode.setColor(ColorRGBA.White);
            }else{
                if ( noOfticksSinceLastSet % 3 == 0){
                    float brightness = 1 - noOfticksSinceLastSet / (2 * 60f);
                    brightness = FastMath.clamp(brightness, 0.3f, 1);

                    ColorRGBA fadedColour = ColorRGBA.fromRGBA255((int) (255 * brightness), (int) (255 * brightness), (int) (255 * brightness), 255);

                    setLabelColourEfficiently(fadedColour);
                }
            }
        }
    }


    private abstract static class LineItem{
        String label;

        public LineItem(String label){
            this.label = label;
        }

        public abstract Node render();

        public void update(double timeslice){

        }
    }
}

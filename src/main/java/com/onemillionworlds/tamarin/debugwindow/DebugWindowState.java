package com.onemillionworlds.tamarin.debugwindow;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.VRAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The debug window creates a ui element that will render debug information to a panel.
 *
 * That panel will follow the player around but can be grabbed and moved if grab support is active in the hands.
 *
 * Requires lemur
 */
public class DebugWindowState extends BaseAppState{

    private final Object lock = new Object();

    /**
     * Because of the need to use this state for during development debugging this INSTANCE allows
     * the state to be statically accessed from anywhere
     */
    public static DebugWindowState INSTANCE;

    Node debugWindowNode = new Node("Tamarin-debug-window-node");
    Container lemurWindow = new Container();

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

    VRAppState vrAppState;
    StatsAppState statsAppState;

    protected float secondCounter = 0.0f;
    protected int frameCounter = 0;

    /**
     * if the DebugWindow is further away than this is is drawn towards the player
     */
    private static final float MAX_DISTANCE_FROM_PLAYER = 2;

    double timeTillNextPositionCheck = 0;

    public DebugWindowState(){
        assert INSTANCE == null : "Can only have 1 DebugWindowState";
        INSTANCE = this;
    }

    @Override
    protected void initialize(Application app){
        lemurWindow.addChild(new Label("Debug window"));
        debugWindowNode.attachChild(lemurWindow);
        lemurWindow.setLocalScale(0.0015f);

        lemurWindow.setLocalTranslation(-0.25f, -0.25f, 0); //a bit arbitrary, but likely to be about correct so a normal-sized window looks at the player right

        AbstractGrabControl control = new RelativeMovingGrabControl();
        debugWindowNode.addControl(control);

        vrAppState = getState(VRAppState.class);
        statsAppState = getState(StatsAppState.class);
        ((SimpleApplication)app).getRootNode().attachChild(debugWindowNode);
    }

    @Override
    protected void cleanup(Application app){
        debugWindowNode.removeFromParent();
    }

    @Override
    protected void onEnable(){
        debugWindowNode.setCullHint(Spatial.CullHint.Inherit);
    }

    @Override
    protected void onDisable(){
        debugWindowNode.setCullHint(Spatial.CullHint.Always);
    }

    public void showFtps(){
        newLineItems.put("FPS", new NonFadingRenderText("FPS"));
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

    @Override
    public void update(float tpf){
        super.update(tpf);

        secondCounter += getApplication().getTimer().getTimePerFrame();
        frameCounter ++;
        if (secondCounter >= 1.0f) {
            int fps = (int) (frameCounter / secondCounter);
            setData("FPS", ""+fps);
            secondCounter = 0.0f;
            frameCounter = 0;
        }

        synchronized(lock){
            if (!newLineItems.isEmpty()){
                newLineItems.forEach((label, lineItem) -> {
                    lemurWindow.addChild(lineItem.render());
                    currentLineItems.put(label, lineItem);
                });
            }

            currentLineItems.forEach((label, lineItem) -> {
                lineItem.update(tpf);
            });
        }

        ticksSinceLastSet.replaceAll((key, ticks) -> ticks+1);

        timeTillNextPositionCheck-=tpf;
        if (timeTillNextPositionCheck<=0){
            Vector3f headPosition = getVrCameraPosition(vrAppState);

            Vector3f relativeDebugPosition = debugWindowNode.getWorldTranslation().subtract(headPosition);
            double distance = relativeDebugPosition.length();
            if (distance>MAX_DISTANCE_FROM_PLAYER){
                //pull the window closer
                debugWindowNode.setLocalTranslation(headPosition.add(relativeDebugPosition.normalize().mult(0.5f*MAX_DISTANCE_FROM_PLAYER)));
                debugWindowNode.lookAt(headPosition, Vector3f.UNIT_Y);
            }

            timeTillNextPositionCheck = 1;
        }

    }

    private static Vector3f getVrCameraPosition(VRAppState vrAppState){
        return vrAppState.getVRViewManager().getLeftCamera().getLocation().add(vrAppState.getVRViewManager().getRightCamera().getLocation()).mult(0.5f);
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
            labelNode.setText(label + ": " + displayDataTable.get(label));
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
                labelNode.setColor(ColorRGBA.White);
            }else{
                float brightness = 1-noOfticksSinceLastSet/(2*60f);
                brightness = FastMath.clamp(brightness, 0.3f, 1);

                ColorRGBA fadedColour = ColorRGBA.fromRGBA255((int)(255*brightness), (int)(255*brightness),(int)(255*brightness),255);
                labelNode.setColor(fadedColour);
            }
        }
    }


    @AllArgsConstructor
    private abstract static class LineItem{
        String label;

        public abstract Node render();

        public void update(double timeslice){

        }
    }
}

package com.onemillionworlds.tamarin.debugwindow;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.VRAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The debug window creates a ui element that will render debug information to a panel.
 *
 * That panel will follow the player around but can be grabbed and moved if grab support is active in the hands.
 *
 * Requires lemur
 */
public class DebugWindowState extends BaseAppState{

    Node debugWindowNode = new Node("Tamarin-debug-window-node");
    Container lemurWindow = new Container();

    Map<String, String> dataTable = new ConcurrentHashMap<>();
    Map<String, Label> labels = new HashMap<>();

    VRAppState vrAppState;
    StatsAppState statsAppState;

    protected float secondCounter = 0.0f;
    protected int frameCounter = 0;

    /**
     * if the DebugWindow is further away than this is is drawn towards the player
     */
    private static final float MAX_DISTANCE_FROM_PLAYER = 2;

    double timeTillNextPositionCheck = 0;

    @Setter
    boolean showFps = true;

    @Override
    protected void initialize(Application app){
        lemurWindow.addChild(new Label("Debug window"));
        debugWindowNode.attachChild(lemurWindow);
        lemurWindow.setLocalScale(0.005f);

        //lemurWindow.setPreferredSize(new Vector3f(0.3f, 0.3f, 0.01f));

        lemurWindow.setLocalTranslation(-0.25f, -0.25f, 0); //a bit arbitrary, but likely to be about correct

        AutoMovingGrabControl control = new AutoMovingGrabControl(new Vector3f(0.2f, 0.2f, 0), 0.01f );
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

    public void setData(String dataLabel, String data){
        dataTable.put(dataLabel, data);
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        if (showFps) {
            secondCounter += getApplication().getTimer().getTimePerFrame();
            frameCounter ++;
            if (secondCounter >= 1.0f) {
                int fps = (int) (frameCounter / secondCounter);
                setData("FPS", ""+fps);
                secondCounter = 0.0f;
                frameCounter = 0;
            }
        }

        for(Map.Entry<String,String> dataEntry : dataTable.entrySet()){
            Label label = labels.get(dataEntry.getKey());
            if (label == null){
                label = new Label("");
                lemurWindow.addChild(label);
            }
            labels.put(dataEntry.getKey(), label);
            label.setText(dataEntry.getKey() + ": " + dataEntry.getValue());
        }

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
}

package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.compatibility.WrongActionTypeException;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.HandSide;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

public class GrabPickingFunction implements BoundHandFunction{

    private List<Vector3f> palmPickPoints = List.of(new Vector3f(0,0,0));

    private String grabAction;

    private Node nodeToGrabPickAgainst;

    private float timeSinceGrabbed;

    /**
     * How much time to let pass between the picking events that trigger grabs (and releases)
     */
    @Setter
    private float grabEvery;

    /**
     * Allows the amount of pressure required to pick something up to be changed.
     * A value between 0 and 1
     */
    private float minimumGripToTrigger = 0.5f;

    private boolean grabActionIsAnalog = true;

    private float lastGripPressure;

    Optional<AbstractGrabControl> currentlyGrabbed = Optional.empty();

    /**
     * This maximum distance that a grab pick will pick up an object.
     *
     * Note that this is the distance from the centre of the hand to the first face the pick line sees
     * @param maxGrabDistance a value in meters
     */
    @Setter
    private float maxGrabDistance = 0.1f;

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;

    public GrabPickingFunction(String grabAction, Node nodeToGrabPickAgainst){
        this.grabAction = grabAction;
        this.nodeToGrabPickAgainst = nodeToGrabPickAgainst;
    }

    /**
     * Sets the points (in the palm coordinate system) where pick lines should be generated from when the player
     * initiates a grab. Consider using {@link GrabPickingFunction#debugGrabPickLines()} to see where the lines you create go
     * @param palmPickPoints the points that grab pick lines emanate from
     */
    public void setPalmPickPoints(List<Vector3f> palmPickPoints){
        this.palmPickPoints = palmPickPoints;
    }


    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.class);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        currentlyGrabbed.ifPresent(grabbed -> grabbed.onRelease(boundHand));
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        timeSinceGrabbed+=timeSlice;
        if (timeSinceGrabbed>grabEvery){
            timeSinceGrabbed = 0;
            float gripPressure =  getGripActionPressure(boundHand, grabAction);

            //the lastGripPressure stuff is so that a clenched fist isn't constantly trying to grab things
            if (gripPressure>minimumGripToTrigger && lastGripPressure<minimumGripToTrigger && currentlyGrabbed.isEmpty()){
                //looking for things in the world to grab
                for(Vector3f palmPickPoint : palmPickPoints){
                    CollisionResults results = boundHand.pickPalm(nodeToGrabPickAgainst, palmPickPoint);
                    Spatial picked = null;
                    for(CollisionResult hit : results){
                        if(!Boolean.TRUE.equals(hit.getGeometry().getUserData(BoundHand.NO_PICK))){
                            if(hit.getDistance() < maxGrabDistance){
                                picked = hit.getGeometry();
                            }
                            break;
                        }
                    }
                    AbstractGrabControl grabControl = null;

                    while(picked != null && grabControl == null){
                        grabControl = picked.getControl(AbstractGrabControl.class);
                        picked = picked.getParent();
                    }

                    if(grabControl != null){
                        currentlyGrabbed = Optional.of(grabControl);
                        grabControl.onGrab(boundHand);
                        break;
                    }
                }

            }else if (gripPressure<minimumGripToTrigger && currentlyGrabbed.isPresent()){
                //drop current item
                currentlyGrabbed.get().onRelease(boundHand);
                currentlyGrabbed = Optional.empty();
            }
            lastGripPressure = gripPressure;
        }
    }

    /**
     * Adds a debug lines in the directions the hand would use for picking when grabbing
     *
     * Note that the red line is the palm origin, the pink lines are extra pick lines.
     *
     * Debug lines cannot be removed, they are for dev help only
     */
    public void debugGrabPickLines(){
        for(Vector3f palmPickPoints : palmPickPoints){
            ColorRGBA colour = palmPickPoints.equals(Vector3f.ZERO) ? ColorRGBA.Red : ColorRGBA.Pink;

            Geometry line = microLine(colour, new Vector3f(0,0,(boundHand.getHandSide() == HandSide.LEFT?1:-1)*maxGrabDistance));
            line.setLocalTranslation(palmPickPoints);
            boundHand.getPalmNode().attachChild(line);
        }
    }

    private float getGripActionPressure(BoundHand boundHand, String action){
        try{
            if (grabActionIsAnalog){
                AnalogActionState grabActionState = actionBasedOpenVrState.getAnalogActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.x;
            }else{
                DigitalActionState grabActionState = actionBasedOpenVrState.getDigitalActionState(action, boundHand.getHandSide().restrictToInputString);
                return grabActionState.state?1:0;
            }
        }catch(WrongActionTypeException wrongActionTypeException){
            //it's the opposite type of action, switch automatically, on the next update the correct type will be used
            grabActionIsAnalog = !grabActionIsAnalog;
            return 0;
        }
    }

    private Geometry microLine(ColorRGBA colorRGBA, Vector3f vector){
        Line line = new Line(new Vector3f(0, 0, 0), vector);
        Geometry geometry = new Geometry("debugHandLine", line);
        Material material = new Material(boundHand.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setLineWidth(5);
        material.setColor("Color", colorRGBA);
        geometry.setMaterial(material);
        geometry.setUserData(BoundHand.NO_PICK, true);
        return geometry;
    }
}

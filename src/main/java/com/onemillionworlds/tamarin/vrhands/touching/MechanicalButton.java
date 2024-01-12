package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.TerminateListener;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.ArrayList;
import java.util.List;

public class MechanicalButton extends Node{

    private float currentTravel = 0;

    /**
     * This boolean ensures that the button can only be pressed once without releasing.
     */
    private boolean availableForPress = true;

    private final ObservableEvent pressEvents = new ObservableEvent();

    private final List<Runnable> pressListeners = new ArrayList<>();

    private final Node movingNode = new Node("MechanicalButton_MovingNode");

    ButtonMovementAxis movementAxis;

    private final float geometrySurfaceDistanceFromOrigin;

    public MechanicalButton(Spatial buttonGeometry, ButtonMovementAxis movementAxis, float maximumButtonTravel, float resetTime){

        BoundingBox overallBoundsLocalisedToSpatialOrigin = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(buttonGeometry);

        geometrySurfaceDistanceFromOrigin = Math.min(
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMin(null)),
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMax(null))
        );

        //we want the top surface of the bounding box

        this.movementAxis = movementAxis;
        attachChild(movingNode);
        movingNode.attachChild(buttonGeometry);

        if(BoundHand.isLemurAvailable()){
            MouseEventControl mec = new MouseEventControl(){
                @Override
                public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){
                    if(event.isReleased()){
                        setTravel(maximumButtonTravel);
                        recordPressed();
                    }

                }
            };

            buttonGeometry.addControl(mec);
        }

        buttonGeometry.addControl(
                new AbstractTouchControl(){
                    @Override
                    protected void controlUpdate(float tpf){
                        getTouchingHand().ifPresent(hand -> {
                            Vector3f handPositionSpatialRelative = worldToLocal(hand.getIndexFingerTip_xPointing().getWorldTranslation(), null);

                            // Calculate the distance the finger tip is pressing along the movement axis
                            float pressDistance = movementAxis.extract(handPositionSpatialRelative) - geometrySurfaceDistanceFromOrigin + BoundHand.FINGER_PICK_SPHERE_RADIUS;

                            pressDistance = Math.min(maximumButtonTravel, pressDistance);
                            pressDistance = Math.max(0, pressDistance);
                            if(pressDistance>currentTravel){
                                setTravel(pressDistance);
                                if (pressDistance>=maximumButtonTravel && availableForPress){
                                    recordPressed();
                                }
                            }
                        });

                        // Reset button position over time
                        if (getTouchingHand().isEmpty()) {

                            if (currentTravel > 0) {
                                float distanceInThisTick = maximumButtonTravel * tpf/resetTime;
                                float newTravel = Math.max(0, currentTravel - distanceInThisTick);
                                setTravel(newTravel);
                            }
                        }
                    }
                }

        );
    }

    private void setTravel(float buttonTravel){
        this.currentTravel = buttonTravel;
        movingNode.setLocalTranslation(movementAxis.axisVector.mult(currentTravel));
        if (buttonTravel == 0){
            availableForPress = true;
        }
    }

    private void recordPressed(){
        pressEvents.fireEvent();
        for(Runnable listener : pressListeners){
            listener.run();
        }
    }

    /**
     * Obtains an object that can be queried to determine if the button has been pressed since the last queried.
     * Note that multiple events could be consolidated together if not checked regularly.
     *
     * <p>
     *     This is an ALTERNATIVE to adding a listener to the press event.
     * </p>
     * @return an object that can be queried to determine if the button has been pressed
     */
    public ObservableEventSubscription subscribeToPressEvents(){
        return pressEvents.subscribe();
    }

    /**
     * Adds a listener to the press event. The listener will be called every time the button is pressed.
     * <p>
     *     A TerminateListener is returned, calling this de-registers the listener.
     * </p>
     * <p>
     *     This is an ALTERNATIVE to subscribing to the press event.
     * </p>
     * @param listener a listerner that will be called immediately if the button is pressed.
     * @return a TerminateListener to de register the listener.
     */
    public TerminateListener addPressListener(Runnable listener){
        pressListeners.add(listener);
        return () -> pressListeners.remove(listener);
    }


    private static boolean isPrimarySix(Vector3f vector){
        return vector.isSimilar(Vector3f.UNIT_X, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Y, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Z, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_X.negate(), 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Y.negate(), 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Z.negate(), 0.001f);
    }

}

package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.lemursupport.NoPressMouseEventControl;
import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.TerminateListener;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.Haptic;
import com.simsilica.lemur.event.MouseEventControl;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MechanicalButton extends Node{

    @Getter
    private float currentTravel = 0;

    /**
     * This boolean ensures that the button can only be pressed once without releasing.
     */
    private boolean availableForPress = true;

    private final ObservableEvent pressEvents = new ObservableEvent();

    private final List<Runnable> pressListeners = new ArrayList<>();

    private final Node movingNode;

    ButtonMovementAxis movementAxis;

    private final float geometrySurfaceDistanceFromOrigin;

    private Optional<Haptic> hapticOnFullDepress = Optional.empty();

    @Setter
    private boolean useFullBoundingBoxBasedCollisions = true;

    /**
     * this isn't a performance optimisation (although it may also be that) it is to give more stable
     * collisions with the button if the player plunges their hand into it. The whole volume is collidable
     * rather than just the surface.
     */
    private final BoundingBox overallBoundsLocalisedToSpatialOrigin;

    private final Geometry representativeGeometry;

    public MechanicalButton(Spatial buttonGeometry, ButtonMovementAxis movementAxis, float maximumButtonTravel, float resetTime){
        representativeGeometry = findGeometry(buttonGeometry);
        assert  representativeGeometry != null : "Couldn't find a geometry in the spatial";
        overallBoundsLocalisedToSpatialOrigin = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(buttonGeometry);

        geometrySurfaceDistanceFromOrigin = Math.min(
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMin(null)),
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMax(null))
        );

        //we want the top surface of the bounding box

        this.movementAxis = movementAxis;
        movingNode= new Node("MechanicalButton_MovingNode"){
            @Override
            public int collideWith(Collidable other, CollisionResults results){
                if(useFullBoundingBoxBasedCollisions){
                    //this isn't a performance optimisation (although it may also be that) it is to give more stable
                    //collisions with the button if the player plunges their hand into it
                    if (other instanceof BoundingSphere boundingSphere){
                        BoundingSphere localSphere = new BoundingSphere(boundingSphere.getRadius(), this.worldToLocal(boundingSphere.getCenter(), null));
                        CollisionResults newResults = new CollisionResults();
                        overallBoundsLocalisedToSpatialOrigin.collideWith(localSphere, newResults);
                        for(int i=0;i<newResults.size();i++){
                            CollisionResult collisionResult = newResults.getCollisionDirect(i);
                            collisionResult.setGeometry(representativeGeometry);
                            results.addCollision(collisionResult);
                        }
                        return newResults.size();
                    }
                }
                return super.collideWith(other, results);
            }
        };

        attachChild(movingNode);
        movingNode.attachChild(buttonGeometry);

        if(BoundHand.isLemurAvailable()){
            //As we are also using a AbstractTouchControl use a NoPressMouseEventControl to avoid double handling
            MouseEventControl mec = new NoPressMouseEventControl(){
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
                                    hapticOnFullDepress.ifPresent(hand::triggerHapticAction);
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

    /**
     * Returns the first geometry it can recursively find in the spatial
     */
    private Geometry findGeometry(Spatial buttonGeometry){
        if (buttonGeometry instanceof Geometry){
            return (Geometry) buttonGeometry;
        }else if (buttonGeometry instanceof Node node){
            for(Spatial child : node.getChildren()){
                Geometry found = findGeometry(child);
                if (found != null){
                    return found;
                }
            }
        }
        return null;
    }

    public void setHapticOnFullDepress(Haptic hapticOnFullDepress){
        this.hapticOnFullDepress = Optional.ofNullable(hapticOnFullDepress);
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
        availableForPress = false;
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
    @SuppressWarnings("unused")
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


}

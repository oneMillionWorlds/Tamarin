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
import com.onemillionworlds.tamarin.observable.ObservableValue;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import com.onemillionworlds.tamarin.observable.TerminateListener;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.Haptic;
import com.simsilica.lemur.event.MouseEventControl;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MechanicalToggle extends Node{

    @Getter
    private float currentTravel = 0;

    private final ObservableValue<ToggleState> pressEvents = new ObservableValue<>(ToggleState.FULLY_OFF);

    private final ObservableValue<Boolean> majorPressEvents = new ObservableValue<>(false);

    private final List<Consumer<ToggleState>> pressListeners = new ArrayList<>();

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

    private final float toggleInTravel;

    @Getter
    private ToggleState currentState = ToggleState.FULLY_OFF;

    /**
     * If true then the button can be untoggled by pressing it again. If false then the button can only be untoggled
     * programmatically
     */
    @Setter
    private boolean allowedToBeUntoggled = true;

    public MechanicalToggle(Spatial buttonGeometry, ButtonMovementAxis movementAxis, float maximumButtonTravelPoint, float toggleInTravel, float resetTime){
        assert toggleInTravel<maximumButtonTravelPoint : "toggleInTravel must be less than maximumButtonTravel (its the half way point that the button will lock in at)";

        this.toggleInTravel = toggleInTravel;

        representativeGeometry = findGeometry(buttonGeometry);
        assert  representativeGeometry != null : "Couldn't find a geometry in the spatial";
        overallBoundsLocalisedToSpatialOrigin = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(buttonGeometry);

        geometrySurfaceDistanceFromOrigin = Math.min(
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMin(null)),
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMax(null))
        );

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
            MouseEventControl mec = new MouseEventControl(){
                @Override
                public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){
                    if(event.isReleased()){
                        if (currentState == ToggleState.FULLY_OFF){
                            setState(ToggleState.TOGGLED_ON);
                        }else if(allowedToBeUntoggled){
                            setState(ToggleState.FULLY_OFF);
                        }
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

                            pressDistance = Math.min(maximumButtonTravelPoint, pressDistance);
                            pressDistance = Math.max(0, pressDistance);
                            if(pressDistance>currentTravel){

                                if (pressDistance == maximumButtonTravelPoint){
                                    hapticOnFullDepress.ifPresent(hand::triggerHapticAction);
                                }
                                setTravel(pressDistance);
                            }
                        });

                        // Reset button position over time
                        if (getTouchingHand().isEmpty()) {

                            if (currentTravel>toggleInTravel){
                                if (currentState == ToggleState.FULLY_OFF){
                                    updateAndNotifyState(ToggleState.TRANSITIONING_ON);
                                } else if (currentState == ToggleState.TOGGLED_ON && allowedToBeUntoggled){
                                    updateAndNotifyState(ToggleState.TRANSITIONING_OFF);
                                }
                            }
                            float distanceInThisTick = maximumButtonTravelPoint * tpf/resetTime;
                            float newTravel = currentTravel;
                            if (currentState == ToggleState.TRANSITIONING_ON){
                                if (currentTravel>toggleInTravel){
                                    newTravel-=distanceInThisTick;
                                    if (newTravel<toggleInTravel){
                                        newTravel = toggleInTravel;
                                        updateAndNotifyState(ToggleState.TOGGLED_ON);
                                    }
                                }
                            }
                            if (currentState == ToggleState.TRANSITIONING_OFF || currentState == ToggleState.FULLY_OFF){
                                if (currentTravel>0){
                                    newTravel-=distanceInThisTick;
                                    if (newTravel<0){
                                        newTravel = 0;
                                        updateAndNotifyState(ToggleState.FULLY_OFF);
                                    }
                                }
                            }

                            setTravel(newTravel);
                        }
                    }
                }

        );
    }

    /**
     * Returns the first geometry it can recursively find in the spatial
     */
    private Geometry findGeometry(Spatial item){
        if (item instanceof Geometry buttonGeometry){
            return buttonGeometry;
        }else if (item instanceof Node node){
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
        if (currentTravel == buttonTravel){
            return;
        }
        this.currentTravel = buttonTravel;
        movingNode.setLocalTranslation(movementAxis.axisVector.mult(currentTravel));
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
    public ObservableValueSubscription<ToggleState> subscribeToPressEvents(){
        return pressEvents.subscribe();
    }

    /**
     * Obtains an object that can be queried to determine if the button has been pressed since the last queried.
     * Note that multiple events could be consolidated together if not checked regularly.
     *
     * <p>
     *     Unlike {@link MechanicalToggle#subscribeToPressEvents()} this only subscribes to the major change from on to
     *     off, not the minor {@link ToggleState#TRANSITIONING_OFF} to {@link ToggleState#FULLY_OFF} and similar
     * </p>
     *
     * <p>
     *     This is an ALTERNATIVE to adding a listener to the press event.
     * </p>
     * @return an object that can be queried to determine if the button has been pressed
     */
    @SuppressWarnings("unused")
    public ObservableValueSubscription<Boolean> subscribeToOnOffEvents(){
        return majorPressEvents.subscribe();
    }

    /**
     *
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
    @SuppressWarnings("UnusedReturnValue")
    public TerminateListener addPressListener(Consumer<ToggleState> listener){
        pressListeners.add(listener);
        return () -> pressListeners.remove(listener);
    }

    /**
     * This manually sets the state of the button. ToggleState.TOGGLED_ON and {@link ToggleState#FULLY_OFF} move
     * the button to those set points. {@link ToggleState#TRANSITIONING_OFF} allows the button to slowly relax from a
     * locked position.
     * <p>
     * {@link ToggleState#TRANSITIONING_OFF} is ignored if it is already {@link ToggleState#FULLY_OFF}
     * </p>
     *
     * @param state the new state
     */
    public void setState(ToggleState state){

        if (currentState == state){
            return;
        }
        if (currentState == ToggleState.FULLY_OFF && state == ToggleState.TRANSITIONING_OFF){
            return;
        }

        if (state == ToggleState.FULLY_OFF){
            setTravel(0);
        }else if (state == ToggleState.TOGGLED_ON){
            setTravel(toggleInTravel);
        }
        updateAndNotifyState(state);
    }

    private void updateAndNotifyState(ToggleState state){
        if (currentState == state){
            return;
        }
        ToggleState previousState = currentState;
        currentState = state;
        pressEvents.set(state);
        for(Consumer<ToggleState> listener : pressListeners){
            listener.accept(state);
        }
        if (previousState.isAKindOfOn() != currentState.isAKindOfOn()){
            majorPressEvents.set(currentState.isAKindOfOn());
        }
    }

    public enum ToggleState{
        FULLY_OFF,
        TOGGLED_ON,
        /**
         * If the button was TOGGLED_ON and then is further depressed it will move to this state. Once the finger is removed
         * it will relax to the off position
         */
        TRANSITIONING_OFF,

        /**
         * Once the finger has gone beyond the lock point the button will be in this state, once the finger is removed it
         * will relax towards the on mid-position.
         */
        TRANSITIONING_ON;

        public boolean isAKindOfOn(){
            return this == TOGGLED_ON || this == TRANSITIONING_ON;
        }
    }

}

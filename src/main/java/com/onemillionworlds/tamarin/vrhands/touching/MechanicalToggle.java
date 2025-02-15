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
import com.jme3.util.SafeArrayList;
import com.onemillionworlds.tamarin.lemursupport.NoPressMouseEventControl;
import com.onemillionworlds.tamarin.observable.ObservableValue;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import com.onemillionworlds.tamarin.observable.TerminateListener;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.Haptic;
import com.simsilica.lemur.event.MouseEventControl;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MechanicalToggle extends Node{

    private float currentTravel = 0;

    private final ObservableValue<ToggleState> pressEvents = new ObservableValue<>(ToggleState.FULLY_OFF);

    private final ObservableValue<Boolean> majorPressEvents = new ObservableValue<>(false);

    private final List<Consumer<ToggleState>> pressListeners = new SafeArrayList<>(castClass(Consumer.class));

    private final List<Runnable> tutorialModePressListeners = new SafeArrayList<>(castClass(Runnable.class));

    private boolean tutorialResetReadyToFire = true;

    private final Node movingNode;

    ButtonMovementAxis movementAxis;

    private final float geometrySurfaceDistanceFromOrigin;

    private Optional<Haptic> hapticOnFullDepress = Optional.empty();

    private boolean useFullBoundingBoxBasedCollisions = true;

    /**
     * this isn't a performance optimisation (although it may also be that) it is to give more stable
     * collisions with the button if the player plunges their hand into it. The whole volume is collidable
     * rather than just the surface.
     */
    private final BoundingBox overallBoundsLocalisedToSpatialOrigin;

    private final Geometry representativeGeometry;

    private final float toggleInTravel;

    private ToggleState currentState = ToggleState.FULLY_OFF;

    /**
     * If true then the button can be untoggled by pressing it again. If false then the button can only be untoggled
     * programmatically
     */
    private boolean allowedToBeUntoggled = true;

    private Supplier<EnablementState> enablementState = () -> EnablementState.ENABLED;

    public MechanicalToggle(Spatial buttonGeometry, ButtonMovementAxis movementAxis, float maximumButtonTravelPoint, float toggleInTravel, float resetTime){
        assert toggleInTravel < maximumButtonTravelPoint : "toggleInTravel must be less than maximumButtonTravel (its the half way point that the button will lock in at)";

        this.toggleInTravel = toggleInTravel;

        representativeGeometry = findGeometry(buttonGeometry);
        assert representativeGeometry != null : "Couldn't find a geometry in the spatial";
        overallBoundsLocalisedToSpatialOrigin = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(buttonGeometry);

        geometrySurfaceDistanceFromOrigin = Math.min(
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMin(null)),
                movementAxis.extract(overallBoundsLocalisedToSpatialOrigin.getMax(null))
        );

        this.movementAxis = movementAxis;
        movingNode = new Node("MechanicalButton_MovingNode"){
            @Override
            public int collideWith(Collidable other, CollisionResults results){
                if(useFullBoundingBoxBasedCollisions){
                    //this isn't a performance optimisation (although it may also be that) it is to give more stable
                    //collisions with the button if the player plunges their hand into it
                    if(other instanceof BoundingSphere boundingSphere){
                        BoundingSphere localSphere = new BoundingSphere(boundingSphere.getRadius(), this.worldToLocal(boundingSphere.getCenter(), null));
                        CollisionResults newResults = new CollisionResults();
                        overallBoundsLocalisedToSpatialOrigin.collideWith(localSphere, newResults);
                        for(int i = 0; i < newResults.size(); i++){
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
            MouseEventControl mec = new NoPressMouseEventControl(){
                @Override
                public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){
                    if(!event.isReleased()){
                        return;
                    }

                    EnablementState enablementState = getEnablementState();
                    if(enablementState == EnablementState.ENABLED){
                        if(currentState == ToggleState.FULLY_OFF){
                            setState(ToggleState.TOGGLED_ON);
                        } else if(allowedToBeUntoggled){
                            setState(ToggleState.FULLY_OFF);
                        }
                    }
                    if(enablementState == EnablementState.TUTORIAL_MODE){

                        for(Runnable listener : tutorialModePressListeners){
                            listener.run();
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
                        EnablementState enablementState = getEnablementState();
                        getTouchingHand().ifPresent(hand -> {
                            if(enablementState == EnablementState.DISABLED_LOCKED){
                                return;
                            }

                            Vector3f handPositionSpatialRelative = worldToLocal(hand.getIndexFingerTip_xPointing().getWorldTranslation(), null);

                            // Calculate the distance the finger tip is pressing along the movement axis
                            float pressDistance = movementAxis.extract(handPositionSpatialRelative) - geometrySurfaceDistanceFromOrigin + BoundHand.FINGER_PICK_SPHERE_RADIUS;

                            pressDistance = Math.min(maximumButtonTravelPoint, pressDistance);
                            pressDistance = Math.max(0, pressDistance);
                            if(pressDistance > currentTravel){

                                if(pressDistance == maximumButtonTravelPoint){
                                    hapticOnFullDepress.ifPresent(hand::triggerHapticAction);
                                }
                                setTravel(pressDistance);
                            }
                        });

                        // Reset button position over time
                        if(getTouchingHand().isEmpty()){

                            if(currentTravel > toggleInTravel){
                                if(enablementState == EnablementState.ENABLED){
                                    if(currentState == ToggleState.FULLY_OFF){
                                        updateAndNotifyState(ToggleState.TRANSITIONING_ON);
                                    } else if(currentState == ToggleState.TOGGLED_ON && allowedToBeUntoggled){
                                        updateAndNotifyState(ToggleState.TRANSITIONING_OFF);
                                    }
                                } else if(enablementState == EnablementState.TUTORIAL_MODE && tutorialResetReadyToFire){
                                    tutorialModePressListeners.forEach(Runnable::run);
                                    tutorialResetReadyToFire = false;
                                }
                            }
                            float distanceInThisTick = maximumButtonTravelPoint * tpf / resetTime;
                            float newTravel = currentTravel;
                            if(currentState == ToggleState.TRANSITIONING_ON){
                                if(currentTravel > toggleInTravel){
                                    newTravel -= distanceInThisTick;
                                    if(newTravel < toggleInTravel){
                                        newTravel = toggleInTravel;
                                        updateAndNotifyState(ToggleState.TOGGLED_ON);
                                    }
                                }
                            }

                            if(currentTravel <= 0){
                                tutorialResetReadyToFire = true;
                            }

                            if(currentState == ToggleState.TRANSITIONING_OFF || currentState == ToggleState.FULLY_OFF){
                                if(currentTravel > 0){
                                    newTravel -= distanceInThisTick;
                                    if(newTravel < 0){
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

    public EnablementState getEnablementState(){
        return enablementState.get();
    }

    public void setEnablementState(EnablementState enablementState){
        this.enablementState = () -> enablementState;
    }

    /**
     * Allows the enablement state to be dynamically determined. This is useful if the enablement state is dependent on
     * some wider state in the application (and you don't want to have to be called setEnablementState every time that
     * changes)
     *
     * @param enablementState a supplier that will be called to determine the enablement state of the button
     */
    public void setDynamicEnablementState(Supplier<EnablementState> enablementState){
        this.enablementState = enablementState;
    }

    /**
     * Returns the first geometry it can recursively find in the spatial
     */
    private Geometry findGeometry(Spatial item){
        if(item instanceof Geometry buttonGeometry){
            return buttonGeometry;
        } else if(item instanceof Node node){
            for(Spatial child : node.getChildren()){
                Geometry found = findGeometry(child);
                if(found != null){
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
        if(currentTravel == buttonTravel){
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
     * This is an ALTERNATIVE to adding a listener to the press event.
     * </p>
     *
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
     * Unlike {@link MechanicalToggle#subscribeToPressEvents()} this only subscribes to the major change from on to
     * off, not the minor {@link ToggleState#TRANSITIONING_OFF} to {@link ToggleState#FULLY_OFF} and similar
     * </p>
     *
     * <p>
     * This is an ALTERNATIVE to adding a listener to the press event.
     * </p>
     *
     * @return an object that can be queried to determine if the button has been pressed
     */
    @SuppressWarnings("unused")
    public ObservableValueSubscription<Boolean> subscribeToOnOffEvents(){
        return majorPressEvents.subscribe();
    }

    /**
     * Adds a listener to the press event. The listener will be called every time the button is pressed.
     * <p>
     * A TerminateListener is returned, calling this de-registers the listener.
     * </p>
     * <p>
     * This is an ALTERNATIVE to subscribing to the press event.
     * </p>
     *
     * @param listener a listerner that will be called immediately if the button is pressed.
     * @return a TerminateListener to de register the listener.
     */
    @SuppressWarnings("UnusedReturnValue")
    public TerminateListener addPressListener(Consumer<ToggleState> listener){
        pressListeners.add(listener);
        return () -> pressListeners.remove(listener);
    }

    /**
     * Adds a listener that will be called when the button is fully depressed in tutorial mode. This is useful if you
     * want to explain what the button does when it is pressed in tutorial mode.
     *
     * <p>
     * see {@link EnablementState#TUTORIAL_MODE} and {@link MechanicalToggle#setEnablementState(EnablementState)}
     * </p>
     *
     * @param listener the listener that will be called when the button is fully depressed in tutorial mode.
     * @return A TerminateListener is returned, calling this de-registers the listener.
     */
    public TerminateListener addTutorialModePressListener(Runnable listener){
        tutorialModePressListeners.add(listener);
        return () -> tutorialModePressListeners.remove(listener);
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

        if(currentState == state){
            return;
        }
        if(currentState == ToggleState.FULLY_OFF && state == ToggleState.TRANSITIONING_OFF){
            return;
        }

        if(state == ToggleState.FULLY_OFF){
            setTravel(0);
        } else if(state == ToggleState.TOGGLED_ON){
            setTravel(toggleInTravel);
        }
        updateAndNotifyState(state);
    }

    private void updateAndNotifyState(ToggleState state){
        if(currentState == state){
            return;
        }
        EnablementState enablementState = getEnablementState();
        if(enablementState != EnablementState.ENABLED){
            return;
        }

        ToggleState previousState = currentState;
        currentState = state;
        pressEvents.set(state);
        for(Consumer<ToggleState> listener : pressListeners){
            listener.accept(state);
        }
        if(previousState.isAKindOfOn() != currentState.isAKindOfOn()){
            majorPressEvents.set(currentState.isAKindOfOn());
        }
    }

    public float getCurrentTravel(){
        return this.currentTravel;
    }

    public ToggleState getCurrentState(){
        return this.currentState;
    }

    public void setUseFullBoundingBoxBasedCollisions(boolean useFullBoundingBoxBasedCollisions){
        this.useFullBoundingBoxBasedCollisions = useFullBoundingBoxBasedCollisions;
    }

    public void setAllowedToBeUntoggled(boolean allowedToBeUntoggled){
        this.allowedToBeUntoggled = allowedToBeUntoggled;
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

    @SuppressWarnings("unchecked") // Suppresses unchecked conversion warning
    private static <T> Class<T> castClass(Class<?> clazz){
        return (Class<T>) clazz;
    }

    public enum EnablementState{
        /**
         * Toggle button works, changes state
         */
        ENABLED,
        /**
         * Toggle button never changes state, but still visually moves when touched with the finger
         */
        DISABLED_MOVABLE,
        /**
         * Toggle button ignores the finger entirely, becomes static part of scene
         */
        DISABLED_LOCKED,

        /**
         * Toggle button never changes state, but still visually moves when touched with the finger. When
         * the button is fully depressed tutorialModePressListeners are called. The idea is that if your application is
         * in "help mode" the toggle is disabled but any press on the button will explain what the button does (which
         * must be hooked up by the application).
         *
         * <p>
         * See {@link MechanicalToggle#addTutorialModePressListener(Runnable)}
         * </p>
         */
        TUTORIAL_MODE
    }

}

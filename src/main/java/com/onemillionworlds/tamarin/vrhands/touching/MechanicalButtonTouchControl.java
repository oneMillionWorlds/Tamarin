package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.observable.ObservableEvent;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import com.onemillionworlds.tamarin.observable.TerminateListener;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.simsilica.lemur.event.MouseEventControl;


import java.util.ArrayList;
import java.util.List;

/**
 * This is a button that as it is touched by the finger starts to depress and when it is fully depressed the touch event
 * fires. It may optionally be configured to lock when depressed and then after annother press return to the original
 * position.
 * <p>
 *     Note that for use in dual VR/Desktop environments this button also responds to mouse clicks if lemur is available.
 * </p>
 */
public class MechanicalButtonTouchControl extends AbstractTouchControl{

    float boundingMaximumOnAxis;

    Vector3f movementAxis;

    Vector3f defaultTranslation;

    float maximumButtonTravel;

    float resetTime;

    /**
     * This boolean ensures that the button can only be pressed once without releasing.
     */
    boolean availableForPress = true;

    ObservableEvent pressEvents = new ObservableEvent();

    List<Runnable> pressListeners = new ArrayList<>();

    public MechanicalButtonTouchControl(Vector3f movementAxis, float maximumButtonTravel, float resetTime){
        if (isCardinal(movementAxis)){
            this.movementAxis = movementAxis;
            this.maximumButtonTravel = maximumButtonTravel;
            this.resetTime = resetTime;
        }else{
            throw new IllegalArgumentException("Movement axis must be one of the 6 cardinal directions");
        }
    }

    @Override
    public void setSpatial(Spatial spatial){
        super.setSpatial(spatial);
        defaultTranslation = spatial.getLocalTranslation().clone();
        refreshBoundingVolumeFromSpatial();

        if(BoundHand.isLemurAvailable()){
            MouseEventControl mec = new MouseEventControl(){
                @Override
                public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture){
                    if(event.isReleased()){
                        Vector3f newLocalTranslation = defaultTranslation.add(movementAxis.mult(maximumButtonTravel));
                        getSpatial().setLocalTranslation(newLocalTranslation);
                        recordPressed();
                    }

                }
            };

            spatial.addControl(mec);
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

    @Override
    protected void controlUpdate(float tpf){
        getTouchingHand().ifPresent(hand -> {
            Vector3f handPositionSpatialRelative = getSpatial().worldToLocal(hand.getIndexFingerTip_xPointing().getWorldTranslation(), null);
            float fingertipSize = BoundHand.FINGER_PICK_SPHERE_RADIUS;

            // Calculate the distance the finger tip is pressing along the movement axis
            float pressDistance = movementAxis.dot(handPositionSpatialRelative) - fingertipSize;
            Vector3f currentLocalTranslation = getSpatial().getLocalTranslation();
            float currentLocalPositionOnAxis = movementAxis.dot(currentLocalTranslation);

            if (pressDistance>=maximumButtonTravel && availableForPress){
                recordPressed();
            }

            // Calculate new position along movement axis
            float newPositionOnAxis = Math.min(currentLocalPositionOnAxis + pressDistance, maximumButtonTravel);

            // Update the position of the button
            Vector3f newLocalTranslation = defaultTranslation.add(movementAxis.mult(newPositionOnAxis));
            getSpatial().setLocalTranslation(newLocalTranslation);
        });

        // Reset button position over time
        if (getTouchingHand().isEmpty()) {
            Vector3f currentLocalTranslation = getSpatial().getLocalTranslation();
            Vector3f directionToDefault = defaultTranslation.subtract(currentLocalTranslation);
            float distanceToDefault = directionToDefault.length();

            if (distanceToDefault > 0) {
                float distanceInThisTick = maximumButtonTravel * tpf/resetTime;

                if (distanceInThisTick > distanceToDefault) {
                    availableForPress = true;
                    getSpatial().setLocalTranslation(defaultTranslation);
                }else{
                    getSpatial().setLocalTranslation(currentLocalTranslation.add(directionToDefault.normalize().mult(distanceInThisTick)));
                }
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp){}

    public void refreshBoundingVolumeFromSpatial(){
        BoundingBox boundingBox = getOverallBoundsLocalisedToSpatialOrigin(spatial);

        boundingMaximumOnAxis = Math.max(movementAxis.dot(boundingBox.getMax(null)), movementAxis.dot(boundingBox.getMin(null)));
    }

    /**
     * Calculates the overall bounding box for the given spatial, localized to the spatial's origin.
     * This function considers all child elements of the spatial, which may include nodes and geometries,
     * each potentially with their own local translations and rotations. The bounding box is adjusted to be
     * relative to the origin of the provided spatial.
     *
     * @param spatial the spatial for which to calculate the bounding box.
     * @return the bounding box localised to the spatial's origin.
     */
    protected static BoundingBox getOverallBoundsLocalisedToSpatialOrigin(Spatial spatial) {
        BoundingBox result = new BoundingBox();
        updateOverallBounds(spatial, new Vector3f(0, 0, 0), new Quaternion(), result);
        return result;
    }

    /**
     * Helper method to recursively update the bounding box based on child spatials.
     *
     * @param spatial the spatial to process.
     * @param parentTranslation accumulated translation from parent spatials.
     * @param parentRotation accumulated rotation from parent spatials.
     * @param overallBounds the bounding box being updated.
     */
    private static void updateOverallBounds(Spatial spatial, Vector3f parentTranslation, Quaternion parentRotation, BoundingBox overallBounds) {
        if (spatial == null) {
            return;
        }

        // Accumulate the local translation and rotation
        Vector3f localTranslation = spatial.getLocalTranslation();
        Quaternion localRotation = spatial.getLocalRotation();
        Vector3f accumulatedTranslation = parentRotation.mult(parentTranslation.add(localTranslation));
        Quaternion accumulatedRotation = parentRotation.mult(localRotation);

        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                updateOverallBounds(child, accumulatedTranslation, accumulatedRotation, overallBounds);
            }
        } else if (spatial instanceof Geometry geometry ) {
            BoundingBox localBounds = getBoundingBox(geometry);

            if (localBounds != null) {
                BoundingBox transformedBounds = transformBoundingBox(localBounds, accumulatedTranslation, accumulatedRotation);
                if (overallBounds.getYExtent() == 0 && overallBounds.getXExtent() == 0 && overallBounds.getZExtent() == 0){
                    //this is the first bounds we've seen
                    overallBounds.setCenter(transformedBounds.getCenter());
                    overallBounds.setXExtent(transformedBounds.getXExtent());
                    overallBounds.setYExtent(transformedBounds.getYExtent());
                    overallBounds.setZExtent(transformedBounds.getZExtent());
                }else{
                    overallBounds.mergeLocal(transformedBounds);
                }
            }
        }
    }

    private static BoundingBox getBoundingBox(Geometry geometry){
        BoundingBox localBounds;
        if (geometry.getModelBound() instanceof BoundingBox){
            localBounds = (BoundingBox) geometry.getModelBound();
        } else  if (geometry.getModelBound() instanceof BoundingSphere boundingSphere){
            localBounds = new BoundingBox(boundingSphere.getCenter(), boundingSphere.getRadius(), boundingSphere.getRadius(), boundingSphere.getRadius());
        } else {
            throw new RuntimeException("Unsupported bounding box type " + geometry.getModelBound().getClass().getName());
        }
        return localBounds;
    }

    /**
     * Transforms the bounding box by the given translation and rotation.
     *
     * @param box the bounding box to transform.
     * @param translation the translation to apply.
     * @param rotation the rotation to apply.
     * @return the transformed bounding box.
     */
    private static BoundingBox transformBoundingBox(BoundingBox box, Vector3f translation, Quaternion rotation) {
        // Transform the corners of the bounding box and find the new extents
        // Note: This is a simplified approach and may need refinement for complex rotations
        Vector3f min = box.getMin(null);
        Vector3f max = box.getMax(null);

        // Transform corners
        Vector3f[] corners = new Vector3f[] {
                min, max,
                new Vector3f(min.x, min.y, max.z),
                new Vector3f(min.x, max.y, min.z),
                new Vector3f(max.x, min.y, min.z),
                new Vector3f(min.x, max.y, max.z),
                new Vector3f(max.x, min.y, max.z),
                new Vector3f(max.x, max.y, min.z)
        };

        for(Vector3f vector3f : corners){
            rotation.multLocal(vector3f);
            vector3f.addLocal(translation);
        }

        // Find new bounding box extents
        BoundingBox transformedBox = new BoundingBox(corners[0], 0, 0, 0);
        for (Vector3f corner : corners) {
            transformedBox.mergeLocal(new BoundingBox(corner, 0, 0, 0));
        }

        return transformedBox;
    }

    private static boolean isCardinal(Vector3f vector){
        return vector.isSimilar(Vector3f.UNIT_X, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Y, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Z, 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_X.negate(), 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Y.negate(), 0.001f) ||
                vector.isSimilar(Vector3f.UNIT_Z.negate(), 0.001f);
    }
}

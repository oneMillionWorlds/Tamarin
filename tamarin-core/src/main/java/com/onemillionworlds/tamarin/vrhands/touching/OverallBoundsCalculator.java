package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class OverallBoundsCalculator{

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

        result.setCenter(result.getCenter().mult(spatial.getLocalScale()));
        result.setXExtent(result.getXExtent() * spatial.getLocalScale().x);
        result.setYExtent(result.getYExtent() * spatial.getLocalScale().y);
        result.setZExtent(result.getZExtent() * spatial.getLocalScale().z);
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


}

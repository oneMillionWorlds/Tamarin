package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.Transform;
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
        updateOverallBounds(spatial, Transform.IDENTITY, result);
        return result;
    }

    /**
     * Helper method to recursively update the bounding box based on child spatials.
     *
     * @param spatial the spatial to process.
     * @param parentTransform accumulated transform (scale, rotation and translation) from parent spatials.
     * @param overallBounds the bounding box being updated.
     */
    private static void updateOverallBounds(Spatial spatial, Transform parentTransform, BoundingBox overallBounds) {
        if (spatial == null) {
            return;
        }

        Transform accumulatedTransform = spatial.getLocalTransform().clone().combineWithParent(parentTransform);

        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                updateOverallBounds(child, accumulatedTransform, overallBounds);
            }
        } else if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            BoundingBox localBounds = getBoundingBox(geometry);

            if (localBounds != null) {
                BoundingBox transformedBounds = transformBoundingBox(localBounds, accumulatedTransform);
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
        } else  if (geometry.getModelBound() instanceof BoundingSphere){
            BoundingSphere boundingSphere = (BoundingSphere) geometry.getModelBound();
            localBounds = new BoundingBox(boundingSphere.getCenter(), boundingSphere.getRadius(), boundingSphere.getRadius(), boundingSphere.getRadius());
        } else {
            throw new RuntimeException("Unsupported bounding box type " + geometry.getModelBound().getClass().getName());
        }
        return localBounds;
    }

    /**
     * Transforms the bounding box by the given transform.
     *
     * @param box the bounding box to transform.
     * @param transform the scale, rotation and translation to apply.
     * @return the transformed bounding box.
     */
    private static BoundingBox transformBoundingBox(BoundingBox box, Transform transform) {
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
            transform.transformVector(vector3f, vector3f);
        }

        // Find new bounding box extents
        BoundingBox transformedBox = new BoundingBox(corners[0], 0, 0, 0);
        for (Vector3f corner : corners) {
            transformedBox.mergeLocal(new BoundingBox(corner, 0, 0, 0));
        }

        return transformedBox;
    }


}

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
     * Calculates the overall bounding box for the given spatial and all its descendants, taking into account every
     * scale, rotation and translation in the hierarchy. The spatial's own local transform is included, so the
     * result is in the coordinate space of the spatial's parent.
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
        Vector3f min = box.getMin(null);
        Vector3f max = box.getMax(null);

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

        BoundingBox transformedBox = new BoundingBox(corners[0], 0, 0, 0);
        for (Vector3f corner : corners) {
            transformedBox.mergeLocal(new BoundingBox(corner, 0, 0, 0));
        }

        return transformedBox;
    }

    /**
     * Converts a world space sphere (e.g. a fingertip) into the local space of the given spatial. The radius is
     * divided by the smallest component of the spatial's world scale, so under a non-uniform scale the local sphere
     * is large enough to contain the world sphere.
     */
    protected static BoundingSphere worldSphereToLocal(Spatial localSpace, BoundingSphere worldSphere){
        Vector3f worldScale = localSpace.getWorldScale();
        float smallestScale = Math.min(Math.abs(worldScale.x), Math.min(Math.abs(worldScale.y), Math.abs(worldScale.z)));
        return new BoundingSphere(worldSphere.getRadius() / smallestScale, localSpace.worldToLocal(worldSphere.getCenter(), null));
    }


}

package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.core.GuiControl;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Sorts the geometries of a 3D Lemur UI by their position in the UI's stacking order, rather than by distance from
 * the camera.
 * <p>
 * Lemur stacks a UI's elements towards +z in the local space of the UI's root (e.g. a button's background sits just in
 * front of the window background behind it). JME's default comparators sort by the camera distance to each geometry's
 * centre, and for nearly coplanar panels which one is "closer" depends on where the user is looking. With
 * semi-transparent styles (like glass) that makes the blended colour of a button change as the user moves their head.
 * </p>
 * <p>
 * All the geometries under the same Lemur root (the topmost ancestor with a {@link GuiControl}) are drawn together,
 * in increasing local z. Separate Lemur UIs are drawn furthest first. In the opaque bucket all Lemur UIs are drawn
 * after the rest of the bucket (which keeps its normal order) so that semi-transparent panels blend over the scene
 * behind them. In the transparent buckets Lemur UIs are interleaved with other geometries, furthest first, in the same
 * way JME's {@code TransparentComparator} orders them.
 * </p>
 * <p>
 * The ordering must be a consistent total order (JME's sort throws if it isn't), which is why Lemur and non-Lemur
 * geometries are never compared with the wrapped comparator.
 * </p>
 */
public class LemurHierarchyComparator implements GeometryComparator{

    private final GeometryComparator delegate;
    private final boolean lemurAfterOtherGeometries;
    private final Predicate<Spatial> isLemurElement;

    private Camera camera;

    private final Map<Geometry, Spatial> lemurRoots = new IdentityHashMap<>();
    private final Map<Spatial, Float> anchorDistances = new IdentityHashMap<>();
    private final Map<Geometry, Float> zInRoot = new IdentityHashMap<>();
    private final Vector3f temp = new Vector3f();

    /**
     * @param delegate the comparator used for geometries that aren't part of a Lemur UI, and to break ties within one.
     *                 Normally Lemur's {@code LayerComparator}, so that text is drawn after a background at the same z.
     * @param lemurAfterOtherGeometries true for the opaque bucket, false for the transparent and translucent buckets.
     */
    public LemurHierarchyComparator(GeometryComparator delegate, boolean lemurAfterOtherGeometries){
        this(delegate, lemurAfterOtherGeometries, spatial -> spatial.getControl(GuiControl.class) != null);
    }

    LemurHierarchyComparator(GeometryComparator delegate, boolean lemurAfterOtherGeometries, Predicate<Spatial> isLemurElement){
        this.delegate = delegate;
        this.lemurAfterOtherGeometries = lemurAfterOtherGeometries;
        this.isLemurElement = isLemurElement;
    }

    public GeometryComparator getDelegate(){
        return delegate;
    }

    @Override
    public void setCamera(Camera camera){
        this.camera = camera;
        lemurRoots.clear();
        anchorDistances.clear();
        zInRoot.clear();
        delegate.setCamera(camera);
    }

    @Override
    public int compare(Geometry a, Geometry b){
        Spatial rootA = getLemurRoot(a);
        Spatial rootB = getLemurRoot(b);

        if(lemurAfterOtherGeometries){
            if(rootA == null && rootB == null){
                return delegate.compare(a, b);
            }
            if(rootA == null){
                return -1;
            }
            if(rootB == null){
                return 1;
            }
        }

        Spatial anchorA = rootA == null ? a : rootA;
        Spatial anchorB = rootB == null ? b : rootB;

        if(anchorA != anchorB){
            int byDistance = Float.compare(getAnchorDistance(anchorB), getAnchorDistance(anchorA));
            if(byDistance != 0){
                return byDistance;
            }
            return Integer.compare(System.identityHashCode(anchorA), System.identityHashCode(anchorB));
        }
        if(rootA == null){
            return 0;
        }
        int byZ = Float.compare(getZInRoot(a, rootA), getZInRoot(b, rootA));
        if(byZ != 0){
            return byZ;
        }
        return delegate.compare(a, b);
    }

    private Spatial getLemurRoot(Geometry geometry){
        if(lemurRoots.containsKey(geometry)){
            return lemurRoots.get(geometry);
        }
        Spatial root = null;
        Spatial current = geometry;
        while(current != null){
            if(isLemurElement.test(current)){
                root = current;
            }
            current = current.getParent();
        }
        lemurRoots.put(geometry, root);
        return root;
    }

    private float getAnchorDistance(Spatial anchor){
        Float distance = anchorDistances.get(anchor);
        if(distance == null){
            BoundingVolume worldBound = anchor.getWorldBound();
            distance = worldBound == null ? anchor.getWorldTranslation().distance(camera.getLocation()) : worldBound.distanceToEdge(camera.getLocation());
            anchorDistances.put(anchor, distance);
        }
        return distance;
    }

    private float getZInRoot(Geometry geometry, Spatial root){
        Float z = zInRoot.get(geometry);
        if(z == null){
            BoundingVolume worldBound = geometry.getWorldBound();
            Vector3f worldCentre = worldBound == null ? geometry.getWorldTranslation() : worldBound.getCenter();
            z = root.worldToLocal(worldCentre, temp).z;
            zInRoot.put(geometry, z);
        }
        return z;
    }
}

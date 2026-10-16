package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.TransparentComparator;
import com.simsilica.lemur.LayerComparator;

/**
 * Installs comparators on a viewport so that Lemur UIs in it are drawn in the right order:
 * <ul>
 *     <li>Lemur's {@link LayerComparator}, which is what Lemur's {@code GuiGlobals.setupGuiComparators} does for the
 *     application's main viewport. Without it text can be drawn before the background behind it, and transparent
 *     parts of glyphs punch holes through the panels behind them.</li>
 *     <li>A {@link LemurHierarchyComparator} in the opaque and transparent buckets (if they still have JME's default
 *     comparators), so that the panels of a 3D Lemur UI are drawn back to front in the UI's own stacking order. Without
 *     it semi-transparent panels can change colour as the user moves their head.</li>
 * </ul>
 * <p>
 * Only load this class after checking Lemur is available.
 * </p>
 */
public class LemurLayerComparators{

    private static final RenderQueue.Bucket[] BUCKETS = {
            RenderQueue.Bucket.Opaque,
            RenderQueue.Bucket.Transparent,
            RenderQueue.Bucket.Translucent,
            RenderQueue.Bucket.Gui
    };

    /**
     * Wraps each of the viewport's bucket comparators (see the class Javadoc). Buckets that already have a
     * {@link LayerComparator} are left alone, so this is safe to call more than once.
     */
    public static void install(ViewPort viewPort){
        RenderQueue renderQueue = viewPort.getQueue();
        for(RenderQueue.Bucket bucket : BUCKETS){
            GeometryComparator existing = renderQueue.getGeometryComparator(bucket);
            if(!(existing instanceof LayerComparator)){
                renderQueue.setGeometryComparator(bucket, new LayerComparator(withHierarchyOrdering(bucket, existing), -1));
            }
        }
    }

    private static GeometryComparator withHierarchyOrdering(RenderQueue.Bucket bucket, GeometryComparator existing){
        if(bucket == RenderQueue.Bucket.Opaque && existing instanceof OpaqueComparator){
            return new LemurHierarchyComparator(existing, true);
        }
        if((bucket == RenderQueue.Bucket.Transparent || bucket == RenderQueue.Bucket.Translucent) && existing instanceof TransparentComparator){
            return new LemurHierarchyComparator(existing, false);
        }
        return existing;
    }
}

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
 *     <li>A {@link LemurHierarchyComparator} around that in the opaque and transparent buckets (if they still have
 *     JME's default comparators), so that each 3D Lemur UI is drawn as a whole, back to front in the UI's own stacking
 *     order. Without it semi-transparent panels can change colour as the user moves their head.</li>
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
     * Wraps each of the viewport's bucket comparators (see the class Javadoc). Buckets that have already been wrapped
     * are left alone, so this is safe to call more than once.
     */
    public static void install(ViewPort viewPort){
        RenderQueue renderQueue = viewPort.getQueue();
        for(RenderQueue.Bucket bucket : BUCKETS){
            GeometryComparator existing = renderQueue.getGeometryComparator(bucket);
            if(!(existing instanceof LayerComparator) && !(existing instanceof LemurHierarchyComparator)){
                renderQueue.setGeometryComparator(bucket, wrap(bucket, existing));
            }
        }
    }

    /**
     * The hierarchy ordering must be the outer comparator. Lemur's layers only order text above its own background,
     * so they must only apply within a single UI; applied across the whole bucket they draw all text after all
     * backgrounds, and text behind a nearer semi-transparent panel then fails the depth test and disappears.
     */
    private static GeometryComparator wrap(RenderQueue.Bucket bucket, GeometryComparator existing){
        LayerComparator layerComparator = new LayerComparator(existing, -1);
        if(bucket == RenderQueue.Bucket.Opaque && existing instanceof OpaqueComparator){
            return new LemurHierarchyComparator(layerComparator, true);
        }
        if((bucket == RenderQueue.Bucket.Transparent || bucket == RenderQueue.Bucket.Translucent) && existing instanceof TransparentComparator){
            return new LemurHierarchyComparator(layerComparator, false);
        }
        return layerComparator;
    }
}

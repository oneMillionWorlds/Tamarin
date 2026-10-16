package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.simsilica.lemur.LayerComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class LemurLayerComparatorsTest{

    @Test
    void installWrapsEveryBucketInALayerComparator(){
        ViewPort viewPort = new ViewPort("test", new Camera(10, 10));

        LemurLayerComparators.install(viewPort);

        RenderQueue queue = viewPort.getQueue();
        assertInstanceOf(LayerComparator.class, queue.getGeometryComparator(RenderQueue.Bucket.Opaque));
        assertInstanceOf(LayerComparator.class, queue.getGeometryComparator(RenderQueue.Bucket.Transparent));
        assertInstanceOf(LayerComparator.class, queue.getGeometryComparator(RenderQueue.Bucket.Translucent));
        assertInstanceOf(LayerComparator.class, queue.getGeometryComparator(RenderQueue.Bucket.Gui));
    }

    @Test
    void installingTwiceDoesNotDoubleWrap(){
        ViewPort viewPort = new ViewPort("test", new Camera(10, 10));
        LemurLayerComparators.install(viewPort);
        GeometryComparator afterFirstInstall = viewPort.getQueue().getGeometryComparator(RenderQueue.Bucket.Opaque);

        LemurLayerComparators.install(viewPort);

        assertSame(afterFirstInstall, viewPort.getQueue().getGeometryComparator(RenderQueue.Bucket.Opaque));
    }
}

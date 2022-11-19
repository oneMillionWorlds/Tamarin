package com.onemillionworlds.tamarin.deferredattachment;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * VR applications can be very sensitive to frame rate drops. One cause of frame rate drops is geometries being
 * attached that then immediately need to have their collision data calculated. The DeferredAttachmentService ensures the
 * geometry is "ready" before being attached. The rest of the application is allowed to continue so anticipate the
 * geometry not appearing for a while
 */
public class DeferredAttachmentService extends BaseAppState{
    public static final String ID = "DeferredAttachmentService";

    private static final Logger logger = Logger.getLogger(DeferredAttachmentService.class.getName());

    ExecutorService executor;

    List<NodeData> autoAttachings = new CopyOnWriteArrayList<>();

    public DeferredAttachmentService(){
        super(ID);
    }

    @Override
    protected void initialize(Application app){
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    protected void cleanup(Application app){
        executor.shutdown();
    }

    public void attachWhenReady(Node nodeToAttachTo, Spatial itemToAttachWhenReady){
        Future<Spatial> preparement = prepareSpatial(itemToAttachWhenReady);
        autoAttachings.add(new NodeData(preparement, nodeToAttachTo));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        for(NodeData inProgressItem : autoAttachings){
            if (inProgressItem.getNodePreparationFuture().isDone()){
                try{
                    inProgressItem.getNodeToAttachTo().attachChild(inProgressItem.getNodePreparationFuture().get());
                } catch(InterruptedException|ExecutionException e){
                    logger.log( Level.WARNING, "Error while retrieving prepared spatial", e);
                }
            }
        }
    }

    /**
     * Registers a task (whose completion can be queried using the future) to generate all the collision data and
     * push buffers to graphics memory on a separate thread. On the futures completion the node should be safe to add
     * to the scene graph without causing performance hiccoughs
     */
    public Future<Spatial> prepareSpatial(Spatial spatialToPrepare){
        return executor.submit(() ->{
            generateCollisionData(spatialToPrepare);
            return spatialToPrepare;
        });
    }

    private void generateCollisionData(Spatial spatialToPrepare){
        if (spatialToPrepare instanceof Geometry){
            ((Geometry)spatialToPrepare).getMesh().createCollisionData();
        }else if (spatialToPrepare instanceof Node) {
            for(Spatial child : ((Node)spatialToPrepare).getChildren()){
                generateCollisionData(child);
            }
        }
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    @Value
    private static class NodeData{
        Future<Spatial> nodePreparationFuture;
        Node nodeToAttachTo;
    }
}

package com.onemillionworlds.tamarin.deferredattachment;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import lombok.Setter;
import lombok.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
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

    ConcurrentLinkedQueue<NodeData> autoAttachings = new ConcurrentLinkedQueue<>();

    private final int numberOfThreads;

    public DeferredAttachmentService(){
        this(1);
    }

    public DeferredAttachmentService(int numberOfThreads){
        super(ID);
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    protected void initialize(Application app){
        startExecutorIfRequired();
    }

    private void startExecutorIfRequired(){
        if(executor == null){
            executor = Executors.newFixedThreadPool(numberOfThreads, r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });
        }
    }

    @Override
    protected void cleanup(Application app){
        if(executor != null){
            executor.shutdown();
        }
    }

    /**
     * This will attach the item to the node once all its collision data is produced. The idea being that if it was just
     * attached then it might cause a performance hiccup, this avoids that.
     *
     * <p>
     *     The generation of the collision data is done on a separate thread so the rest of the application can continue but
     *     the attachment itself will happen on the main thread so it is safe to attach to the scene graph
     * </p>
     *
     * @param nodeToAttachTo the node to attach the spatial to once the spatial is ready
     * @param itemToAttachWhenReady a spatial that is to have its collision data calculated on annother thread
     */
    public void attachWhenReady(Node nodeToAttachTo, Spatial itemToAttachWhenReady){
        Future<Spatial> preparement = prepareSpatial(itemToAttachWhenReady);
        autoAttachings.add(new NodeData(preparement, nodeToAttachTo));
    }

    /**
     * This will build the spatial in another thread, generate all its collision data.
     * Then once done it will attach the item to the node. The idea being that if it was just
     * attached then it might cause a performance hiccup, this avoids that.
     *
     * <p>
     *     The generation is done on a separate thread so the rest of the application can continue but
     *     the attachment itself will happen on the main thread so it is safe to attach to the scene graph
     * </p>
     *
     * @param nodeToAttachTo the node to attach the spatial to once the spatial is ready
     * @param itemToAttachWhenReady a builder for the spatial
     */
    public void attachWhenReady(Node nodeToAttachTo, Supplier<Spatial> itemToAttachWhenReady){
        Future<Spatial> preparement = prepareSpatial(itemToAttachWhenReady);
        autoAttachings.add(new NodeData(preparement, nodeToAttachTo));
    }

    /**
     * This will build the spatial in another thread, generate all its collision data.
     * Then once done it will attach the item to the node (which is immediately returned but initially empty.
     * The idea being that if it was just attached then it might cause a performance hiccup, this avoids that.
     *
     * <p>
     *     The generation is done on a separate thread so the rest of the application can continue but
     *     the attachment itself will happen on the main thread so it is safe to attach to the scene graph
     * </p>
     *
     * <p>
     *     IMPORTANT! You do need to avoid cloning the node until its filled in (otherwise you'll get a clone of an empty node).
     *     Use {@link DeferredAttachmentService#attachWhenReadyWithSafeClone(Supplier)} if you need to clone the node.
     *     That keeps track of the cloning and will attach cloned children when ready.
     * </p>
     *
     * @param itemToAttachWhenReady a builder for the spatial
     * @return a node that will initially be empty, but once the spatial is ready that spatial will be attached
     */
    public Node attachWhenReady(Supplier<Spatial> itemToAttachWhenReady){
        Node node = new Node("DeferredAttachmentNode");
        attachWhenReady(node, itemToAttachWhenReady);
        return node;
    }

    /**
     * This will build the spatial in another thread, generate all its collision data.
     * Then once done it will attach the item to the node (which is immediately returned but initially empty.
     * The idea being that if it was just attached then it might cause a performance hiccup, this avoids that.
     *
     * <p>
     *     The generation is done on a separate thread so the rest of the application can continue but
     *     the attachment itself will happen on the main thread so it is safe to attach to the scene graph
     * </p>
     *
     * <p>
     *     The returned node CAN be cloned and the clone will initially be empty (if this is empty) but will be
     *     filled in when the item is ready.
     * </p>
     *
     * @param itemToAttachWhenReady a builder for the spatial
     * @return a node that will initially be empty, but once the spatial is ready that spatial will be attached
     */
    public Node attachWhenReadyWithSafeClone(Supplier<Spatial> itemToAttachWhenReady){
        NodeWithSafeClone node = new NodeWithSafeClone("DeferredAttachmentNode");

        Future<Spatial> preparement = prepareSpatial(itemToAttachWhenReady);
        NodeData nodeData = new NodeData(preparement, node);
        node.setNodeData(nodeData);
        autoAttachings.add(nodeData);
        return node;
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        Iterator<NodeData> iterator = autoAttachings.iterator();
        while(iterator.hasNext()){
            NodeData inProgressItem = iterator.next();
            if (inProgressItem.getNodePreparationFuture().isDone()){
                try{
                    inProgressItem.attach();
                } catch(InterruptedException|ExecutionException e){
                    logger.log( Level.WARNING, "Error while retrieving prepared spatial", e);
                }
                iterator.remove();
            }
        }
    }

    /**
     * Registers a task (whose completion can be queried using the future) to generate all the collision data on a separate thread.
     * On the futures completion the node should be safe to add to the scene graph without causing performance hiccoughs
     */
    public Future<Spatial> prepareSpatial(Spatial spatialToPrepare){
        startExecutorIfRequired();
        return executor.submit(() ->{
            generateCollisionData(spatialToPrepare);
            return spatialToPrepare;
        });
    }

    /**
     * Registers a task (whose completion can be queried using the future) to generate the spatial, all the collision data and
     * push buffers to graphics memory on a separate thread. On the futures completion the node should be safe to add
     * to the scene graph without causing performance hiccoughs
     */
    public Future<Spatial> prepareSpatial(Supplier<Spatial> spatialToPrepare){
        startExecutorIfRequired();
        return executor.submit(() ->{
            Spatial spatial = spatialToPrepare.get();
            generateCollisionData(spatial);
            return spatial;
        });
    }

    private void generateCollisionData(Spatial spatialToPrepare){
        CollisionResults collisionResult = new CollisionResults();
        //a sphere that won't actually collide with anything, to avoid unnecessary calculation
        BoundingSphere boundingSphere = new BoundingSphere(0.01f, new Vector3f(1000000,1000000,100000));

        if (spatialToPrepare instanceof Geometry geometry){
            //by doing a geometry.collideWith (rather than geometry.getMesh().createCollisionData()) the collision
            //data is calculated only if not already calculated
            geometry.collideWith(boundingSphere, collisionResult);
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
        Object lock = new Object();
        List<Node> nodeToCloneAttachTo = new ArrayList<>();
        List<Node> nodeToCloneAttachToCloningMaterials = new ArrayList<>();

        public NodeData(Future<Spatial> nodePreparationFuture, Node nodeToAttachTo){
            this.nodePreparationFuture = nodePreparationFuture;
            this.nodeToAttachTo = nodeToAttachTo;
        }

        public void attach() throws InterruptedException, ExecutionException{
            if (nodeToCloneAttachTo.isEmpty() && nodeToCloneAttachToCloningMaterials.isEmpty()){
                getNodeToAttachTo().attachChild(getNodePreparationFuture().get());
            }else{
                Spatial spatial = getNodePreparationFuture().get();
                synchronized(lock){
                    for(Node node : nodeToCloneAttachTo){
                        node.attachChild(spatial.clone(false));
                    }
                    for(Node node : nodeToCloneAttachToCloningMaterials){
                        node.attachChild(spatial.clone(true));
                    }
                    getNodeToAttachTo().attachChild(getNodePreparationFuture().get());
                }
            }
        }
    }

    /**
     * This node is aware its contents may be attached later and clones those contents when attached
     */
    public static class NodeWithSafeClone extends Node{
        @Setter
        private NodeData nodeData;
        private NodeWithSafeClone(String name){
            super(name);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Spatial clone(){
            return clone(true);
        }

        @Override
        public Node clone(boolean cloneMaterials){
            if (getChildren().isEmpty()){
                synchronized(nodeData.lock){
                    Node clone = new Node(getName());
                    if (cloneMaterials){
                        nodeData.nodeToCloneAttachToCloningMaterials.add(clone);
                    }else{
                        nodeData.nodeToCloneAttachTo.add(clone);
                    }
                    return clone;
                }
            }else{
                return super.clone(true);
            }
        }
    }
}

package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.BoneStance;
import com.onemillionworlds.tamarin.compatibility.HandMode;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;
import com.onemillionworlds.tamarin.vrhands.functions.LemurClickFunction;
import com.onemillionworlds.tamarin.vrhands.functions.PickMarkerFunction;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class BoundHand{

    private static boolean lemurCheckedAvailable = false ;

    public static String NO_PICK = "noPick";

    private HandMode handMode = HandMode.WITHOUT_CONTROLLER;

    private final Node rawOpenVrPosition = new Node();

    private final Node geometryNode = new Node();

    /**
     * Returns a node that will update with the hands position and rotation.
     *
     * This node has an orientation such that x aligns with the hands pointing direction, Y pointing upwards and Z
     * pointing to the right
     *
     * This is an ideal node for things like picking lines, which can be put in the x direction
     *
     * Note that the (0,0,0) position is just in front of the thumb, not the centre of the hand.
     *
     * This node is primarily used for picking, but if you want a node to attach to that only cares about the bulk
     * hand position
     *
     */
    @Getter
    private final Node handNode_xPointing = new Node();

    /**
     * A hand node, with +z pointing in the direction of the bulk hand. This is used primarily for direct lemur interactions
     */
    @Getter
    private final Node handNode_zPointing = new Node();

    private final Node pickLineNode = new Node();

    /**
     * This is a node that sits on the palm (but near the fingers) whose +x points out way from the palm (towards whatever
     * would be grabbed). It is live updated based on the skeleton positions so its exact relations to other nodes may change as
     * the node moves.
     *
     */
    private final Node palmNode_xPointing = new Node();

    private final Node debugPointsNode = new Node();

    /**
     * The hand will be detached from the scene graph and will no longer receive updates
     */
    public abstract void unbindHand();

    private final String postActionName;

    private final String skeletonActionName;

    private final Armature armature;

    @Getter
    private final AssetManager assetManager;

    private final HandSide handSide;

    private final ActionBasedOpenVrState vrState;

    /**
     * Debug points add markers onto the hands where the bone positions are, and their directions
     */
    private boolean debugPoints = false;

    private float baseSkinDepth = 0.02f;

    Map<Class<? extends BoundHandFunction>, BoundHandFunction> functions = new HashMap<>();

    public BoundHand(ActionBasedOpenVrState vrState, String postActionName, String skeletonActionName, Spatial handGeometry, Armature armature, AssetManager assetManager, HandSide handSide){
        this.vrState = Objects.requireNonNull(vrState);
        this.geometryNode.attachChild(handGeometry);
        this.postActionName = postActionName;
        this.skeletonActionName = skeletonActionName;
        this.armature = armature;
        this.assetManager = assetManager;
        this.handSide = handSide;
        this.rawOpenVrPosition.attachChild(handNode_xPointing);
        this.rawOpenVrPosition.attachChild(debugPointsNode);

        searchForGeometry(handGeometry).forEach(g -> g.setUserData(NO_PICK, true));

        Quaternion naturalRotation = new Quaternion();
        naturalRotation.fromAngleAxis(-0.75f* FastMath.PI, Vector3f.UNIT_X);
        Quaternion zToXRotation = new Quaternion();
        zToXRotation.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_Z);
        Quaternion rotateAxes = new Quaternion();
        rotateAxes.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_X);

        handNode_xPointing.setLocalRotation(naturalRotation.mult(zToXRotation).mult(rotateAxes));

        Quaternion xPointingToZPointing = new Quaternion();
        xPointingToZPointing.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        handNode_zPointing.setLocalRotation(xPointingToZPointing);
        handNode_xPointing.attachChild(handNode_zPointing);

        rawOpenVrPosition.attachChild(palmNode_xPointing);

        rawOpenVrPosition.attachChild(geometryNode);

        handNode_xPointing.attachChild(pickLineNode);

        addFunction(new PickMarkerFunction());
    }

    public HandMode getHandMode(){
        return handMode;
    }

    /**
     * Returns a node that will update with the hands position and rotation. If you are dealing with raw bone positions
     * they are in this coordinate system.
     *
     * Note that this is in the orientation that OpenVR provides which isn't very helpful
     * (it doesn't map well to the direction the hand is pointing for example)
     *
     * You probably don't want this and probably want {@link BoundHand#getHandNode_xPointing}
     * which has a more natural rotation. Unless you are dealing with raw bone positions, which are in this coordinate
     * system.
     *
     * @return the raw hand node
     */
    public Node getRawOpenVrNode(){
        return rawOpenVrPosition;
    }


    /**
     * Returns a node that will update with the hands position and rotation.
     *
     * This node has an orientation such that x aligns with the hands pointing direction, Y pointing upwards and Z
     * pointing to the right, as defined by the middle finger metacarpal bone. Its X,Y and Z are likely to be in similar
     * directions to those of {@link BoundHand#getHandNode_xPointing()} but not precisely
     * @return the palm node
     */
    public Node getPalmNode(){
        return palmNode_xPointing;
    }

    /**
     * The way the hand is being held, see javadoc on HandMode itself for more details
     * @param handMode the handMode
     */
    public void setHandMode(HandMode handMode){
        this.handMode = handMode;
    }

    public String getPostActionName(){
        return postActionName;
    }

    public String getSkeletonActionName(){
        return skeletonActionName;
    }

    public Armature getArmature(){
        return armature;
    }

    /**
     * The material that will be applied to any geometries within the spatial provided as the hand
     * @param material the material
     */
    public void setMaterial(Material material){
        searchForGeometry(geometryNode).forEach(g -> g.setMaterial(material));
    }

    /**
     * Adds the requested
     * @param function the functionality to add (things like grabbing, clicking etc are implemented as BoundHandFunctions
     * @return a method that if called will remove the function
     */
    public Runnable addFunction(BoundHandFunction function){
        function.onBind(this, vrState.getStateManager());
        functions.put(function.getClass(), function);
        return () -> removeFunction(function.getClass());
    }

    public void removeFunction(Class<? extends BoundHandFunction> functionToRemove){
        BoundHandFunction function = functions.remove(functionToRemove);
        if (function!=null){
            function.onUnbind(this, vrState.getStateManager());
        }
    }

    public <T extends BoundHandFunction> T getFunction(Class<T> function){
        @SuppressWarnings("unchecked")
        T functionClass = (T) functions.get(function);
        Objects.requireNonNull(functionClass, () -> "Function " + function.getSimpleName() + " missing");
        return functionClass;
    }

    protected void update(float timeSlice, Map<String, BoneStance> boneStances){
        if (debugPoints){
            debugPointsNode.detachAllChildren();
            debugPointsNode.attachChild(armatureToNodes(getArmature(), ColorRGBA.Red));
        }
        updatePalm(timeSlice, boneStances);
        functions.values().forEach(f -> f.update(timeSlice, this, vrState.getStateManager()));

    }

    private void updatePalm(float timeSlice, Map<String, BoneStance> boneStances){
        //the palm node is put at the position between the finger_middle_0_l bone and finger_middle_meta_l, but with the
        // rotation of the finger_middle_meta_l bone. This gives roughly the position of a grab point, with a sensible rotation
        String proximalName = handSide == HandSide.LEFT ? "finger_middle_0_l" : "finger_middle_0_r";
        String metacarpalName = handSide == HandSide.LEFT ? "finger_middle_meta_l" : "finger_middle_meta_r";

        BoneStance metacarpel = boneStances.get(metacarpalName);
        BoneStance proximal = boneStances.get(proximalName);
        if (metacarpel != null){

            Quaternion coordinateStandardisingRotation;
            if (handSide == HandSide.LEFT){
                coordinateStandardisingRotation = new Quaternion();
                coordinateStandardisingRotation.fromAngleAxis(-0.5f*FastMath.PI, Vector3f.UNIT_X);
            }else{
                Quaternion aboutY = new Quaternion();
                aboutY.fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);

                Quaternion aboutX = new Quaternion();
                aboutX.fromAngleAxis(-0.5f*FastMath.PI, Vector3f.UNIT_X);

                coordinateStandardisingRotation = aboutY.mult(aboutX);
            }

            palmNode_xPointing.setLocalTranslation(proximal.position.add(metacarpel.position).multLocal(0.5f));
            palmNode_xPointing.setLocalRotation(metacarpel.orientation.mult(coordinateStandardisingRotation));
        }
    }

    /**
     * Picks from a point just in front of the thumb (the point the getHandNode_zPointing() is at) in the direction
     * out away from the hand.
     *
     * Note that the geometry of the hand itself may be the first result from the pick but more reasonable pick results
     * will follow. (These will have NO_PICK = true as userdata which can be used to ignore them)
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    public CollisionResults pickBulkHand(Node nodeToPickAgainst){
        Vector3f pickOrigin = getHandNode_xPointing().getWorldTranslation();
        Vector3f pickingPoint = getHandNode_xPointing().localToWorld(new Vector3f(1,0,0), null);
        Vector3f pickingVector = pickingPoint.subtract(pickOrigin);
        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(pickOrigin, pickingVector);

        nodeToPickAgainst.collideWith(ray, results);
        return results;
    }

    /**
     * This will set the hand to do a pick in the same direction as {@link BoundHand#pickBulkHand}/lemur clicks
     * and place a marker (by default a white sphere) at the point where the pick hits a geometry. This gives the
     * player an indication what they would pick it they clicked now; think of it like a mouse pointer in 3d space
     */
    public void setPickMarkerContinuous(Node nodeToPickAgainst){
        getFunction(PickMarkerFunction.class).setPickMarkerContinuous(nodeToPickAgainst);
    }

    /**
     * This will stop that action started by {@link BoundHand#setPickMarkerContinuous}
     */
    public void clearPickMarkerContinuous(){
        getFunction(PickMarkerFunction.class).clearPickMarkerContinuous();
    }

    /**
     * Picks from roughly the centre of the palm out from the palm (i.e. for the left hand it points right)
     *
     * This can be useful to use picking to determine what the player wishes to grab
     *
     * Note that the geometry of the hand itself may be the first result from the pick but more reasonable pick results
     * will follow. (These will have NO_PICK = true as userdata which can be used to ignore them)
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    public CollisionResults pickPalm(Node nodeToPickAgainst){
        return pickPalm(nodeToPickAgainst, Vector3f.ZERO);
    }

    /**
     * Picks outward away from the palm palmRelativePosition to {@link BoundHand#getPalmNode()}.
     * Note that x is towards the fingers, y is up and z is right (whether +z is the palm direction depends on if this
     * is the left or right hand)
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     * @param palmRelativePosition the position for the pick line to start
     */
    public CollisionResults pickPalm(Node nodeToPickAgainst, Vector3f palmRelativePosition){
        Vector3f pickOrigin = getPalmNode().localToWorld(palmRelativePosition, null);
        Vector3f pickingPoint = getPalmNode().localToWorld(palmRelativePosition.add(0,0,handSide == HandSide.LEFT?1:-1), null);
        Vector3f pickingVector = pickingPoint.subtract(pickOrigin);
        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(pickOrigin, pickingVector);

        nodeToPickAgainst.collideWith(ray, results);
        return results;
    }

    /**
     * @return Left or right hand
     */
    public HandSide getHandSide(){
        return handSide;
    }

    /**
     * Will start rendering the positions of the bones (note if all is well they will be inside the hands, so not really
     * visible (doing this is not performant, debug only)
     */
    public void debugArmature(){
        this.debugPoints = true;
    }

    /**
     * Adds a debug line in the direction the hand would use for picking or clicking
     */
    public void debugPointingPickLine(){
        handNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0.25f,0,0)));
    }

    /**
     * Adds green (x), yellow (y) and red (x) lines indicating the coordinate system of the node
     * {@link BoundHand#getHandNode_xPointing()}
     */
    public void debugHandNodeXPointingCoordinateSystem(){
        handNode_xPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.25f,0,0)));
        handNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0,0.15f,0)));
        handNode_xPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0,0f,0.1f)));
    }

    /**
     * Adds green (x), yellow (y) and red (x) lines indicating the coordinate system of the node
     * {@link BoundHand#getHandNode_xPointing()}
     */
    public void debugHandNodeZPointingCoordinateSystem(){
        handNode_zPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.15f,0,0)));
        handNode_zPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0,0.15f,0)));
        handNode_zPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0,0f,0.25f)));
    }

    /**
     * Adds green (x), yellow (y) and red (x) lines indicating the coordinate system of the node
     * {@link BoundHand#getPalmNode()}
     */
    public void debugPalmCoordinateSystem(){
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.25f,0,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0,0.15f,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0,0f,0.1f)));
    }

    /**
     * Returns a world position that a spatial's holdCentre should be at.
     * @param distanceFromSkin how far from the skin the hold position should be (provided so differing sized objects make sense)
     */
    public Vector3f getHoldPosition(float distanceFromSkin){
        return palmNode_xPointing.localToWorld(new Vector3f(0,0,(handSide==HandSide.LEFT?1:-1) * (baseSkinDepth+distanceFromSkin)), null );
    }

    /**
     * The rotation that should be applied to objects currently being held by this hand
     * @return
     */
    public Quaternion getHoldRotation(){
        return getPalmNode().getWorldRotation();
    }

    /**
     * When a grab action is specified (action in the openVr action manifest sense of the word) then periodically
     * (see {@link GrabPickingFunction#setGrabEvery}) if the action is true then a grab pick will occur and if the pick finds
     * any spatials with a control of type {@link AbstractGrabControl} then it will grab them. Equally, once bound if the
     * grab action is released then it will unbind from them.
     *
     * The action can be non hand specific as the hand restricts the action to only the hand this BoundHand represents
     *
     * The grab action can be either an analog or digital action
     *
     * @param grabAction the openVr action name to use to decide if the hand is grabbing
     * @param nodeToPickAgainst the node to scan for items to grab (probably the root node)
     */
    public void setGrabAction(String grabAction, Node nodeToPickAgainst){
        addFunction(new GrabPickingFunction(grabAction, nodeToPickAgainst));
    }

    public void clearGrabAction(){
        removeFunction(GrabPickingFunction.class);
    }

    /**
     * Will bind an action (see actions manifest) against a lemur click (picks against the
     * passed node). Call {@link BoundHand#clearClickAction_lemurSupport} to remove binding
     *
     * This requires lemur to be on the class path (or else you'll get an exception).
     *
     * It kind of "fakes" a click. So it's only limited in what it does. It tracks up the parents of things it hits looking
     * for things which are a button, or have a MouseEventControl. If it finds one it clicks that, then returns.
     *
     * Its worth noting that the MouseButtonEvents will not have meaningful x,y coordinates
     *
     * @param clickAction the action (see action manifest) that will trigger a click, can be a vector1 or a digital action.
     * @param nodeToPickAgainst The node that is picked against to look for lemur UIs
     */
    public void setClickAction_lemurSupport(String clickAction, Node nodeToPickAgainst){
        assertLemurAvailable();
        clearClickAction_lemurSupport();
        addFunction(new LemurClickFunction(clickAction, nodeToPickAgainst));
    }

    public void clearClickAction_lemurSupport(){
        removeFunction(LemurClickFunction.class);
    }

    /**
     * Set the depth between the centre of the palm and the skin of the palm used by the hand model you have bound.
     *
     * This ensures that held objects are flush against the palm
     * @param baseSkinDepth a value in meters (0.02 is a good example of the right kind of size)
     */
    public void setBaseSkinDepth(float baseSkinDepth){
        this.baseSkinDepth = baseSkinDepth;
    }

    /**
     * Broadly similar to attaching a geometry to {@link BoundHand#getHandNode_xPointing()} but it will get special
     * handling to ensure it doesn't block lemur picks (and will all get the {@link BoundHand#NO_PICK} label on all its
     * geometries which may make it easier to avoid when using manual picking. Also it can be easily removed with the
     * {@link BoundHand#removePickLine()} method
     * @param spatial the pick line (+X should be in the direction of the pick line)
     */
    public void attachPickLine( Spatial spatial ){
        searchForGeometry(spatial).forEach(g -> g.setUserData(NO_PICK, true));
        pickLineNode.attachChild(spatial);
    }

    public void removePickLine(){
        pickLineNode.detachAllChildren();
    }

    private static Collection<Geometry> searchForGeometry(Spatial spatial){
        if (spatial instanceof Geometry){
            return List.of((Geometry)spatial);
        }else if (spatial instanceof Node){
            List<Geometry> geometries = new ArrayList<>();
            for(Spatial child : ((Node)spatial).getChildren()){
                geometries.addAll(searchForGeometry(child));
            }

            return geometries;
        }else{
            throw new RuntimeException("Could not find skinable model");
        }
    }

    private Spatial armatureToNodes(Armature armature, ColorRGBA colorRGBA){
        return jointToNode(armature.getRoots()[0], colorRGBA);
    }

    private Spatial jointToNode(Joint joint, ColorRGBA colorRGBA){

        Node node = new Node();
        node.setLocalTranslation(joint.getLocalTranslation());
        node.setLocalRotation(joint.getLocalRotation());
        node.attachChild(microBox(colorRGBA));
        node.attachChild(microLine(colorRGBA));
        for(Joint child : joint.getChildren()){
            node.attachChild(jointToNode(child, colorRGBA));
        }
        return node;
    }

    private Geometry microBox(ColorRGBA colorRGBA){
        Box b = new Box(0.002f, 0.002f, 0.002f);
        Geometry geom = new Geometry("debugHandBox", b);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colorRGBA);
        geom.setMaterial(mat);
        geom.setUserData(NO_PICK, true);
        return geom;
    }

    private Geometry microLine(ColorRGBA colorRGBA){
        return microLine(colorRGBA, new Vector3f(0.015f, 0, 0));
    }

    private Geometry microLine(ColorRGBA colorRGBA, Vector3f vector){
        Line line = new Line(new Vector3f(0, 0, 0), vector);
        Geometry geometry = new Geometry("debugHandLine", line);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setLineWidth(5);
        material.setColor("Color", colorRGBA);
        geometry.setMaterial(material);
        geometry.setUserData(NO_PICK, true);
        return geometry;
    }

    /**
     * Given a picking result returns the first result that is not marked as being {@link BoundHand#NO_PICK}
     * @param collisionResults the collision result
     * @return the first hit
     */
    public static Optional<CollisionResult> firstNonSkippedHit( CollisionResults collisionResults ){
        for(CollisionResult hit: collisionResults){
            if (!Boolean.TRUE.equals(hit.getGeometry().getUserData(NO_PICK))){
                return Optional.of(hit);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the direction the hand is pointing (i.e. the direction the fingers would point if they aren't curled)
     * @return the bulkPointingDirection
     */
    public Vector3f getBulkPointingDirection(){
        return handNode_xPointing.getWorldRotation().mult(Vector3f.UNIT_X);
    }

    public static void assertLemurAvailable(){
        if (!isLemurAvailable()){
            throw new RuntimeException("Lemur not available on class path. Lemur required for methods named _lemurSupport or classes with Lemur in name");
        }
    }

    public static boolean isLemurAvailable(){
        if (lemurCheckedAvailable){
            return true;
        }else{
            try {
                Class.forName("com.simsilica.lemur.Button");
                lemurCheckedAvailable = true;
            } catch (Throwable ex) {
                lemurCheckedAvailable = false;
            }
            return lemurCheckedAvailable;
        }
    }
}

package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
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
import com.jme3.scene.shape.Sphere;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.BoneStance;
import com.onemillionworlds.tamarin.compatibility.HandMode;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class BoundHand{

    public static String NO_PICK = "noPick";

    private HandMode handMode = HandMode.WITHOUT_CONTROLLER;

    private final Node rawOpenVrPosition = new Node();

    private final Node geometryNode = new Node();

    private final Node handNode_xPointing = new Node();

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

    private final AssetManager assetManager;

    private final HandSide handSide;

    private final ActionBasedOpenVrState vrState;

    /**
     * Debug points add markers onto the hands where the bone positions are, and their directions
     */
    private boolean debugPoints = false;

    private Optional<String> grabAction = Optional.empty();
    Node nodeToGrabPickAgainst;

    private float grabEvery = 1f/10;

    private float timeSinceGrabbed = 100;

    private float minimumGripToTrigger = 0.5f;

    private float maxGrabDistance = 0.1f;

    private float baseSkinDepth = 0.02f;

    Optional<AbstractGrabControl> currentlyGrabbed = Optional.empty();

    Optional<Node> pickMarkerAgainstContinuous = Optional.empty();

    Spatial pickMarker;

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

        rawOpenVrPosition.attachChild(palmNode_xPointing);

        rawOpenVrPosition.attachChild(geometryNode);

        handNode_xPointing.attachChild(pickLineNode);

        pickMarker= defaultPickMarker();
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
     * pointing to the right
     *
     * This is an ideal node for things like picking lines, which can be put in the x direction
     *
     * Note that the (0,0,0) position is just in front of the thumb, not the centre of the hand.
     *
     * This node is primarily used for picking, but if you want a node to attach to that only cares about the bulk
     * hand position
     *
     * @return a node to connect things to
     */
    public Node getHandNode_xPointing(){
        return handNode_xPointing;
    }

    /**
     * Returns a node that will update with the hands position and rotation.
     *
     * This node has an orientation such that x aligns with the hands pointing direction, Y pointing upwards and Z
     * pointing to the right, as defined by the middle finger metacarpal bone. Its X,Y and Z are likely to be in similar
     * directions to those of {@link BoundHand#getHandNode_xPointing()} but not precisely
     * @return
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

    public void setMaterial(Material material){
        searchForGeometry(geometryNode).forEach(g -> g.setMaterial(material));
    }

    protected void update(float timeSlice, Map<String, BoneStance> boneStances){
        if (debugPoints){
            debugPointsNode.detachAllChildren();
            debugPointsNode.attachChild(armatureToNodes(getArmature(), ColorRGBA.Red));
        }

        updatePalm(timeSlice, boneStances);
        updateForGrab(timeSlice);
        updateForPickMarker(timeSlice);

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
     * This will set the hand to do a pick in the same direction as {@link BoundHand#pickBulkHand}/{@link BoundHand#click_lemurSupport}
     * and place a marker (by default a white sphere) at the point where the pick hits a geometry. This gives the
     * player an indication what they would pick it they clicked now; think of it like a mouse pointer in 3d space
     * @param nodeToPickAgainst
     */
    public void setPickMarkerContinuous(Node nodeToPickAgainst){
        pickMarkerAgainstContinuous = Optional.of(nodeToPickAgainst);
        getHandNode_xPointing().attachChild(pickMarker);


    }

    public void clearPickMarkerContinuous(){
        pickMarkerAgainstContinuous = Optional.empty();
        pickMarker.removeFromParent();
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
        Vector3f pickOrigin = getPalmNode().getWorldTranslation();
        Vector3f pickingPoint = getPalmNode().localToWorld(new Vector3f(0,0,handSide == HandSide.LEFT?1:-1), null);
        Vector3f pickingVector = pickingPoint.subtract(pickOrigin);
        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(pickOrigin, pickingVector);

        nodeToPickAgainst.collideWith(ray, results);
        return results;
    }

    /**
     * Left or right hand
     * @return
     */
    public HandSide getHandSide(){
        return handSide;
    }

    /**
     * Picks using {@link BoundHand#pickBulkHand} then simulates a pick on the first thing it hits.
     *
     * Note; it ignores anything connected to getHandNode(), so you can "pick through" any picking line markers or
     * anything that has user data "noPick" with value true
     *
     * This requires lemur to be on the class path (or else you'll get an exception).
     *
     * It kind of "fakes" a click. So it's only limited in what it does. It tracks up the parents of things it hits looking
     * for things which are a button, or have a MouseEventControl. If it finds one it clicks that, then returns.
     *
     * Its worth noting that the MouseButtonEvents will not have meaningful x,y coordinates
     *
     * @param nodeToPickAgainst the node that contains things that can be clicked
     */
    public void click_lemurSupport(Node nodeToPickAgainst){
        CollisionResults results = pickBulkHand(nodeToPickAgainst);

        for(int i=0;i<results.size();i++){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();

                while(processedSpatial!=null){
                    if (processedSpatial instanceof Button){
                        ((Button)processedSpatial).click();
                        return;
                    }
                    MouseEventControl mec = processedSpatial.getControl(MouseEventControl.class);
                    if ( mec!=null ){
                        mec.mouseButtonEvent(new MouseButtonEvent(0, true, 0, 0), processedSpatial, processedSpatial);
                        return;
                    }

                    processedSpatial = processedSpatial.getParent();
                }
                return;
            }

        }
    }

    public void debugArmature(){
        this.debugPoints = true;
    }

    public void debugPickLines(){
        handNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0.25f,0,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0,0,handSide == HandSide.LEFT?0.25f:-0.25f)));
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
     * (see {@link BoundHand#setGrabEvery}) if the action is true then a grab pick will occure and if the pick finds
     * any spatials with a control of type {@link AbstractGrabControl} then it will grab them. Equally once bound if the
     * grab action is released then it will unbind from them.
     *
     * The action can be non hand specific as the hand restricts the action to only the hand this BoundHand represents
     *
     * The grab action should be an analog action
     *
     * @param grabAction the openVr action name to use to decide if the hand is grabbing
     * @param nodeToPickAgainst the node to scan for items to grab (probably the root node)
     */
    public void setGrabAction(String grabAction, Node nodeToPickAgainst){
        this.grabAction = Optional.of(grabAction);
        this.nodeToGrabPickAgainst = nodeToPickAgainst;
    }

    public void clearGrabAction(){
        this.grabAction = Optional.empty();
        this.nodeToGrabPickAgainst = null;
    }

    public void setGrabEvery(float grabEvery){
        this.grabEvery = grabEvery;
    }

    /**
     * Allows the amount of pressure required to pick something up to be changed
     * @param minimumGripToTrigger a value between 0 and 1
     */
    public void setMinimumGripToTrigger(float minimumGripToTrigger){
        this.minimumGripToTrigger = minimumGripToTrigger;
    }

    /**
     * This maximum distance that a grab pick will pick up an object.
     *
     * Note that this is the distance from the centre of the hand to the first face the pick line sees
     * @param maxGrabDistance a value in meters
     */
    public void setMaxGrabDistance(float maxGrabDistance){
        this.maxGrabDistance = maxGrabDistance;
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

    private void updateForPickMarker(float timslice){
        pickMarkerAgainstContinuous.ifPresent(node -> {
            firstNonSkippedHit(pickBulkHand(node)).ifPresentOrElse(
                    hit -> {
                        pickMarker.setCullHint(Spatial.CullHint.Inherit);
                        pickMarker.setLocalTranslation(hit.getDistance(), 0, 0);
                    },
                    () -> pickMarker.setCullHint(Spatial.CullHint.Always)

            );
        });
    }

    private void updateForGrab(float timeSlice){
        grabAction.ifPresent(action -> {
            timeSinceGrabbed+=timeSlice;
            if (timeSinceGrabbed>grabEvery){
                timeSinceGrabbed = 0;
                AnalogActionState grabActionState = vrState.getAnalogActionState(action, handSide.restrictToInputString);

                if (grabActionState.x>minimumGripToTrigger && currentlyGrabbed.isEmpty()){
                    //looking for things in the world to grab
                    CollisionResults results = pickPalm(nodeToGrabPickAgainst);
                    Spatial picked = null;
                    for(CollisionResult hit: results){
                        if (!Boolean.TRUE.equals(hit.getGeometry().getUserData(NO_PICK))){
                            if (hit.getDistance()<maxGrabDistance){
                                picked = hit.getGeometry();
                            }
                            break;
                        }
                    }
                    AbstractGrabControl grabControl = null;

                    while(picked !=null && grabControl == null){
                        grabControl = picked.getControl(AbstractGrabControl.class);
                        picked = picked.getParent();
                    }

                    if (grabControl!=null){
                        currentlyGrabbed = Optional.of(grabControl);
                        grabControl.onGrab(this);
                    }

                }else if (grabActionState.x<minimumGripToTrigger && currentlyGrabbed.isPresent()){
                    //drop current item
                    currentlyGrabbed.get().onRelease();
                    currentlyGrabbed = Optional.empty();
                }

            }
        });
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

    private Geometry defaultPickMarker(){
        Sphere sphere = new Sphere(15, 15, 0.01f);
        Geometry geometry = new Geometry("pickingMarker", sphere);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.White);
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
}

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
import com.onemillionworlds.tamarin.compatibility.BoneStance;
import com.onemillionworlds.tamarin.compatibility.HandMode;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class BoundHand{

    private HandMode handMode = HandMode.WITHOUT_CONTROLLER;

    private final Node rawOpenVrPosition = new Node();

    private final Node geometryNode = new Node();

    private final Node handNode_xPointing = new Node();

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

    /**
     * Debug points add markers onto the hands where the bone positions are, and their directions
     */
    private boolean debugPoints = false;

    public BoundHand(String postActionName, String skeletonActionName, Spatial handGeometry, Armature armature, AssetManager assetManager, HandSide handSide){
        this.geometryNode.attachChild(handGeometry);
        this.postActionName = postActionName;
        this.skeletonActionName = skeletonActionName;
        this.armature = armature;
        this.assetManager = assetManager;
        this.handSide = handSide;
        this.rawOpenVrPosition.attachChild(handNode_xPointing);
        this.rawOpenVrPosition.attachChild(debugPointsNode);

        Quaternion naturalRotation = new Quaternion();
        naturalRotation.fromAngleAxis(-0.75f* FastMath.PI, Vector3f.UNIT_X);
        Quaternion zToXRotation = new Quaternion();
        zToXRotation.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_Z);
        Quaternion rotateAxes = new Quaternion();
        rotateAxes.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_X);

        handNode_xPointing.setLocalRotation(naturalRotation.mult(zToXRotation).mult(rotateAxes));

        rawOpenVrPosition.attachChild(palmNode_xPointing);

        rawOpenVrPosition.attachChild(geometryNode);
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

        //the palm node is put at the position between the finger_middle_0_l bone and finger_middle_meta_l, but with the
        // rotation of the finger_middle_meta_l bone. This gives roughly the position of a grab point, with a sensible rotation
        String proximalName = handSide == HandSide.LEFT ? "finger_middle_0_l" : "finger_middle_0_r";
        String metacarpelName = handSide == HandSide.LEFT ? "finger_middle_meta_l" : "finger_middle_meta_r";

        BoneStance metacarpel = boneStances.get(metacarpelName);
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
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    public CollisionResults pickBulkHand(Node nodeToPickAgainst){
        Vector3f pickOrigin = getHandNode_xPointing().getWorldTranslation();
        Vector3f pickingPoint = getHandNode_xPointing().localToWorld(new Vector3f(0,0,-1), null);
        Vector3f pickingVector = pickingPoint.subtract(pickOrigin);
        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(pickOrigin, pickingVector);

        nodeToPickAgainst.collideWith(ray, results);
        return results;
    }

    /**
     * Picks using {@link BoundHand#pickBulkHand} then simulates a pick on the first thing it hits.
     *
     * Note; it ignores anything connected to getHandNode(), so you can "pick through" any picking line markers or
     * anything that has user data "noPick" with value true
     *
     * This requires lemur to be on the class path (or else you'll get an exception).
     *
     * It kind of "fakes" a click. So its only limited in what it does. It tracks up the parents of things it hits looking
     * for things which are a button, or have a MouseEventControl. If it finds one it clicks that, then returns.
     *
     * Its worth noting that the MouseButtonEvents will not have meaningful x,y coordinates
     *
     * @param nodeToPickAgainst
     * @return
     */
    public void click_lemurSupport(Node nodeToPickAgainst){
        CollisionResults results = pickBulkHand(nodeToPickAgainst);

        for(int i=0;i<results.size();i++){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData("noPick"));
            skip |= parentStream(collision.getGeometry().getParent()).anyMatch(s -> s == rawOpenVrPosition);

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
        rawOpenVrPosition.attachChild(microLine(ColorRGBA.Green, new Vector3f(0,0,-0.25f)));
        handNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0.25f,0,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0.25f,0,0)));
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
     * {@link BoundHand#getPalmNode_xPointing()}
     */
    public void debugPalmCoordinateSystem(){
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.25f,0,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0,0.15f,0)));
        palmNode_xPointing.attachChild(microLine(ColorRGBA.Red, new Vector3f(0,0f,0.1f)));
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

    /**
     * Returns a linear stream that is the list of parents up from the specified node
     * @param basePoint
     * @return
     */
    private static Stream<Spatial> parentStream(Spatial basePoint){
        return Stream.iterate(basePoint, Objects::nonNull, Spatial::getParent);

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
        return geometry;
    }


}

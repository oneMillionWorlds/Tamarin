package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.compatibility.HandMode;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.event.MouseEventControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class BoundHand{

    private HandMode handMode = HandMode.WITHOUT_CONTROLLER;

    private final Node handGeometryNode = new Node();

    private final Node handNode_zPointing = new Node();

    /**
     * The hand will be detached from the scene graph and will no longer receive updates
     */
    public abstract void unbindHand();

    private final String postActionName;

    private final String skeletonActionName;

    private final Armature armature;

    public BoundHand(String postActionName, String skeletonActionName, Spatial handGeometry, Armature armature){
        this.handGeometryNode.attachChild(handGeometry);
        this.postActionName = postActionName;
        this.skeletonActionName = skeletonActionName;
        this.armature = armature;

        this.handGeometryNode.attachChild(handNode_zPointing);

        Quaternion naturalRotation = new Quaternion();
        naturalRotation.fromAngleAxis(-0.25f* FastMath.PI, Vector3f.UNIT_X);
        handNode_zPointing.setLocalRotation(naturalRotation);
    }

    public HandMode getHandMode(){
        return handMode;
    }

    /**
     * Returns a node that will update with the hands position and rotation.
     *
     * Note that this is in the orientation that OpenVR provides which isn't very helpful
     * (it doesn't map well to the direction the hand is pointing for example)
     *
     * You probably don't want this and probably want {@link BoundHand#getHandNode_zPointing}
     * which has a more natural rotation
     *
     * @return the raw hand node
     */
    public Node getHandNode(){
        return handGeometryNode;
    }

    /**
     * Returns a node that will update with the hands position and rotation.
     *
     * This node has an orientation such that negative Z aligned with the hands pointing direction and +X aligned with the
     * direction along the hand (which direction along the hand +X or -X depends on if its the left or right hand).
     *
     * This is an ideal node for things like picking lines, which can be put in the negative z direction
     *
     * Note that the position is just in front of the thumb, not the centre of the hand
     *
     * @return a node to connect things to
     */
    public Node getHandNode_zPointing(){
        return handNode_zPointing;
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
        searchForGeometry(getHandNode()).forEach(g -> g.setMaterial(material));
    }

    /**
     * Picks from a point just in front of the thumb (the point the getHandNode_zPointing() is at) in the direction
     * out away from the hand.
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    public CollisionResults pickBulkHand(Node nodeToPickAgainst){
        Vector3f pickOrigin = getHandNode_zPointing().getWorldTranslation();
        Vector3f pickingPoint = getHandNode_zPointing().localToWorld(new Vector3f(0,0,-1), null);
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
            skip |= parentStream(collision.getGeometry().getParent()).anyMatch(s -> s == handGeometryNode);

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
    private  static Stream<Spatial> parentStream(Spatial basePoint){
        return Stream.iterate(basePoint, Objects::nonNull, Spatial::getParent);

    }


}

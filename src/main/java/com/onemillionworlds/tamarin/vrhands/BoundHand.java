package com.onemillionworlds.tamarin.vrhands;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingSphere;
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
import com.jme3.scene.shape.Sphere;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.math.RotationalVelocity;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import com.onemillionworlds.tamarin.vrhands.functions.ClimbSupport;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;
import com.onemillionworlds.tamarin.vrhands.functions.LemurClickFunction;
import com.onemillionworlds.tamarin.vrhands.functions.PickMarkerFunction;
import com.onemillionworlds.tamarin.vrhands.functions.PressFunction;
import com.onemillionworlds.tamarin.vrhands.grabbing.AbstractGrabControl;
import com.onemillionworlds.tamarin.vrhands.touching.AbstractTouchControl;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public abstract class BoundHand{

    private static final float FINGER_PICK_SPHERE_RADIUS = 0.0075f;

    private static boolean lemurCheckedAvailable = false ;

    /**
     * Placed on GEOMETRIES that should not be picked by the hand (either grab or point pick)
     */
    public static String NO_PICK = "TAMARIN_NO_PICK";

    /**
     * Placed on nodes where if a pick is done on a child it will stop looking higher in the parent chain for
     * grab controls or similar.
     */
    public static final String TAMARIN_STOP_BUBBLING = "TAMARIN_STOP_BUBBLING";

    private final Node rawOpenXrPosition = new Node();

    private final Node geometryNode = new Node();

    /**
     * Returns a node that will update with the hands position and rotation.
     * <p>
     * The exact position and rotation of this node depends on the pose type specified for the hand:
     *  <p>
     * If the pose is AIM them it will be just in front and above the hand with +X pointing in the direction a held weapon
     * would fire with Y pointing upwards and Z pointing to the right (when the hands are held with the palms
     * facing each other)
     *  <p>
     * If the pose is GRIP them it will be away from the palm, with +X pointing in the direction a held weapon
     * would fire with Y pointing upwards and Z pointing to the right (when the hands are held with the palms
     * facing each other)
     * <p>
     * This node has an orientation such that x aligns with the grip direction (like you are holding a sword, and Z
     * pointing to the right (when the hands are held with the palms facing each other).
     * <p>
     * This is an ideal node for things like picking lines, which can be put in the x direction
     * <p>
     * Note that the (0,0,0) position is just in front of the thumb, not the centre of the hand.
     * <p>
     * This node is primarily used for picking, but if you want a node to attach to that only cares about the bulk
     * hand position
     *
     */
    @Getter
    private final Node handNode_xPointing = new Node();

    /**
     * A hand node, with +z pointing in the direction of the bulk hand (if an AIM pose). This is used primarily for direct lemur interactions
     */
    @Getter
    private final Node handNode_zPointing = new Node();

    private final Node pickLineNode = new Node();

    /**
     * This is a node that sits on the palm (but near the fingers) such that with the hands held so the palms face each other
     * +X faces forwards (along the line of the fingers) the +Y points upwards and +Z points to the right.
     * It is live updated based on the skeleton positions so its exact relations to other nodes may change as
     * the node moves.
     */
    private final Node palmNode_xPointing = new Node();

    /**
     * This is a node that sits near on the tip of the index finger whose +x points out way from the index
     * finger (and Y point up and Z points right if the hands are held with palms facing each other. This node sits inside
     * the finger such that a sphere would touch the skin of the finger in 5 directions.
     * The exact distance from the skin can vary (it is reported by OpenXr) but for bound hand's perspective it is considered
     * to be {@link BoundHand#FINGER_PICK_SPHERE_RADIUS} away from the skin.
     */
    @Getter
    private final Node indexFingerTip_xPointing = new Node();

    private final Node debugPointsNode = new Node();

    /**
     * A node at the wrist.
     * <p>
     * <b>With hands held with thumbs upwards</b> +x going to the left, +y goes upwards and +z goes towards the fingers
     */
    @Getter
    private final Node wristNode = new Node();

    /**
     * The hand will be detached from the scene graph and will no longer receive updates
     */
    public abstract void unbindHand();

    @Getter
    private final ActionHandle handPoseActionName;

    @Getter
    private final ActionHandle skeletonActionName;

    @Getter
    private Armature armature;

    @Getter
    private final AssetManager assetManager;

    /**
     * Left or right hand
     */
    @Getter
    private final HandSide handSide;

    private final OpenXrActionState vrState;

    /**
     * Debug points add markers onto the hands where the bone positions are, and their directions
     */
    private boolean debugPoints = false;

    private float baseSkinDepth = 0.02f;

    /**
     * The velocity (in world coordinates) that the hand is currently moving at
     */
    @Getter
    private Vector3f velocity_world = new Vector3f();

    /**
     * The rotational velocity (in world coordinates) that the hand is currently rotating at
     */
    @Getter
    private RotationalVelocity rotationalVelocity_world = new RotationalVelocity(new Vector3f());

    /**
     * When doing a palm pick spheres of this radius are created above the palm
     */
    @Setter
    private float palmPickSphereRadius = 0.02f;

    /**
     * When doing a palm pick these are the points where spheres are formed to detect if the palm is against anything
     */
    @Setter
    private List<Vector3f> palmPickPoints;

    private final List<BoundHandFunction> functions = new CopyOnWriteArrayList<>();

    /**
     * A pointing arrangement is when the index finger is mostly straight and the ring ringer is not.
     * <p>
     * This is the sort of hand position that indicates pressing buttons with the index finger
     */
    @Getter
    public boolean handPointing = false;

    private final HandJoint middleProximalName = HandJoint.MIDDLE_PROXIMAL_EXT;
    private final HandJoint middleMetacarpalName = HandJoint.MIDDLE_METACARPAL_EXT;

    private final HandJoint indexMetacarpalName = HandJoint.INDEX_METACARPAL_EXT;
    private final HandJoint indexEndName = HandJoint.INDEX_TIP_EXT;
    private final HandJoint index2Name = HandJoint.INDEX_INTERMEDIATE_EXT;
    private final HandJoint index1Name = HandJoint.INDEX_PROXIMAL_EXT;

    private final HandJoint ringMetacarpalName = HandJoint.RING_METACARPAL_EXT;

    private final HandJoint ringEndName = HandJoint.RING_TIP_EXT;
    private final HandJoint ring2Name = HandJoint.RING_INTERMEDIATE_EXT;
    private final HandJoint ring1Name = HandJoint.RING_PROXIMAL_EXT;

    private final HandJoint wristName = HandJoint.WRIST_EXT;

    public BoundHand(OpenXrActionState vrState, ActionHandle handPoseActionName, ActionHandle skeletonActionName, Spatial handGeometry, Armature armature, AssetManager assetManager, HandSide handSide){
        this.vrState = Objects.requireNonNull(vrState);
        this.geometryNode.attachChild(handGeometry);
        this.handPoseActionName = handPoseActionName;
        this.skeletonActionName = skeletonActionName;
        this.armature = armature;
        this.assetManager = assetManager;
        this.handSide = handSide;
        this.rawOpenXrPosition.attachChild(handNode_xPointing);
        this.rawOpenXrPosition.attachChild(debugPointsNode);

        float outOfPalm = (handSide == HandSide.LEFT ? 1 : -1);
        this.palmPickPoints = List.of(new Vector3f(0,0,outOfPalm*(0.01f+palmPickSphereRadius)), new Vector3f(0.02f,-0.03f,outOfPalm*(0.01f+palmPickSphereRadius)), new Vector3f(0.03f,0.03f,outOfPalm*(0.005f+palmPickSphereRadius)), new Vector3f(-0.03f,0,outOfPalm*(0.01f+palmPickSphereRadius)));

        searchForGeometry(handGeometry).forEach(g -> g.setUserData(NO_PICK, true));

        Quaternion naturalRotation = new Quaternion();
        naturalRotation.fromAngleAxis(-0.5f* FastMath.PI, Vector3f.UNIT_X);
        Quaternion zToXRotation = new Quaternion();
        zToXRotation.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_Z);
        Quaternion rotateAxes = new Quaternion();
        rotateAxes.fromAngleAxis(0.5f* FastMath.PI, Vector3f.UNIT_X);

        handNode_xPointing.setLocalRotation(naturalRotation.mult(zToXRotation).mult(rotateAxes));

        Quaternion xPointingToZPointing = new Quaternion();
        xPointingToZPointing.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        handNode_zPointing.setLocalRotation(xPointingToZPointing);
        handNode_xPointing.attachChild(handNode_zPointing);

        rawOpenXrPosition.attachChild(palmNode_xPointing);
        rawOpenXrPosition.attachChild(geometryNode);
        rawOpenXrPosition.attachChild(indexFingerTip_xPointing);
        rawOpenXrPosition.attachChild(wristNode);
        handNode_xPointing.attachChild(pickLineNode);

        addFunction(new ClimbSupport());
    }

    /**
     *
     * @param handGeometry A spatial that will be updated with the hand geometry, it must have a SkinningControl
     *                     at its top level
     */
    public void updateHandGeometryAndControlledArmature(Spatial handGeometry){
        geometryNode.detachAllChildren();
        geometryNode.attachChild(handGeometry);
        searchForGeometry(handGeometry).forEach(g -> g.setUserData(NO_PICK, true));

        SkinningControl skinningControl = handGeometry.getControl(SkinningControl.class);

        if (skinningControl == null){
            throw new IllegalArgumentException("Hand geometry must have a SkinningControl");
        }
        this.armature = skinningControl.getArmature();
    }

    /**
     * Returns a node that will update with the hands position and rotation. If you are dealing with raw bone positions
     * they are in this coordinate system.
     * <p>
     * Note that this is in the orientation that OpenVR provides which isn't very helpful
     * (it doesn't map well to the direction the hand is pointing for example)
     * <p>
     * You probably don't want this and probably want {@link BoundHand#getHandNode_xPointing}
     * which has a more natural rotation. Unless you are dealing with raw bone positions, which are in this coordinate
     * system.
     *
     * @return the raw hand node
     */
    public Node getRawOpenVrNode(){
        return rawOpenXrPosition;
    }


    /**
     * Returns a node that will update with the hands position and rotation.
     * <p>
     * This node has an orientation such that x aligns with the hands pointing direction, Y pointing upwards and Z
     * pointing to the right, as defined by the middle finger metacarpal bone. Its X,Y and Z are likely to be in similar
     * directions to those of {@link BoundHand#getHandNode_xPointing()} but not precisely
     * @return the palm node
     */
    public Node getPalmNode(){
        return palmNode_xPointing;
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
    public FunctionRegistration addFunction(BoundHandFunction function){
        function.onBind(this, vrState.getStateManager());
        functions.add(function);
        return () ->
            removeFunction(function);

    }

    /**
     * Note that this function can behave oddly when multiple copies of the same BoundHandFunction type are
     * registered. It is better to use the Runnable returned by addFunction to remove functions
     */
    public void removeFunction(Class<? extends BoundHandFunction> functionToRemove){
        BoundHandFunction function = functions.stream().filter(f -> f.getClass().equals(functionToRemove)).findFirst().orElse(null);
        removeFunction(function);
    }

    public void removeFunction(BoundHandFunction functionToRemove){
        if (functionToRemove!=null){
            functionToRemove.onUnbind(this, vrState.getStateManager());
            functions.remove(functionToRemove);
        }
    }

    public <T extends BoundHandFunction> Optional<T> getFunctionOpt(Class<T> function){
        //noinspection unchecked
        return functions.stream().filter(f -> f.getClass().equals(function)).map(f -> (T)f).findFirst();
    }

    public <T extends BoundHandFunction> T getFunction(Class<T> function){
        return getFunctionOpt(function).orElseThrow();
    }

    protected void update(float timeSlice, Map<HandJoint, BonePose> boneStances){
        if (debugPoints){
            debugPointsNode.detachAllChildren();
            debugPointsNode.attachChild(armatureToNodes(getArmature(), ColorRGBA.Red));
        }
        updatePalm(timeSlice, boneStances);
        updateFingerTips(boneStances);
        updateWrist(boneStances);
        functions.forEach(f -> f.update(timeSlice, this, vrState.getStateManager()));

        updatePointingState(boneStances);
    }

    /**
     * Updates the hand to check if it's in a pointing arrangement;
     * fist with index finger outstretched, like when pressing a button
     */
    private void updatePointingState(Map<HandJoint, BonePose> boneStances){
        BonePose indexEnd = boneStances.get(indexEndName);
        BonePose index2= boneStances.get(index2Name);
        BonePose index1 = boneStances.get(index1Name);
        BonePose indexMeta = boneStances.get(indexMetacarpalName);

        BonePose ringEnd = boneStances.get(ringEndName);
        BonePose ring2= boneStances.get(ring2Name);
        BonePose ring1 = boneStances.get(ring1Name);
        BonePose ringMeta = boneStances.get(ringMetacarpalName);

        if (notNull(indexEnd, index2, index1, ringEnd, ring2, ring1, indexMeta, ringMeta)){
            float ringFingerAlignment = ringEnd.position().subtract(ring2.position()).normalizeLocal().dot(ring1.position().subtract(ringMeta.position()).normalizeLocal());
            float indexFingerAlignment = indexEnd.position().subtract(index2.position()).normalizeLocal().dot(index1.position().subtract(indexMeta.position()).normalizeLocal());
            handPointing = indexFingerAlignment > 0.85 && ringFingerAlignment < 0.8;

        }
    }

    private void updateWrist( Map<HandJoint, BonePose> boneStances){
        BonePose wrist = boneStances.get(wristName);
        if (wrist!=null){
            wristNode.setLocalTranslation(wrist.position());
            wristNode.setLocalRotation(wrist.orientation());
        }
    }

    private void updatePalm(float timeSlice, Map<HandJoint, BonePose> boneStances){
        //the palm node is put at the position between the finger_middle_0_l bone and finger_middle_meta_l, but with the
        // rotation of the finger_middle_meta_l bone. This gives roughly the position of a grab point, with a sensible rotation

        BonePose metacarpel = boneStances.get(middleMetacarpalName);
        BonePose proximal = boneStances.get(middleProximalName);
        if (metacarpel != null){

            Quaternion coordinateStandardisingRotation = new Quaternion();
            if (handSide == HandSide.LEFT){
                coordinateStandardisingRotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
                coordinateStandardisingRotation.multLocal(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
            }else{
                coordinateStandardisingRotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
                coordinateStandardisingRotation.multLocal(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
            }


            palmNode_xPointing.setLocalTranslation(proximal.position().add(metacarpel.position()).multLocal(0.5f));
            palmNode_xPointing.setLocalRotation(metacarpel.orientation().mult(coordinateStandardisingRotation));
        }
    }

    private void updateFingerTips(Map<HandJoint, BonePose> boneStances){
        BonePose indexFingerTip = boneStances.get(indexEndName);
        if (indexFingerTip!=null){
            indexFingerTip_xPointing.setLocalTranslation(indexFingerTip.position());

            Quaternion rotation = indexFingerTip.orientation();
            Quaternion coordinateStandardisingRotation = new Quaternion();
            if (handSide == HandSide.LEFT){
                coordinateStandardisingRotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
                coordinateStandardisingRotation.multLocal(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
            }else{
                coordinateStandardisingRotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
                coordinateStandardisingRotation.multLocal(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
            }

            indexFingerTip_xPointing.setLocalRotation(rotation.mult(coordinateStandardisingRotation));
        }
    }

    /**
     * Picks using a small sphere at the index finger tip (to catch if the index finger has been plunged into something)
     * @param nodeToPickAgainst the node that contains geometries to be picked from
     * @return the results
     */
    public CollisionResults pickIndexFingerTip(Node nodeToPickAgainst){
        CollisionResults results = new CollisionResults();
        BoundingSphere sphere = new BoundingSphere(FINGER_PICK_SPHERE_RADIUS, indexFingerTip_xPointing.getWorldTranslation());
        nodeToPickAgainst.collideWith(sphere, results);
        return results;
    }

    /**
     * Picks from a point just in front of the thumb (the point the getHandNode_zPointing() is at) in the direction
     * out away from the hand.
     * <p>
     * Note that the geometry of the hand itself may be the first result from the pick but more reasonable pick results
     * will follow. (These will have NO_PICK = true as userdata which can be used to ignore them)
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    public CollisionResults pickBulkHand(Node nodeToPickAgainst){
        Vector3f pickOrigin = getHandNode_xPointing().getWorldTranslation();
        Vector3f pickingPoint = getHandNode_xPointing().localToWorld(new Vector3f(1,0,0), null);
        Vector3f pickingVector = pickingPoint.subtract(pickOrigin);
        pickingVector.normalizeLocal();
        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(pickOrigin, pickingVector);

        nodeToPickAgainst.collideWith(ray, results);
        return results;
    }

    /**
     * This will set the hand to do a pick in the same direction as {@link BoundHand#pickBulkHand}/lemur clicks
     * and place a marker (by default a white sphere) at the point where the pick hits a geometry. This gives the
     * player an indication what they would pick it they clicked now; think of it like a mouse pointer in 3d space
     * <p>
     * Run the returned runnable to remove the pick marker
     */
    public FunctionRegistration setPickMarkerContinuous(Node nodeToPickAgainst){
        return addFunction(new PickMarkerFunction(nodeToPickAgainst));
    }

    /**
     * Picks from roughly the centre of the palm out from the palm (i.e. for the left hand it points right)
     * <p>
     * This can be used to detect what the palm is pointing at (which is rarely useful to be honest)
     * <p>
     * Note that the geometry of the hand itself may be the first result from the pick but more reasonable pick results
     * will follow. (These will have NO_PICK = true as userdata which can be used to ignore them)
     * <p>
     * deprecated as intend to move towards the pickPalmSpheres method
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     */
    @Deprecated
    public CollisionResults pickPalm(Node nodeToPickAgainst){
        return pickPalm(nodeToPickAgainst, Vector3f.ZERO);
    }

    /**
     * Picks (using a series of bounding shapes) just beyond the palm. Unlike pickPalm it does not pick out to an
     * infinite range but uses a series of spheres
     * <p>
     * This can be useful to use picking to determine what the player wishes to grab
     */
    public CollisionResults pickGrab(Node nodeToPickAgainst){
        Vector3f worldPickLocation = new Vector3f();

        CollisionResults overallResults = new CollisionResults();
        for(Vector3f pickPoint : palmPickPoints){
            worldPickLocation = getPalmNode().localToWorld(pickPoint, worldPickLocation);
            CollisionResults results = new CollisionResults();
            BoundingSphere sphere = new BoundingSphere(palmPickSphereRadius, worldPickLocation);
            nodeToPickAgainst.collideWith(sphere, results);
            for(int i=0;i<results.size();i++){
                CollisionResult result = results.getCollision(i);
                if(!Boolean.TRUE.equals(result.getGeometry().getUserData(NO_PICK))){
                    overallResults.addCollision(result);
                }
            }
        }
        return overallResults;
    }

    /**
     * Picks outward away from the palm palmRelativePosition to {@link BoundHand#getPalmNode()}.
     * Note that x is towards the fingers, y is up and z is right (whether +z is the palm direction depends on if this
     * is the left or right hand)
     * <p>
     * deprecated as intend to move towards the pickPalmSpheres method
     *
     * @param nodeToPickAgainst node that is the parent of all things that can be picked. Probably the root node
     * @param palmRelativePosition the position for the pick line to start
     */
    @Deprecated
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
        handNode_xPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.25f,0,0)));
        indexFingerTip_xPointing.attachChild(microLine(ColorRGBA.Green, new Vector3f(0.25f,0,0) ));
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
     * spheres showing the current grab points for the palm.
     * <p>
     * The first (index zero) points are bright, the last points (high index) are dark
     * {@link BoundHand#getPalmNode()}
     */
    public FunctionRegistration debugPalmGrabPoints(){
        List<Geometry> addedPoints = new ArrayList<>();

        int index = 0;
        for(Vector3f grabPoint: this.palmPickPoints){
            float brightness = ((float)this.palmPickPoints.size()-index)/this.palmPickPoints.size();
            index++;
            Geometry sphere = sphere(new ColorRGBA(brightness,brightness,brightness,1), grabPoint, palmPickSphereRadius);
            addedPoints.add(sphere);
            palmNode_xPointing.attachChild(sphere);
        }
        return () -> addedPoints.forEach(Spatial::removeFromParent);
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
     *
     */
    public Quaternion getHoldRotation(){
        return getPalmNode().getWorldRotation();
    }

    /**
     * When a grab action is specified (action in the openVr action manifest sense of the word) then periodically
     * (see {@link GrabPickingFunction#setGrabEvery}) if the action is true then a grab pick will occur and if the pick finds
     * any spatials with a control of type {@link AbstractGrabControl} then it will grab them. Equally, once bound if the
     * grab action is released then it will unbind from them.
     * <p>
     * The action can be non hand specific as the hand restricts the action to only the hand this BoundHand represents
     * <p>
     * The grab action can be either an analog or digital action
     * <p>
     * Use the {@link FunctionRegistration} to end the function when done
     *
     * @param grabAction the openVr action name to use to decide if the hand is grabbing
     * @param nodeToPickAgainst the node to scan for items to grab (probably the root node)
     */
    public FunctionRegistration setGrabAction(ActionHandle grabAction, Node nodeToPickAgainst){
        GrabPickingFunction grabPickingFunction = new GrabPickingFunction(grabAction, nodeToPickAgainst);
        return addFunction(grabPickingFunction);
    }

    public void clearGrabAction(){
        removeFunction(GrabPickingFunction.class);
    }

    /**
     * Will bind an action (see actions manifest) against a lemur click (picks against the
     * passed node).
     * <p>
     * This requires lemur to be on the class path (or else you'll get an exception).
     * <p>
     * It kind of "fakes" a click. So it's only limited in what it does. It tracks up the parents of things it hits looking
     * for things which are a button, or have a MouseEventControl. If it finds one it clicks that, then returns.
     * <p>
     * Its worth noting that the MouseButtonEvents will not have meaningful x,y coordinates
     * <p>
     * More advanced functionality, like receiving on click on nothing events can be obtained by using the {@link BoundHand#addFunction(BoundHandFunction)}
     * method and adding a configured LemurClickFunction
     * <p>
     * <strong>NOTE: at present only a single action can be picked against (but potentially many nodes) at a time and old click actions will be deregistered.
     * However, that restriction may be lifted in later versions so old actions should be explicitly removed for forwards
     * compatibility</strong>
     * <p>
     * Use the {@link FunctionRegistration} to end the function when done
     *
     * @param clickAction the action (see action manifest) that will trigger a click, can be a vector1 or a digital action.
     * @param nodesToPickAgainst The node(s) that is picked against to look for lemur UIs
     * @return a Runnable that if called will end the click action
     */
    public FunctionRegistration setClickAction_lemurSupport(ActionHandle clickAction, Node... nodesToPickAgainst){
        assertLemurAvailable();
        clearClickAction_lemurSupport(); //the reason for this is the way that with many nodes dominance becomes a problem, if attached as several ClickActions (would probably be fine if bound to different buttons)
        return addFunction(new LemurClickFunction(clickAction, nodesToPickAgainst));
    }
    /**
     * Will continuously look for touches between the node and the index finger tip. Will trigger lemur buttons etc (if
     * lemur is available) and Tamarin {@link AbstractTouchControl}. Filters out accidental double taps and only fires
     * when the fingertip first touches (I.e. putting your finger on a button will only generate a single event, not
     * continuous events while the finger remains in contact)
     * <p>
     * Use the {@link FunctionRegistration} to end the function when done
     *
     * @param nodeToScanForTouches the note to scan for contact with the fingertip
     * @param requireFingerPointing if the scan should only occur if the hand is in a pointing arrangement
     *                              (index finger outstretched, other fingers curled)
     * @param vibrateActionOnTouch the action name of the vibration binding (e.g. "/actions/main/out/haptic"). Can be null for no vibrate
     * @param vibrateOnTouchIntensity how hard the vibration response is. Should be between 0 (none) and 1 (lots)
     */
    public FunctionRegistration setFingerTipPressDetection(Node nodeToScanForTouches, boolean requireFingerPointing, ActionHandle vibrateActionOnTouch, float vibrateOnTouchIntensity){
        return addFunction(new PressFunction(nodeToScanForTouches, requireFingerPointing, vibrateActionOnTouch, vibrateOnTouchIntensity));
    }

    /**
     * Clears the click action.
     */
    public void clearClickAction_lemurSupport(){
        removeFunction(LemurClickFunction.class);
    }

    /**
     * Set the depth between the centre of the palm and the skin of the palm used by the hand model you have bound.
     * <p>
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
     * <p>
     * Use the {@link FunctionRegistration} to end the function when done
     * <p>
     * {@link BoundHand#removePickLine()} method
     * @param spatial the pick line (+X should be in the direction of the pick line)
     * @return a FunctionRegistration that will remove the pick line
     */
    public FunctionRegistration attachPickLine( Spatial spatial ){
        searchForGeometry(spatial).forEach(g -> g.setUserData(NO_PICK, true));
        pickLineNode.attachChild(spatial);
        return spatial::removeFromParent;
    }

    public void removePickLine(){
        pickLineNode.detachAllChildren();
    }

    protected void updateVelocityData(Vector3f velocity_world, RotationalVelocity rotationalVelocity_world){
        this.velocity_world = velocity_world;
        this.rotationalVelocity_world = rotationalVelocity_world;
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

    private Geometry sphere(ColorRGBA colorRGBA, Vector3f position, float radius){
        Sphere b = new Sphere(10, 10, radius);
        Geometry geom = new Geometry("debugHandSphere", b);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colorRGBA);
        geom.setMaterial(mat);
        geom.setUserData(NO_PICK, true);
        geom.setLocalTranslation(position);
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

    @Override
    public String toString(){
        return handSide.name() + " hand";
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

    public static boolean notNull(Object... objects){
        for(Object o:objects){
            if (o == null){
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the current state of the action (abstract version of a button press) generated by this hand only.
     * <p>
     * This is called for analog style actions (most commonly joysticks, but button pressure can also be mapped in analog).
     * <p>
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     * <p>
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from in
     * the action manifest default bindings.
     * <p>
     * @param actionHandle the handle for the action (just an object with the set name and action name)
     * @return the AnalogActionState that has details on if the state has changed, what the state is etc.
     */
    public FloatActionState getFloatActionState(ActionHandle actionHandle){
        return vrState.getFloatActionState(actionHandle, getHandSide().restrictToInputString);
    }

    /**
     * Triggers a haptic action (aka a vibration) restricted to just one input (e.g. left or right hand).
     * <p>
     * This method is typically used to bind the haptic to both hands then decide at run time which hand to sent to     *
     * <p>
     * @param actionHandle the handle for the action (just an object with the set name and action name)
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     */
    public void triggerHapticAction(ActionHandle actionHandle, float duration, float frequency, float amplitude){
        vrState.triggerHapticAction(actionHandle, duration, frequency, amplitude, getHandSide().restrictToInputString);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     * <p>
     * This is called for digital style actions (a button is pressed, or not)
     * <p>
     * This method is commonly called when it is important which hand the action is found on. For example while
     * holding a weapon a button may be bound to "eject magazine" to allow you to load a new one, but that would only
     * want to take effect on the hand that is holding the weapon
     * <p>
     * Note that this action must still be bound in the action manifest against this hand it to receive the input
     *
     * @param actionHandle the handle for the action (just an object with the set name and action name)
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    public BooleanActionState getBooleanActionState(ActionHandle actionHandle){
        return vrState.getBooleanActionState(actionHandle, getHandSide().restrictToInputString);
    }
}

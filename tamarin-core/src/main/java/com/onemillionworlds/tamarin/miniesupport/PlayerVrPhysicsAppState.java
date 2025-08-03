package com.onemillionworlds.tamarin.miniesupport;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.PhysicsSweepTestResult;
import com.jme3.bullet.collision.shapes.ConvexShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vinette.VrVignetteState;

import java.util.ArrayList;
import java.util.List;

/**
 * When added the player will be connected to the physics world. Meaning:
 * - (user code provided) walking requests  can be vetoed by physics (i.e. no walking into walls),
 * - if the players head is within a wall their view will be occluded
 *
 * <ul>
 *  <li>(user code provided) walking requests can be vetoed by physics (i.e. no walking into walls)</li>
 *  <li>if the players head is within a wall their view will be occluded</li>
 *  <li>If the player walks above a chasm they will fall</li>
 *  <li>If the player walks above a step they will rise</li>
 * </ul>
 *
 * <p>
 *     N.B. this class is a quick start player-physics integration. If you want deeply customised player physics interaction
 *     use this class as inspiration to create your own version
 * </p>
 *
 * <p>
 *     Note this class does not cause player hand physics. Use {@link KinematicHandPhysics} for that
 * </p>
 *
 */
@SuppressWarnings("unused")
public class PlayerVrPhysicsAppState extends BaseAppState{
    public static final String ID = "PlayerPhysicsAppState";

    private static final float FALL_CHECK_STEP_HEIGHT = 0.01f;

    private final PhysicsRigidBody headGhostSphere = new PhysicsRigidBody(new SphereCollisionShape(0.3f));

    private PhysicsSpace physicsSpace;
    private final VrVignetteState vignette = new VrVignetteState();
    private boolean playerIsFalling = false;
    private XrAppState vrAppState;

    private HeadOcclusionMode headOcclusionMode = HeadOcclusionMode.DEFAULT_RIGID_BODIES_CAUSE_OCCLUSION;
    private float headPenetrationAtMaxOcclusion = 0.10f;

    private float maximumAllowedStepHeight = 0.25f;

    private float playerFallSpeed = 10;

    public PlayerVrPhysicsAppState(){
        super(ID);
    }

    @Override
    protected void initialize(Application app){

        vrAppState = getState(XrAppState.ID, XrAppState.class);

        vignette.setVignetteAmount(0);
        getStateManager().attach(vignette);

        BulletAppState bulletAppState = getState(BulletAppState.class);
        physicsSpace = bulletAppState.getPhysicsSpace();
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}

    @Override
    public void update(float tpf){
        super.update(tpf);

        if(headOcclusionMode!=HeadOcclusionMode.OFF){
            List<Float> depths = new ArrayList<>(0);
            PhysicsCollisionListener collisionListener = event -> {

                Object applicationDataForOverride =
                        (event.getObjectA() == headGhostSphere
                        ? event.getObjectB()
                        : event.getObjectA()).getApplicationData();

                boolean shouldContribute = (applicationDataForOverride instanceof VrMinieAdvice)
                        ? ((VrMinieAdvice) applicationDataForOverride).shouldTriggerViewOcclusion()
                        : headOcclusionMode == HeadOcclusionMode.DEFAULT_RIGID_BODIES_CAUSE_OCCLUSION;

                if(shouldContribute){
                    depths.add(Math.abs(event.getDistance1()));
                }
            };
            headGhostSphere.setPhysicsLocation(vrAppState.getVrCameraPosition());
            physicsSpace.contactTest(headGhostSphere, collisionListener);

            float maxPenetration = 0;
            for(Float depth : depths){
                maxPenetration = Math.max(maxPenetration, depth);
            }

            vignette.setVignetteAmount(FastMath.clamp(maxPenetration / headPenetrationAtMaxOcclusion, 0, 1));
        }

        if(playerIsFalling){
            fall(tpf);
        }
    }

    /**
     * Will test the head position via a sphere around the eyes centre point. If the head is overlapping with a wall
     * vision will begin to be occluded. Once the head reaches enough depth
     */
    public void setHeadWallTestRadius(float radius){
        headGhostSphere.setCollisionShape(new SphereCollisionShape(radius));
    }

    /**
     * At this depth of the head being in a wall the vision will be completely blanked out
     */
    public void setHeadWallIntersectionAtMaximumOcclusion(float depth){
        headPenetrationAtMaxOcclusion = depth;
    }

    /**
     * Configures the head occlusion mode for the player. This determines how
     * the system handles occlusion effects when the player's head intersects
     * with objects or walls in the game environment.
     *
     * @param headOcclusionMode the {@code HeadOcclusionMode} specifying how head
     *                          occlusion calculations should be performed. Possible
     *                          values include disabling occlusion, causing all rigid
     *                          bodies to trigger occlusion, or selectively determining
     *                          which rigid bodies contribute to occlusion based on
     *                          additional criteria.
     */
    public void setHeadOcclusionMode(HeadOcclusionMode headOcclusionMode){
        this.headOcclusionMode = headOcclusionMode;
    }

    /**
     * Sets the occlusion color for the head vignette, which is used to visually
     * represent occlusion effects when the player's head intersects with an object
     * or wall in the game environment.
     *
     * @param occlusionColour the {@code ColorRGBA} defining the color of the vignette
     *                        used for head occlusion effects.
     */
    public void setHeadOcclusionColour(ColorRGBA occlusionColour){
        this.vignette.setVignetteColor(occlusionColour);
    }

    /**
     * Sets the maximum allowed step height for the player.
     * This value determines the threshold height that the player is capable of stepping
     * over during movements within the game world. Objects higher than this value will be
     * treated as walls (which block player movement)
     *
     * @param maximumAllowedStepHeight the maximum height, in world units, the player
     *                                 can step over while moving.
     */
    public void setMaximumAllowedStepHeight(float maximumAllowedStepHeight){
        this.maximumAllowedStepHeight = maximumAllowedStepHeight;
    }

    /**
     * Sets the fall speed for the player in the game environment.
     * Note the player does not accelerate, they immediately fall at
     * this speed
     */
    public void setPlayerFallSpeed(float playerFallSpeed){
        this.playerFallSpeed = playerFallSpeed;
    }

    /**
     * Moves the player by simulating walking in the specified direction.
     * This method considers collision detection, step height for inclines,
     * and falling conditions to determine the player's final position.
     * It ensures that the movement is valid within the physics world and
     * handles stepping up or falling if necessary.
     *
     * <p>
     *     If the movement would move the player into collision with the physics world then
     *     the movement is vetoed (i.e. doesn't happen)
     * </p>
     *
     * @param walkAmount A {@code Vector2f} representing the direction and magnitude
     *                   of the walk. The x and y components indicate the movement
     *                   on the horizontal plane (i.e. the traditional x and z).
     */
    public void moveByWalking(Vector2f walkAmount){
        if (walkAmount.length()>0){

            float sizeOfFootTest = 0.3f;
            ConvexShape footTestShape = new SphereCollisionShape(sizeOfFootTest);

            Vector3f startingWalkTestPosition = getPlayerFeetPosition().add(0, maximumAllowedStepHeight + sizeOfFootTest, 0);
            Vector3f endingFootPosition = startingWalkTestPosition.add(walkAmount.x, 0, walkAmount.y);

            Transform startingFootTransform = new Transform();
            startingFootTransform.setTranslation(startingWalkTestPosition);

            Transform endingFootTransform = new Transform();
            endingFootTransform.setTranslation(endingFootPosition);

            List<PhysicsSweepTestResult> results = physicsSpace.sweepTest(footTestShape, startingFootTransform, endingFootTransform)
                    .stream()
                    .filter(str -> {
                        Object applicationData = str.getCollisionObject().getApplicationData();
                        if(applicationData instanceof VrMinieAdvice vrMinieAdvice){
                            return vrMinieAdvice.shouldPreventPlayerWalkingThrough();
                        }
                        return true;
                    })
                    .toList();
            if(results.isEmpty()){
                // allow the motion
                getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(walkAmount.x, 0, walkAmount.y));

                // see if we should now "step up" as a result of an incline or fall
                float totalTestLineLength = sizeOfFootTest + maximumAllowedStepHeight + FALL_CHECK_STEP_HEIGHT;
                float bottomOfFootTestLineLength = sizeOfFootTest + maximumAllowedStepHeight;

                List<PhysicsRayTestResult> physicsRayTestResults = physicsSpace.rayTest(endingFootPosition, endingFootPosition.add(0, -totalTestLineLength, 0));

                if(physicsRayTestResults.isEmpty()){
                    playerIsFalling = true;
                } else{
                    // see if we should "step up"
                    float furthestPointFraction = Float.MAX_VALUE;
                    for(PhysicsRayTestResult rayTestResult : physicsRayTestResults){
                        furthestPointFraction = Math.min(furthestPointFraction, rayTestResult.getHitFraction());
                    }
                    float furthestPointLength = furthestPointFraction * totalTestLineLength;
                    if(furthestPointLength < bottomOfFootTestLineLength){
                        float stepUp = bottomOfFootTestLineLength - furthestPointLength;
                        getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(0, stepUp, 0));
                    }
                }
            }
        }
    }

    private void fall(float timeslice){
        Vector3f playerFootPosition = getPlayerFeetPosition();

        float distanceToTest = 1;

        List<PhysicsRayTestResult> physicsRayTestResults = physicsSpace.rayTest(playerFootPosition, playerFootPosition.add(0, -distanceToTest, 0));

        float fractionToGround = Float.MAX_VALUE;
        for(PhysicsRayTestResult rayTestResult : physicsRayTestResults){
            fractionToGround = Math.min(fractionToGround, rayTestResult.getHitFraction());
        }
        float distanceToGround = fractionToGround * distanceToTest;

        float distanceToFall = timeslice * playerFallSpeed;
        if(distanceToFall>distanceToGround){
            playerIsFalling = false;
            distanceToFall = distanceToGround;
        }

        getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(0, -distanceToFall, 0));
    }

    /**
     * The players feet are at the height of the observer, but the x,z of the cameras
     * @return the player's foot position
     */
    private Vector3f getPlayerFeetPosition(){
        return vrAppState.getPlayerFeetPosition();
    }

    private Node getObserver(){
        return vrAppState.getObserver();
    }

    @Override
    protected void cleanup(Application app){
        getStateManager().detach(vignette);
    }

    public enum HeadOcclusionMode{
        /**
         * Represents a state where head occlusion calculations are disabled.
         * When this mode is active, no occlusion effects are applied regardless
         * of the player's head position or its interaction with objects or walls
         * in the game environment.
         */
        OFF,

        /**
         * Represents a head occlusion mode where all rigid bodies by default contribute
         * to head occlusion calculations. This means that any rigid body intersecting
         * with the player's head will trigger occlusion effects. In order for a rigid
         * body to not cause occlusion the rigid body must have an applicationData
         * of type {@link VrMinieAdvice} and the method
         * {@link VrMinieAdvice#shouldTriggerViewOcclusion()} return false
         *
         * <p>
         * In this mode things like walls shouldn't have VrMinieAdvice and things
         * like bullets probably should
         * </p>
         */
        DEFAULT_RIGID_BODIES_CAUSE_OCCLUSION,
        /**
         * Represents a mode where-by default rigid bodies do not contribute to
         * head occlusion calculation. In order for a rigid body to cause occlusion
         * the rigid body must have an applicationData of type {@link VrMinieAdvice}
         * and the method {@link VrMinieAdvice#shouldTriggerViewOcclusion()} return
         * true.
         * <p>
         * In this mode things like walls probably should have VrMinieAdvice and things
         * like bullets probably shouldn't
         * </p>
         */
        DEFAULT_RIGID_BODIES_DO_NOT_CAUSE_OCCLUSION
    }

}

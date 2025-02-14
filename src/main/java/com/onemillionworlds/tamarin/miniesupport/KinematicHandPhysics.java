package com.onemillionworlds.tamarin.miniesupport;

import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import com.onemillionworlds.tamarin.vrhands.functions.GrabActionNormaliser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This binds the hands to physics. The hand will provide kinematic physics objects that allow for interactions between
 * the hands and physics objects
 *
 * <p>
 *     Use like:
 * </p>
 * <pre>
 * {@code
 *         vrHands.getHandControls().forEach(hand -> {
 *             functionRegistrations.add(hand.addFunction(new KinematicHandPhysics(bulletAppState.getPhysicsSpace())));
 *         });
 * }
 * </pre>
 *
 */
@SuppressWarnings("unused")
public class KinematicHandPhysics implements BoundHandFunction{

    private final Map<PhysicsRigidBody, Float> fingerInteractionImmunity = new HashMap<>();

    VrMinieAdvice vrMinieAdvice = VrMinieAdvice.builder()
            .setShouldPreventPlayerWalkingThrough(false) // don't have hands blocking own walking
            .setShouldTriggerViewOcclusion(false) // don't have hands close to eyes triggering occlusion
            .setObjectCanBePickedUp(false)
            .build();

    Quaternion fromZToY = new Quaternion();
    {
        fromZToY.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
    }

    private final PhysicsSpace physicsSpace;

    private static final List<JointPair> PAIRS_THAT_WILL_GET_PHYSICALISED = List.of(
            new JointPair(HandJoint.LITTLE_PROXIMAL_EXT, HandJoint.LITTLE_INTERMEDIATE_EXT),
            new JointPair(HandJoint.LITTLE_INTERMEDIATE_EXT, HandJoint.LITTLE_DISTAL_EXT),
            new JointPair(HandJoint.LITTLE_DISTAL_EXT, HandJoint.LITTLE_TIP_EXT),
            new JointPair(HandJoint.MIDDLE_PROXIMAL_EXT, HandJoint.MIDDLE_INTERMEDIATE_EXT),
            new JointPair(HandJoint.MIDDLE_INTERMEDIATE_EXT, HandJoint.MIDDLE_DISTAL_EXT),
            new JointPair(HandJoint.MIDDLE_DISTAL_EXT, HandJoint.MIDDLE_TIP_EXT),
            new JointPair(HandJoint.INDEX_PROXIMAL_EXT, HandJoint.INDEX_INTERMEDIATE_EXT),
            new JointPair(HandJoint.INDEX_INTERMEDIATE_EXT, HandJoint.INDEX_DISTAL_EXT),
            new JointPair(HandJoint.INDEX_DISTAL_EXT, HandJoint.INDEX_TIP_EXT),
            new JointPair(HandJoint.RING_PROXIMAL_EXT, HandJoint.RING_INTERMEDIATE_EXT),
            new JointPair(HandJoint.RING_INTERMEDIATE_EXT, HandJoint.RING_DISTAL_EXT),
            new JointPair(HandJoint.RING_DISTAL_EXT, HandJoint.RING_TIP_EXT),
            new JointPair(HandJoint.THUMB_PROXIMAL_EXT, HandJoint.THUMB_DISTAL_EXT),
            new JointPair(HandJoint.THUMB_DISTAL_EXT, HandJoint.THUMB_TIP_EXT)
    );

    private final Map<JointPair, PhysicsRigidBody> existingFingerParts = new HashMap<>();

    private final Map<JointPair, Vector3f> previousPositions = new HashMap<>();

    private final ActionHandle grabAction;

    private final GrabActionNormaliser grabActionNormaliser = new GrabActionNormaliser();

    /**
     * Allows the amount of pressure required to pick something up to be changed.
     * A value between 0 and 1
     */
    private float minimumGripToTrigger = 0.5f;

    private float lastGripPressure;

    private Optional<GripData> currentlyGrabbed = Optional.empty();

    private final PhysicsRigidBody grabShape = new PhysicsRigidBody(new SphereCollisionShape(0.05f), 10);

    public KinematicHandPhysics(PhysicsSpace physicsSpace){
        this.physicsSpace = physicsSpace;
        this.grabAction = null;
    }

    public KinematicHandPhysics(PhysicsSpace physicsSpace, ActionHandle grabAction){
        this.physicsSpace = physicsSpace;
        this.grabAction = grabAction;
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){

    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        existingFingerParts.values().forEach(physicsSpace::remove);
        existingFingerParts.clear();
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        for(JointPair pairGettingPhysicalised : PAIRS_THAT_WILL_GET_PHYSICALISED){
            BonePose from = boundHand.getBonePose_world(pairGettingPhysicalised.from());
            BonePose to = boundHand.getBonePose_world(pairGettingPhysicalised.to());

            if(from!=null && to!=null){
                PhysicsRigidBody rigidBody = existingFingerParts.computeIfAbsent(pairGettingPhysicalised, key -> buildNewRigidBody(from, to));
                Vector3f averagePosition = to.position().add(from.position()).multLocal(0.5f);

                rigidBody.setPhysicsLocation(averagePosition);
                rigidBody.setPhysicsRotation(from.orientation().mult(fromZToY));

                Vector3f previousPosition = previousPositions.getOrDefault(pairGettingPhysicalised, averagePosition);

                Vector3f velocity = averagePosition.subtract(previousPosition).multLocal(1/timeSlice);
                rigidBody.setLinearVelocity(velocity);

                previousPositions.put(pairGettingPhysicalised, averagePosition);
            }
        }

        if(grabAction!=null){
            float gripPressure = grabActionNormaliser.getGripActionPressure(boundHand, grabAction);

            Node palmNode = boundHand.getPalmNode();

            //the lastGripPressure stuff is so that a clenched fist isn't constantly trying to grab things
            if (gripPressure>minimumGripToTrigger && lastGripPressure<minimumGripToTrigger && currentlyGrabbed.isEmpty()){
                // do a physics pick
                grabShape.setPhysicsLocation(palmNode.getWorldTranslation());

                List<PhysicsRigidBody> grabbedBodies = new ArrayList<>(0);

                PhysicsCollisionListener collisionListener = event -> {
                    PhysicsCollisionObject physicsCollisionObject =
                            grabShape == event.getObjectA() ? event.getObjectB() : event.getObjectA();

                    if(physicsCollisionObject instanceof PhysicsRigidBody physicsRigidBody){
                        boolean shouldBeGrabbed = !physicsRigidBody.isKinematic() && physicsRigidBody.getMass() > 0f;
                        if(physicsCollisionObject.getApplicationData() instanceof VrMinieAdvice itemVrMinieAdvice){
                            shouldBeGrabbed = itemVrMinieAdvice.canBePickedUp();
                        }
                        if(shouldBeGrabbed){
                            grabbedBodies.add(physicsRigidBody);
                        }
                    }
                };

                physicsSpace.contactTest(grabShape, collisionListener);

                if(!grabbedBodies.isEmpty()){
                    PhysicsRigidBody bodyToBeGrabbed = grabbedBodies.get(0);
                    Vector3f startingPosition = bodyToBeGrabbed.getPhysicsLocation();
                    Quaternion startingRotation = bodyToBeGrabbed.getPhysicsRotation();

                    Vector3f startingPosition_local = palmNode.worldToLocal(startingPosition, null);
                    Quaternion startingRotation_local = palmNode.getWorldRotation().inverse().mult(startingRotation);

                    for(PhysicsRigidBody fingerPart : existingFingerParts.values()){
                        bodyToBeGrabbed.addToIgnoreList(fingerPart);
                    }

                    currentlyGrabbed = Optional.of(new GripData(
                            bodyToBeGrabbed,
                            startingPosition_local,
                            startingRotation_local
                    ));
                }
            }else if (gripPressure<minimumGripToTrigger && currentlyGrabbed.isPresent()){
                //drop current item
                PhysicsRigidBody physicsRigidBody = currentlyGrabbed.get().getGrippedObject();
                fingerInteractionImmunity.put(physicsRigidBody, 1f);
                currentlyGrabbed = Optional.empty();

            }
            lastGripPressure = gripPressure;

            if(currentlyGrabbed.isPresent()){
                GripData gripData = currentlyGrabbed.get();
                PhysicsRigidBody grippedObject = gripData.getGrippedObject();
                Vector3f relativeGripPosition = gripData.getRelativeGripPosition();
                Quaternion relativeRotation = gripData.getRelativeRotation();
                Vector3f position_world = palmNode.localToWorld(relativeGripPosition, null);
                Quaternion rotation_world = palmNode.getWorldRotation().mult(relativeRotation);

                Vector3f velocity = position_world.subtract(gripData.lastPosition).multLocal(1/timeSlice);

                grippedObject.setPhysicsLocation(position_world);
                grippedObject.setPhysicsRotation(rotation_world);
                grippedObject.setLinearVelocity(velocity);
                grippedObject.setAngularVelocity(Vector3f.ZERO);
                gripData.setLastPosition(position_world);
            }

            // reduce all items in fingerInteractionImmunity by timeslice
            reduceFingerInteractionImmunity(timeSlice);
        }
    }

    // Reduce all items in fingerInteractionImmunity by timeslice
    private void reduceFingerInteractionImmunity(float timeSlice) {
        fingerInteractionImmunity.replaceAll((key, value) -> Math.max(0, value - timeSlice));
        fingerInteractionImmunity.entrySet().removeIf(entry -> {
            boolean endImunity = entry.getValue() <= 0;
            if(endImunity){
                for(PhysicsRigidBody fingerPart : existingFingerParts.values()){
                    entry.getKey().removeFromIgnoreList(fingerPart);
                }
            }
            return endImunity;
        });
    }

    public void setMinimumGripToTrigger(float minimumGripToTrigger){
        this.minimumGripToTrigger = minimumGripToTrigger;
    }

    private PhysicsRigidBody buildNewRigidBody(BonePose from, BonePose to){
        float length = to.position().distance(from.position());
        CollisionShape shape = new CapsuleCollisionShape(to.radius(), length/2);

        PhysicsRigidBody physicsRigidBody = new PhysicsRigidBody(shape, 10);
        physicsRigidBody.setApplicationData(vrMinieAdvice);
        physicsRigidBody.setKinematic(true);

        physicsSpace.add(physicsRigidBody);
        return physicsRigidBody;
    }

    private static class GripData{
        private final PhysicsRigidBody grippedObject;
        private final Vector3f relativeGripPosition;
        private final Quaternion relativeRotation;
        private Vector3f lastPosition;

        public GripData(
                PhysicsRigidBody grippedObject,
                Vector3f relativeGripPosition,
                Quaternion relativeRotation
        ){
            this.grippedObject = grippedObject;
            this.relativeGripPosition = relativeGripPosition;
            this.relativeRotation = relativeRotation;
            this.lastPosition = grippedObject.getPhysicsLocation().clone();
        }

        public void setLastPosition(Vector3f lastPosition){
            this.lastPosition = lastPosition;
        }

        public PhysicsRigidBody getGrippedObject(){
            return grippedObject;
        }

        public Vector3f getRelativeGripPosition(){
            return relativeGripPosition;
        }

        public Quaternion getRelativeRotation(){
            return relativeRotation;
        }

        public Vector3f getLastPosition(){
            return lastPosition;
        }
    }

    private record JointPair(
            HandJoint from,
            HandJoint to
    ){
    }
}

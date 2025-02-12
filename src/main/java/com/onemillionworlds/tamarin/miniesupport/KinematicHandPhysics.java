package com.onemillionworlds.tamarin.miniesupport;

import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This binds the hands to physics. The hand will provide kinematic physics objects that allow for interactions between
 * the hands and physics objects
 *
 * <p>
 *     Use like:
 * <pre>
 * {@code
 *         vrHands.getHandControls().forEach(hand -> {
 *             functionRegistrations.add(hand.addFunction(new KinematicHandPhysics(bulletAppState.getPhysicsSpace())));
 *         });
 * }
 * </pre>
 * </p>
 */
@SuppressWarnings("unused")
public class KinematicHandPhysics implements BoundHandFunction{

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

    public KinematicHandPhysics(PhysicsSpace physicsSpace){
        this.physicsSpace = physicsSpace;
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
            }
        }
    }

    private PhysicsRigidBody buildNewRigidBody(BonePose from, BonePose to){
        float length = to.position().distance(from.position());
        CollisionShape shape = new CapsuleCollisionShape(to.radius(), length/2);

        PhysicsRigidBody physicsRigidBody = new PhysicsRigidBody(shape, 10);
        physicsRigidBody.setKinematic(true);
        physicsSpace.add(physicsRigidBody);
        return physicsRigidBody;
    }

    private record JointPair(
            HandJoint from,
            HandJoint to
    ){
    }
}

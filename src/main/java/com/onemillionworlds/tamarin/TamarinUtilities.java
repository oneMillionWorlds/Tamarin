package com.onemillionworlds.tamarin;

import com.jme3.app.VRAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.ArrayList;
import java.util.List;

public class TamarinUtilities{

    /**
     * Often you'll want to move the player to a particular location, however, the players head should never be moved
     * directly (as it is defined by the headset) instead the section of the real world that overlaps with the virtual
     * world should be moved such that the headset ends up requesting what you wanted anyway. This method achieves that
     * @param vrAppState the VRAppState
     * @param feetPosition where the players feet should be after the move
     */
    public static void movePlayerFeetToPosition(VRAppState vrAppState, Vector3f feetPosition){
        Vector3f playerCurrentPosition = vrAppState.getVRViewManager().getLeftCamera().getLocation().add(vrAppState.getVRViewManager().getRightCamera().getLocation()).mult(0.5f);
        Node observerNode = (Node)vrAppState.getObserver();
        Vector3f movement = feetPosition.subtract(playerCurrentPosition);

        Vector3f currentObserverPosition = observerNode.getLocalTranslation();

        observerNode.setLocalTranslation(currentObserverPosition.x+movement.x, feetPosition.y, currentObserverPosition.z+movement.z);
    }

    /**
     * This will rotate the observer such that the player is looking at the requested position. Only considered rotation
     * int the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     * If the position is the same as the current position, this will do nothing.
     */
    public static void playerLookAtPosition(VRAppState vrAppState, Vector3f position){
        Vector3f currentPosition = getVrCameraPosition(vrAppState);

        Vector3f desiredLookDirection = position.subtract(currentPosition);
        desiredLookDirection.y = 0;
        if (desiredLookDirection.lengthSquared()>0){
            playerLookInDirection(vrAppState, desiredLookDirection.normalizeLocal());
        }
    }

    /**
     * This will rotate the observer such that the player is looking in the requested direction. Only considered rotation
     * int the X-Z plane so the y coordinate is ignored (and so you won't get your universe all messed up relative to the
     * real world).
     */
    public static void playerLookInDirection(VRAppState vrAppState, Vector3f lookDirection){
        Vector3f currentLookDirection = new Vector3f(getVrCameraLookDirection(vrAppState));
        currentLookDirection.y = 0;
        Vector3f requestedLookDirection = new Vector3f(lookDirection);
        requestedLookDirection.y = 0  ;

        if (currentLookDirection.lengthSquared()>0 && requestedLookDirection.lengthSquared()>0){
            @SuppressWarnings("SuspiciousNameCombination")
            float currentAngle = FastMath.atan2(currentLookDirection.x, currentLookDirection.z);
            @SuppressWarnings("SuspiciousNameCombination")
            float requestedAngle = FastMath.atan2(requestedLookDirection.x, requestedLookDirection.z);
            float changeInAngle = requestedAngle - currentAngle;
            rotateObserverWithoutMovingPlayer(vrAppState, changeInAngle);
        }
    }

    /**
     * Often you'll want to programatically turn the player, which should be done by rotating the observer.
     * However, if the player isn't standing directly above the observer this rotation will induce motion.
     * This method corrects for that and gives the impression the player is just turning
     * @param vrAppState the VRAppState
     * @param angleAboutYAxis the requested turn angle. Positive numbers turn left, negative numbers turn right
     */
    public static void rotateObserverWithoutMovingPlayer(VRAppState vrAppState, float angleAboutYAxis){
        Node observerNode = (Node)vrAppState.getObserver();

        Quaternion currentRotation = observerNode.getLocalRotation();
        Quaternion leftTurn = new Quaternion();
        leftTurn.fromAngleAxis(angleAboutYAxis, Vector3f.UNIT_Y);

        /* Because the player may be a short distance from the observer rotating the observer may move the
         * player. This requires that a small movement in the observer occur along with the rotation
         */
        Vector3f playerStartPosition = vrAppState.getVRViewManager().getLeftCamera().getLocation().add(vrAppState.getVRViewManager().getRightCamera().getLocation()).mult(0.5f);
        Vector3f playerStartPositionObserverRelative = observerNode.worldToLocal(playerStartPosition, null);
        observerNode.setLocalRotation(leftTurn.mult(currentRotation));
        Vector3f playerPositionAfterRotation = observerNode.localToWorld(playerStartPositionObserverRelative, null);

        Vector3f inducedError = playerPositionAfterRotation.subtract(playerStartPosition);
        observerNode.setLocalTranslation(observerNode.getLocalTranslation().subtract(inducedError));
    }

    /**
     * Returns the rotation of the VR cameras (technically the left camera, but they should be the same
     */
    public static Vector3f getVrCameraLookDirection(VRAppState vrAppState){
        return vrAppState.getVRViewManager().getLeftCamera().getDirection();
    }

    /**
     * Returns the average position of the 2 VR cameras (i.e. half way between the left and right eyes)
     */
    public static Vector3f getVrCameraPosition(VRAppState vrAppState){
        return vrAppState.getVRViewManager().getLeftCamera().getLocation().add(vrAppState.getVRViewManager().getRightCamera().getLocation()).mult(0.5f);
    }

    /**
     * Will search the collision results (and all parents) looking for controls of that class.
     * <p>
     * Note that this method respects the TAMARIN_STOP_BUBBLING user data. Meaning if a boolean of true is registered
     * with that key then it will stop looking up the parent chain
     */
    public static <T extends Control> List<T> findAllControlsInResults(Class<T> searchClass, CollisionResults collisionResults){
        List<T> results = new ArrayList<>(1); //usually we find 1 or zero results
        directResults:
        for(CollisionResult result : collisionResults){
            if (Boolean.TRUE.equals(result.getGeometry().getUserData(BoundHand.NO_PICK))){
                continue;
            }
            Spatial workingTarget = result.getGeometry();
            while(workingTarget !=null){
                if (Boolean.TRUE.equals(workingTarget.getUserData(BoundHand.TAMARIN_STOP_BUBBLING))){
                    continue directResults;
                }

                T control = workingTarget.getControl(searchClass);
                if (control !=null && !results.contains(control)){
                    results.add(control);
                }
                workingTarget = workingTarget.getParent();
            }
        }
        return results;
    }

}


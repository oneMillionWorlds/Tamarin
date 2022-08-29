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
import com.onemillionworlds.tamarin.lemursupport.LemurSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        currentObserverPosition.x+=movement.x;
        currentObserverPosition.z+=movement.z;
        currentObserverPosition.y = feetPosition.y;
    }

    /**
     * Often you'll want to programatically turn the player, which should be done by rotating the observer.
     *
     * However, if the player isn't standing directly above the observer this rotation will induce motion.
     *
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
     * Often you'll want to programatically turn the player, which should be done by rotating the observer.
     *
     * However, if the player isn't standing directly above the observer this rotation will induce motion.
     *
     * Deprecated, no need to pass observerNode
     *
     * This method corrects for that and gives the impression the player is just turning
     * @param observerNode the node that represents the observer (see tamarin wiki for explanation on observer node)
     * @param vrAppState the VRAppState
     * @param angleAboutYAxis the requested turn angle. Positive numbers turn left, negative numbers turn right
     */
    @Deprecated
    public static void rotateObserverWithoutMovingPlayer(Node observerNode, VRAppState vrAppState, float angleAboutYAxis){
        rotateObserverWithoutMovingPlayer(vrAppState, angleAboutYAxis);
    }

    /**
     * Will search the collision results (and all parents) looking for controls of that class.
     *
     * Note that this method respects the TAMARIN_STOP_BUBBLING user data. Meaning if a boolean of true is registered
     * with that key then it will stop looking up the parent chain
     */
    public static <T extends Control> Collection<T> findAllControlsInResults(Class<T> searchClass, CollisionResults collisionResults){
        Set<T> results = new HashSet<>(1); //usually we find 1 or zero results
        for(CollisionResult result : collisionResults){
            Spatial workingTarget = result.getGeometry();
            while(workingTarget !=null){
                if (Boolean.TRUE.equals(workingTarget.getUserData(LemurSupport.TAMARIN_STOP_BUBBLING))){
                    continue;
                }

                T control = workingTarget.getControl(searchClass);
                if (control !=null){
                    results.add(control);
                }
                workingTarget = workingTarget.getParent();
            }
        }
        return results;
    }

}


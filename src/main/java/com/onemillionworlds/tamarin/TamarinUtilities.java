package com.onemillionworlds.tamarin;

import com.jme3.app.VRAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class TamarinUtilities{

    /**
     * Often you'll want to programatically turn the player, which should be done by rotating the observer.
     *
     * However, if the player isn't standing directly above the observer this rotation will induce motion.
     *
     * This method corrects for that and gives the impression the player is just turning
     * @param observerNode the node that represents the observer (see tamarin wiki for explanation on observer node)
     * @param vrAppState the VRAppState
     * @param angleAboutYAxis the requested turn angle. Positive numbers turn left, negative numbers turn right
     */
    public static void rotateObserverWithoutMovingPlayer(Node observerNode, VRAppState vrAppState, float angleAboutYAxis){
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

}


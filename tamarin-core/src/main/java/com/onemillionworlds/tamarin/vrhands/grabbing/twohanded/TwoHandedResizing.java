package com.onemillionworlds.tamarin.vrhands.grabbing.twohanded;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Decides what happens to an object held in two hands as the hands move apart or together (see
 * {@link com.onemillionworlds.tamarin.vrhands.grabbing.AbstractRelativeMovingGrabControl#setTwoHandedResizing}).
 * <p>
 * Implementations can change the target in any way, e.g. its spatial scale ({@link UniformScaleTwoHandedResizing}) or
 * by rebuilding its mesh so that it grows while its border stays the same thickness. They then report where the
 * grasped point (the point that was midway between the hands) has ended up on the target, and the grab control
 * positions the target so that point stays midway between the hands.
 * </p>
 */
public interface TwoHandedResizing{

    /**
     * Called when a second hand grabs the target, before the first call to {@link #resize}.
     */
    default void twoHandedGrabStarted(Spatial target){}

    /**
     * Called every frame that two hands hold the target. Resize the target as wanted then return where the grasped
     * point is now, in the target's local coordinates.
     *
     * @return where the point that was midway between the hands (see
     * {@link TwoHandedResizeContext#getStartGraspPointOnTarget()}) is now, in the target's local coordinates. Return
     * that start point unchanged if the resize doesn't move it within the target's local coordinates (which is true of
     * changing the spatial's own scale).
     */
    Vector3f resize(TwoHandedResizeContext context);

    /**
     * Called when the target stops being held by two hands (one or both hands let go). A good place to finalise
     * anything that was only approximated while resizing.
     */
    default void twoHandedGrabEnded(Spatial target){}
}

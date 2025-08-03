package com.onemillionworlds.tamarin.vrhands.grabbing.restrictions;

import com.jme3.math.Vector3f;

public interface GrabMoveRestriction{

    /**
     * Enforce a restriction in the final position of the grabbed object. This is by default in coordinates relative to
     * the move target's parent (aka local translation).
     * However, restrictionUtilities can be used to do your calculations in global coordinates if that is more convenient.
     *
     * <p>
     *     The position is relative to the move target's parent (Generally the move target is the object being grabbed, but
     *     some usual Grab controls (like {@link com.onemillionworlds.tamarin.vrhands.grabbing.ParentRelativeMovingGrabControl}
     *     may have a different move target)
     * </p>
     *
     * @param naturalPositionLocal the unrestricted position (relative to the move target's parent). I.e. the position the
     *                              object would be moved to if there were no restrictions
     * @param restrictionUtilities a set of utilities that can be used to convert between local and global coordinates
     *                             or other useful functions to help with calculating the restriction
     * @return the restricted position (relative to the move target's parent)
     */
    Vector3f restrictPosition(Vector3f naturalPositionLocal, RestrictionUtilities restrictionUtilities);

}

package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Optional;

/**
 * A snap target is a point or shape that a grabbed object can snap to (i.e. when the object is close enough to the target
 * it will move to the target's position).
 */
public interface SnapTarget{

    /**
     * Tests this target to see if it should snap to the given position. If it should, the position it should snap to
     * is returned otherwise an empty optional is returned.
     *
     * <p>
     *     N.B. The coordinate system is decided at a higher level (i.e. global or local space)
     * </p>
     *
     * @param position the natural position of the object
     * @param restrictionUtilities utilities for coordinate conversions and similar (optional to use)
     * @return the position to snap to or an empty optional if it should not snap
     */
    Optional<Vector3f> shouldSnap(Vector3f position, RestrictionUtilities restrictionUtilities);

}

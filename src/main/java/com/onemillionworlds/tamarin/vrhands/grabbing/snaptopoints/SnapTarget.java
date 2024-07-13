package com.onemillionworlds.tamarin.vrhands.grabbing.snaptopoints;

import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictionUtilities;

import java.util.Optional;

/**
 * A snap target is a point or shape that a grabbed object can snap to (i.e. when the object is close enough to the target
 * it will move to the target's position).
 */
public abstract class SnapTarget{

    int priority = 0;

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
    public abstract Optional<Vector3f> shouldSnap(Vector3f position, RestrictionUtilities restrictionUtilities);

    /**
     * Gets the priority of this snap target. Higher priority snap targets are considered first (even if they are further
     * away). Higher numbers indicate higher priority.
     * @return the priority of this snap target. Higher numbers indicate higher priority.
     */
    public int getPriority(){
        return priority;
    }

    /**
     * Sets the priority of this snap target. Higher priority snap targets are considered first (even if they are further
     * away). Higher numbers indicate higher priority.
     * @param priority the priority of this snap target. Higher numbers indicate higher priority.
     */
    public void setPriority(int priority){
        this.priority = priority;
    }
}

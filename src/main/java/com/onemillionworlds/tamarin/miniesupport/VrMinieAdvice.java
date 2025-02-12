package com.onemillionworlds.tamarin.miniesupport;

/**
 * Should be set as the PhysicsRigidBody application data to override default behaviour
 */
public interface VrMinieAdvice{

    /**
     * Used with {@link PlayerPhysicsAppState} to override the behaviour of the
     * head intersecting with this object
     *
     * @return true if the view occlusion should be triggered, otherwise false.
     */
    boolean shouldTriggerViewOcclusion();

    /**
     * Determines whether the player should be prevented from walking through
     * the object this method is associated with.
     *
     * <p>
     *     PhysicsRigidBodies that do not have VrMinieAdvice are assumed to block walking through
     *     them
     * </p>
     *
     * @return true if the player should be prevented from walking through the object, otherwise false.
     */
    boolean shouldPreventPlayerWalkingThrough();

}

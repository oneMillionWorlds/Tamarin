package com.onemillionworlds.tamarin.miniesupport;

/**
 * Should be set as the PhysicsRigidBody application data to override default behaviour
 */
public interface VrMinieAdvice{

    /**
     * Used with {@link PlayerVrPhysicsAppState} to override the behaviour of the
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

    static VrMinieAdviceBuilder builder(){
        return new VrMinieAdviceBuilder();
    }

    class VrMinieAdviceBuilder{
        boolean shouldTriggerViewOcclusion = true;
        boolean shouldPreventPlayerWalkingThrough = true;
        public VrMinieAdviceBuilder setShouldTriggerViewOcclusion(boolean shouldTriggerViewOcclusion){
            this.shouldTriggerViewOcclusion = shouldTriggerViewOcclusion;
            return this;
        }

        public VrMinieAdviceBuilder setShouldPreventPlayerWalkingThrough(boolean shouldPreventPlayerWalkingThrough){
            this.shouldPreventPlayerWalkingThrough = shouldPreventPlayerWalkingThrough;
            return this;
        }

        public VrMinieAdvice build(){
            return new VrMinieAdvice() {
                @Override
                public boolean shouldTriggerViewOcclusion() {
                    return shouldTriggerViewOcclusion;
                }
                @Override
                public boolean shouldPreventPlayerWalkingThrough() {
                    return shouldPreventPlayerWalkingThrough;
                }
            };
        }
    }

}

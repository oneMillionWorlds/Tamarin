package com.onemillionworlds.tamarin.vrhands.skeletonsynthesis;

import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.util.Map;

public interface SkeletonSynthesiser {
    /**
     * If the Synthesiser should preferentially use its own bone positions over the runtime-provided ones.
     */
    SynthesiseMode getSynthesiseMode();

    /**
     * Called once at startup. Can do any setup required like triggering async loads (or nothing)
     */
    void initialise();

    /**
     * Given the current grip and trigger pressures, synthesise the positions of the bones in the hand skeleton.
     */
    Map<HandJoint, BonePose> synthesiseBonePositions(BoundHand boundHand);

    enum SynthesiseMode{
        /**
         * Will only be used if the call to the runtime to get bone positions fails.
         */
        FALLBACK_ONLY,

        /**
         * Will be used in preference to the runtime-provided bone positions.
         */
        ALWAYS_SYNTHESISE
    }
}

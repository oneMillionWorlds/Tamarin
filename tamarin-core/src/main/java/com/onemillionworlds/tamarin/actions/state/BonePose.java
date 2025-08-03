package com.onemillionworlds.tamarin.actions.state;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @param position the bones position (relative the overall hand pose space)
 * @param orientation the bones orientation (relative the overall hand pose space)
 * @param radius the radius from the bones position to the skin's surface
 */
public record BonePose(
        Vector3f position,
        Quaternion orientation,
        float radius
){
}

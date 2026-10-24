package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * Rotation helpers for working out how a two-handed grasp has turned.
 */
final class GraspMaths{

    private static final float PARALLEL_TOLERANCE = 1e-6f;

    private GraspMaths(){}

    /**
     * The smallest rotation that turns the direction of {@code from} into the direction of {@code to}. Zero length
     * vectors give no rotation.
     */
    static Quaternion rotationBetween(Vector3f from, Vector3f to){
        Vector3f fromDirection = from.normalize();
        Vector3f toDirection = to.normalize();
        if(fromDirection.lengthSquared() == 0 || toDirection.lengthSquared() == 0){
            return new Quaternion();
        }
        float dot = fromDirection.dot(toDirection);
        if(dot < -1 + PARALLEL_TOLERANCE){
            return new Quaternion().fromAngleNormalAxis(FastMath.PI, anyPerpendicular(fromDirection));
        }
        Vector3f cross = fromDirection.cross(toDirection);
        return new Quaternion(cross.x, cross.y, cross.z, 1 + dot).normalizeLocal();
    }

    /**
     * The angle (in the range -PI to PI) that {@code rotation} turns about {@code unitAxis}, ignoring any swing of
     * that axis itself (a swing-twist decomposition).
     */
    static float twistAngle(Quaternion rotation, Vector3f unitAxis){
        float projection = rotation.getX() * unitAxis.x + rotation.getY() * unitAxis.y + rotation.getZ() * unitAxis.z;
        float w = rotation.getW();
        if(w < 0){
            projection = -projection;
            w = -w;
        }
        return 2 * FastMath.atan2(projection, w);
    }

    private static Vector3f anyPerpendicular(Vector3f unitVector){
        Vector3f other = Math.abs(unitVector.x) < 0.9f ? Vector3f.UNIT_X : Vector3f.UNIT_Y;
        return unitVector.cross(other).normalizeLocal();
    }
}

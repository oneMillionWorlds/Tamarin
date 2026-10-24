package com.onemillionworlds.tamarin.vrhands.grabbing;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.testhelpers.Vector3fAsserts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraspMathsTest{

    private static final float TOLERANCE = 1e-5f;

    @Test
    void rotationBetweenTurnsTheFirstDirectionOntoTheSecond(){
        Vector3f from = new Vector3f(2, 0, 0);
        Vector3f to = new Vector3f(0, 1, 1);

        Quaternion rotation = GraspMaths.rotationBetween(from, to);

        Vector3fAsserts.assertEquals(to.normalize(), rotation.mult(from.normalize()), TOLERANCE);
    }

    @Test
    void rotationBetweenParallelDirectionsIsNoRotation(){
        Quaternion rotation = GraspMaths.rotationBetween(new Vector3f(1, 2, 3), new Vector3f(2, 4, 6));

        Vector3fAsserts.assertEquals(Vector3f.UNIT_Y, rotation.mult(Vector3f.UNIT_Y), TOLERANCE);
    }

    @Test
    void rotationBetweenOppositeDirectionsIsAHalfTurn(){
        Vector3f from = new Vector3f(1, 0, 0);

        Quaternion rotation = GraspMaths.rotationBetween(from, new Vector3f(-3, 0, 0));

        Vector3fAsserts.assertEquals(new Vector3f(-1, 0, 0), rotation.mult(from), TOLERANCE);
    }

    @Test
    void rotationBetweenAZeroLengthVectorIsNoRotation(){
        Quaternion rotation = GraspMaths.rotationBetween(Vector3f.ZERO, Vector3f.UNIT_X);

        Vector3fAsserts.assertEquals(Vector3f.UNIT_Z, rotation.mult(Vector3f.UNIT_Z), TOLERANCE);
    }

    @Test
    void twistAngleOfARotationAboutTheAxisIsItsAngle(){
        Quaternion rotation = new Quaternion().fromAngleNormalAxis(0.7f, Vector3f.UNIT_X);

        assertEquals(0.7f, GraspMaths.twistAngle(rotation, Vector3f.UNIT_X), TOLERANCE);
        assertEquals(-0.7f, GraspMaths.twistAngle(rotation, Vector3f.UNIT_X.negate()), TOLERANCE);
    }

    @Test
    void twistAngleIgnoresRotationAboutPerpendicularAxes(){
        Quaternion rotation = new Quaternion().fromAngleNormalAxis(1.2f, Vector3f.UNIT_Y);

        assertEquals(0, GraspMaths.twistAngle(rotation, Vector3f.UNIT_X), TOLERANCE);
    }

    @Test
    void twistAngleIsUnaffectedByTheSignOfTheQuaternion(){
        Quaternion rotation = new Quaternion().fromAngleNormalAxis(0.4f, Vector3f.UNIT_Z);
        Quaternion negated = new Quaternion(-rotation.getX(), -rotation.getY(), -rotation.getZ(), -rotation.getW());

        assertEquals(0.4f, GraspMaths.twistAngle(negated, Vector3f.UNIT_Z), TOLERANCE);
    }

    @Test
    void twistAngleOfACombinedRotationIsJustTheTwist(){
        Quaternion twist = new Quaternion().fromAngleNormalAxis(FastMath.QUARTER_PI, Vector3f.UNIT_X);
        Quaternion swing = new Quaternion().fromAngleNormalAxis(0.3f, Vector3f.UNIT_Z);

        assertEquals(FastMath.QUARTER_PI, GraspMaths.twistAngle(swing.mult(twist), Vector3f.UNIT_X), TOLERANCE);
    }
}

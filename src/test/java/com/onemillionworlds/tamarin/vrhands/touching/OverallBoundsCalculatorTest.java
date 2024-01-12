package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.testhelpers.Vector3fAsserts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverallBoundsCalculatorTest{

    @Test
    public void testSingleGeometry() {
        Geometry geom = new Geometry("TestGeom", new Box(1, 1, 1));
        geom.setModelBound(new BoundingBox(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1)));
        geom.updateModelBound();

        BoundingBox result = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(geom);
        assertEquals(new Vector3f(0, 0, 0), result.getCenter());
        assertEquals(1, result.getXExtent(), 0.0001);
        assertEquals(1, result.getYExtent(), 0.0001);
        assertEquals(1, result.getZExtent(), 0.0001);
    }

    @Test
    public void testNodeWithMultipleGeometries() {
        Node node = new Node("TestNode");

        Geometry geom = new Geometry("Geom1", new Box(1, 1, 1));
        node.attachChild(geom);

        Geometry geom2 = new Geometry("Geom1", new Box(1, 2, 1));
        geom2.setLocalTranslation(10, 2, 0);
        node.attachChild(geom2);

        BoundingBox result = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(node);
        Vector3f minimum = result.getMin(new Vector3f());
        Vector3f maximum = result.getMax(new Vector3f());

        Vector3fAsserts.assertEquals(new Vector3f(-1, -1, -1), minimum, 0.0001f);
        Vector3fAsserts.assertEquals(new Vector3f(11, 4, 1), maximum, 0.0001f);
    }

    @Test
    public void testNestedNodes() {
        Node node = new Node("TestNode");
        Node child = new Node("TestNode");
        node.attachChild(child);
        child.setLocalTranslation(10, 0, 0);

        Geometry geom = new Geometry("Geom1", new Box(1, 1, 1));
        geom.setLocalTranslation(0, 0, 5);
        child.attachChild(geom);


        BoundingBox result = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(node);
        Vector3f minimum = result.getMin(new Vector3f());
        Vector3f maximum = result.getMax(new Vector3f());

        Vector3fAsserts.assertEquals(new Vector3f(9, -1, 4), minimum, 0.0001f);
        Vector3fAsserts.assertEquals(new Vector3f(11, 1, 6), maximum, 0.0001f);
    }

    @Test
    public void rotatedGeometry() {
        Node node = new Node("TestNode");

        Geometry geom = new Geometry("Geom1", new Box(1, 1, 1));
        geom.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y));
        node.attachChild(geom);

        BoundingBox result = OverallBoundsCalculator.getOverallBoundsLocalisedToSpatialOrigin(node);
        Vector3f minimum = result.getMin(new Vector3f());
        Vector3f maximum = result.getMax(new Vector3f());

        Vector3fAsserts.assertEquals(new Vector3f(-FastMath.sqrt(2), -1, -FastMath.sqrt(2)), minimum, 0.0001f);
        Vector3fAsserts.assertEquals(new Vector3f(FastMath.sqrt(2), 1, FastMath.sqrt(2)), maximum, 0.0001f);
    }

}
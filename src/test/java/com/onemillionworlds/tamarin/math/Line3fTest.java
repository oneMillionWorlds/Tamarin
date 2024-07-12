package com.onemillionworlds.tamarin.math;

import com.onemillionworlds.tamarin.testhelpers.Line3fAsserts;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import com.jme3.math.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class Line3fTest{
    @Test
    public void clipToBeWithinRectangle_parallelLines() {
        Line3f parallelToX = new Line3f(new Vector3f(1,2,3),new Vector3f(1,4,6));

        assertTrue(parallelToX.clipToBeWithinRectangle(new Vector3f(1.1f,2,3),new Vector3f(1.1f,4,6)).isEmpty(), "clipped away entirely when parallel");

        Line3f parallelToXClippedDown = parallelToX.clipToBeWithinRectangle(new Vector3f(0,0,0),new Vector3f(2,5,7)).get();
        Line3fAsserts.assertEquals("Not clipped away entirely when parallel", parallelToX, parallelToXClippedDown,0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_exactlyOnOuterFace() {
        Line3f onEdgeOfCube = new Line3f(new Vector3f(1,0.5f,0.5f),new Vector3f(1,0.7f,0.7f));

        Optional<Line3f> clipped = onEdgeOfCube.clipToBeWithinRectangle(new Vector3f(0,0,0),new Vector3f(1,1,1));

        Line3fAsserts.assertEquals("Not clipped away entirely when parallel", Optional.of(onEdgeOfCube), clipped, 0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_exactlyOnInnerFace() {
        Line3f onEdgeOfCube = new Line3f(new Vector3f(0,0.5f,0.5f),new Vector3f(0,0.7f,0.7f));

        Optional<Line3f> clipped = onEdgeOfCube.clipToBeWithinRectangle(new Vector3f(0,0,0),new Vector3f(1,1,1));

        Line3fAsserts.assertEquals("Not clipped away entirely when parallel", Optional.of(onEdgeOfCube), clipped, 0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_oneAxisLines(){
        Line3f startLine = new Line3f(new Vector3f(4,1,5),new Vector3f(4,10,5));

        Line3f clippedDown = startLine.clipToBeWithinRectangle(new Vector3f(0,4,0),new Vector3f(10,5,10)).get();
        Line3f expected =  new Line3f(new Vector3f(4,4,5),new Vector3f(4,5,5));
        Line3fAsserts.assertEquals("Clipped when straight", expected, clippedDown,0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_oneAxisLines_topToBottom(){
        Line3f startLine = new Line3f(new Vector3f(4,10,5),new Vector3f(4,1,5));

        Line3f clippedDown = startLine.clipToBeWithinRectangle(new Vector3f(0,4,0),new Vector3f(10,5,10)).get();
        Line3f expected =  new Line3f(new Vector3f(4,5,5),new Vector3f(4,4,5));
        Line3fAsserts.assertEquals("Clipped when straight", expected, clippedDown,0.0001f);

    }

    @Test
    public void clipToBeWithinRectangle_threeAxisLines_throughCorner(){
        Line3f startingLine = new Line3f(new Vector3f(1,1,1),new Vector3f(10,10,10));

        Line3f clippedDown = startingLine.clipToBeWithinRectangle(new Vector3f(4,4,4),new Vector3f(5,5,5)).get();
        Line3f expected =  new Line3f(new Vector3f(4,4,4),new Vector3f(5,5,5));
        Line3fAsserts.assertEquals("Clipped when through corners", expected, clippedDown,0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_threeAxisLines_throughFace(){
        Line3f startingLine = new Line3f(new Vector3f(5,0,5),new Vector3f(6,10,6));

        Line3f clippedDown = startingLine.clipToBeWithinRectangle(new Vector3f(0,4,0),new Vector3f(10,5,10)).get();
        Line3f expected =  new Line3f(new Vector3f(5.4f,4,5.4f),new Vector3f(5.5f,5,5.5f));
        Line3fAsserts.assertEquals("Clipped when going through face", expected, clippedDown,0.0001f);
    }

    @Test
    public void clipToBeWithinRectangle_threeAxisLines_Eliminated(){
        Line3f startingLine = new Line3f(new Vector3f(5,0,5),new Vector3f(6,10,6));

        Optional<Line3f> clippedDown = startingLine.clipToBeWithinRectangle(new Vector3f(0,0,0),new Vector3f(1,1,10));
        Optional<Line3f> expected = Optional.empty();
        assertEquals(clippedDown, expected, "Line eliminated entirely");

    }
}
/* [2018] (C) Richard Tingle */
package com.onemillionworlds.tamarin.testhelpers;


import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.math.Line3f;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;


public class Line3fAsserts{
    public static void assertEquals(Line3f expected, Line3f actual, float permittedError){
        assertEquals("", expected, actual, permittedError);
    }

    public static void assertEquals(String message, Line3f expected, Line3f actual, float permittedError){
        boolean equal = equals(expected, actual, permittedError);

        if(!message.isEmpty()){
            message = message + ". ";
        }

        if(!equal){
            fail(message + "Expected " + expected+ " Got " + actual);
        }
    }

    public static void assertEquals(String message, Optional<Line3f> expected, Optional<Line3f> actual, float permittedError){
        if(expected.isPresent() != actual.isPresent()){
            fail(message + "Expected " + expected+ " Got " + actual);
        }
        if(expected.isEmpty()){
            return;
        }

        boolean equal = equals(expected.get(), actual.get(), permittedError);

        if(!message.isEmpty()){
            message = message + ". ";
        }

        if(!equal){
            fail(message + "Expected " + expected+ " Got " + actual);
        }
    }


    public static void assertNotEquals(Line3f expected, Line3f actual, float permittedError){
        boolean equal = equals(expected, actual, permittedError);

        if(equal){
            fail("Expected not to" + expected + " Got " + actual);
        }
    }

    private static boolean equals(Line3f a, Line3f b, float permittedError) {
        if (a == b){
            return true;
        }
        if (a == null || b == null){
            return false;
        }
        Vector3f aStart = a.start;
        Vector3f aEnd = a.end;
        Vector3f bStart = b.start;
        Vector3f bEnd = b.end;

        if (!Vector3fAsserts.equals(aStart, bStart, permittedError)){
            return false;
        }
        return Vector3fAsserts.equals(aEnd, bEnd, permittedError);
    }
}

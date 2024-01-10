/* [2018] (C) Richard Tingle */
package com.onemillionworlds.tamarin.testhelpers;


import com.jme3.math.Vector3f;

import static org.junit.jupiter.api.Assertions.fail;


public class Vector3fAsserts{
    public static void assertEquals(Vector3f expected, Vector3f actual, float permittedError){
        assertEquals("", expected, actual, permittedError);
    }

    public static void assertEquals(String message, Vector3f expected, Vector3f actual, float permittedError){
        boolean equal = equals(expected, actual, permittedError);

        if(!message.isEmpty()){
            message = message + ". ";
        }

        if(!equal){
            fail(message + "Expected " + expected+ " Got " + actual);
        }
    }


    public static void assertNotEquals(Vector3f expected, Vector3f actual, float permittedError){
        boolean equal = equals(expected, actual, permittedError);

        if(equal){
            fail("Expected not to" + expected + " Got " + actual);
        }
    }

    private static boolean equals(Vector3f a, Vector3f b, float permittedError) {
        if (a == b){
            return true;
        }
        if (a == null || b == null){
            return false;
        }


        if (Math.abs(a.x - b.x) > permittedError) return false;
        if (Math.abs(a.y - b.y) > permittedError) return false;
        if (Math.abs(a.z - b.z) > permittedError) return false;
        return true;
    }
}

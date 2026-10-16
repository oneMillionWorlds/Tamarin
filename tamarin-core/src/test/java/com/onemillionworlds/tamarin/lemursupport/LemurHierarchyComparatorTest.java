package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LemurHierarchyComparatorTest{

    private static final Predicate<Spatial> IS_LEMUR_ELEMENT = spatial -> spatial.getName() != null && spatial.getName().startsWith("lemur");

    @Test
    void withinOneLemurUiLowerLocalZIsDrawnFirstWhereverTheCameraLooks(){
        Node scene = new Node("scene");
        Node window = lemurWindow(scene, new Vector3f(0, 0, -2));
        Geometry windowBackground = geometry("windowBackground", window, new Vector3f(0, 0, 0), 1);
        Node button = new Node("lemurButton");
        button.setLocalTranslation(0.5f, 0.5f, 0.01f);
        window.attachChild(button);
        Geometry buttonBackground = geometry("buttonBackground", button, new Vector3f(0, 0, 0), 0.2f);
        scene.updateGeometricState();

        for(boolean opaque : new boolean[]{true, false}){
            LemurHierarchyComparator comparator = new LemurHierarchyComparator(opaque ? opaqueComparatorIgnoringMaterials() : new TransparentComparator(), opaque, IS_LEMUR_ELEMENT);
            for(Vector3f lookAt : new Vector3f[]{new Vector3f(0, 0, -2), new Vector3f(0.5f, 0.5f, -2), new Vector3f(0, 3, -2), new Vector3f(0, -3, -2), new Vector3f(3, 0, -2)}){
                comparator.setCamera(camera(Vector3f.ZERO, lookAt));
                assertTrue(comparator.compare(windowBackground, buttonBackground) < 0, "looking at " + lookAt);
                assertTrue(comparator.compare(buttonBackground, windowBackground) > 0, "looking at " + lookAt);
            }
        }
    }

    @Test
    void opaqueBucketDrawsLemurAfterOtherGeometries(){
        Node scene = new Node("scene");
        Node window = lemurWindow(scene, new Vector3f(0, 0, -10));
        Geometry windowBackground = geometry("windowBackground", window, new Vector3f(0, 0, 0), 1);
        Geometry nearbyCube = geometry("cube", scene, new Vector3f(0, 0, -1), 0.5f);
        scene.updateGeometricState();

        LemurHierarchyComparator comparator = new LemurHierarchyComparator(new OpaqueComparator(), true, IS_LEMUR_ELEMENT);
        comparator.setCamera(camera(Vector3f.ZERO, new Vector3f(0, 0, -1)));

        assertTrue(comparator.compare(nearbyCube, windowBackground) < 0);
        assertTrue(comparator.compare(windowBackground, nearbyCube) > 0);
    }

    @Test
    void transparentBucketDrawsSeparateLemurUisAndOtherGeometriesFurthestFirst(){
        Node scene = new Node("scene");
        Node nearWindow = lemurWindow(scene, new Vector3f(0, 0, -2));
        Geometry nearWindowBackground = geometry("nearWindowBackground", nearWindow, new Vector3f(0, 0, 0), 0.5f);
        Node farWindow = lemurWindow(scene, new Vector3f(0, 0, -10));
        Geometry farWindowBackground = geometry("farWindowBackground", farWindow, new Vector3f(0, 0, 0), 0.5f);
        Geometry middleCube = geometry("cube", scene, new Vector3f(0, 0, -5), 0.5f);
        scene.updateGeometricState();

        GeometryList list = new GeometryList(new LemurHierarchyComparator(new TransparentComparator(), false, IS_LEMUR_ELEMENT));
        list.add(nearWindowBackground);
        list.add(middleCube);
        list.add(farWindowBackground);
        list.setCamera(camera(Vector3f.ZERO, new Vector3f(0, 0, -1)));
        list.sort();

        assertSame(farWindowBackground, list.get(0));
        assertSame(middleCube, list.get(1));
        assertSame(nearWindowBackground, list.get(2));
    }

    @Test
    void sortingAMixedSceneFromManyDirectionsIsConsistent(){
        Random random = new Random(42);
        Node scene = new Node("scene");
        List<Geometry> geometries = new ArrayList<>();
        for(int windowIndex = 0; windowIndex < 4; windowIndex++){
            Node window = lemurWindow(scene, randomVector(random, 5));
            for(int i = 0; i < 15; i++){
                Node panel = new Node("lemurPanel");
                panel.setLocalTranslation(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextInt(3) * 0.01f);
                window.attachChild(panel);
                geometries.add(geometry("panelBackground", panel, new Vector3f(0, 0, 0), 0.1f));
            }
        }
        for(int i = 0; i < 40; i++){
            geometries.add(geometry("cube", scene, randomVector(random, 5), 0.2f));
        }
        scene.updateGeometricState();

        for(boolean opaque : new boolean[]{true, false}){
            GeometryList list = new GeometryList(new LemurHierarchyComparator(opaque ? opaqueComparatorIgnoringMaterials() : new TransparentComparator(), opaque, IS_LEMUR_ELEMENT));
            for(int frame = 0; frame < 50; frame++){
                list.clear();
                geometries.forEach(list::add);
                list.setCamera(camera(randomVector(random, 1), randomVector(random, 10)));
                list.sort();
                assertEquals(geometries.size(), list.size());
            }
        }
    }

    /**
     * The test geometries have no materials, so this sorts like {@link OpaqueComparator} but without the material sort.
     */
    private static OpaqueComparator opaqueComparatorIgnoringMaterials(){
        return new OpaqueComparator(){
            @Override
            public int compare(Geometry o1, Geometry o2){
                return Float.compare(distanceToCam(o1), distanceToCam(o2));
            }
        };
    }

    private static Node lemurWindow(Node scene, Vector3f position){
        Node window = new Node("lemurWindow");
        window.setLocalTranslation(position);
        scene.attachChild(window);
        return window;
    }

    private static Geometry geometry(String name, Node parent, Vector3f position, float halfSize){
        Geometry geometry = new Geometry(name, new Box(halfSize, halfSize, 0.001f));
        geometry.setLocalTranslation(position);
        parent.attachChild(geometry);
        return geometry;
    }

    private static Camera camera(Vector3f location, Vector3f lookAt){
        Camera camera = new Camera(100, 100);
        camera.setFrustumPerspective(90, 1, 0.1f, 100);
        camera.setLocation(location);
        camera.lookAt(lookAt, Vector3f.UNIT_Y.add(0.001f, 0, 0.001f));
        return camera;
    }

    private static Vector3f randomVector(Random random, float scale){
        return new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).multLocal(2 * scale);
    }
}

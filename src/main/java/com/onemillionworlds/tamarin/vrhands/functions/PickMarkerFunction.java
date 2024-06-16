package com.onemillionworlds.tamarin.vrhands.functions;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

/**
 * If given a node to pick against will pick to determine distance and place a marker there
 */
public class PickMarkerFunction implements BoundHandFunction{

    BoundHand boundHand;

    final Node pickMarkerAgainstContinuous;

    Spatial pickMarker;

    /**
     * This will set the hand to do a pick in the same direction as {@link BoundHand#pickBulkHand}/lemur clicks
     * and place a marker (by default a white sphere) at the point where the pick hits a geometry. This gives the
     * player an indication what they would pick it they clicked now; think of it like a mouse pointer in 3d space
     */
    public PickMarkerFunction(Node pickMarkerAgainstContinuous){
        this.pickMarkerAgainstContinuous = pickMarkerAgainstContinuous;
    }

    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand = boundHand;
        pickMarker = defaultPickMarker(boundHand.getAssetManager());
        boundHand.getHandNode_xPointing().attachChild(pickMarker);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        pickMarker.removeFromParent();
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){

        BoundHand.firstNonSkippedHit(boundHand.pickBulkHand(pickMarkerAgainstContinuous))
                .filter(hit -> Float.isFinite(hit.getDistance())) //work around for JME bug #2284
                .ifPresentOrElse(
                hit -> {
                    pickMarker.setCullHint(Spatial.CullHint.Inherit);
                    pickMarker.setLocalTranslation(hit.getDistance(), 0, 0);
                },
                () -> pickMarker.setCullHint(Spatial.CullHint.Always)
        );
    }

    private Geometry defaultPickMarker(AssetManager assetManager){
        Sphere sphere = new Sphere(15, 15, 0.01f);
        Geometry geometry = new Geometry("pickingMarker", sphere);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.White);
        geometry.setMaterial(material);
        geometry.setUserData(BoundHand.NO_PICK, true);
        return geometry;
    }

}

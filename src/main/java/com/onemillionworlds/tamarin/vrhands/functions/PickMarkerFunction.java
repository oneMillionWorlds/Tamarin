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
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;

import java.util.Optional;

/**
 * If given a node to pick against will pick to determine distance and place a marker there
 */
public class PickMarkerFunction implements BoundHandFunction{

    BoundHand boundHand;

    Optional<Node> pickMarkerAgainstContinuous = Optional.empty();

    Spatial pickMarker;

    /**
     * This will set the hand to do a pick in the same direction as {@link BoundHand#pickBulkHand}/{@link BoundHand#click_lemurSupport}
     * and place a marker (by default a white sphere) at the point where the pick hits a geometry. This gives the
     * player an indication what they would pick it they clicked now; think of it like a mouse pointer in 3d space
     * @param nodeToPickAgainst
     */
    public void setPickMarkerContinuous(Node nodeToPickAgainst){
        pickMarkerAgainstContinuous = Optional.of(nodeToPickAgainst);
        boundHand.getHandNode_xPointing().attachChild(pickMarker);
    }

    /**
     * This will stop that action started by {@link BoundHand#setPickMarkerContinuous}
     */
    public void clearPickMarkerContinuous(){
        pickMarkerAgainstContinuous = Optional.empty();
        pickMarker.removeFromParent();
    }


    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand = boundHand;
        pickMarker = defaultPickMarker(boundHand.getAssetManager());
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){

    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        pickMarkerAgainstContinuous.ifPresent(node -> {
            BoundHand.firstNonSkippedHit(boundHand.pickBulkHand(node)).ifPresentOrElse(
                    hit -> {
                        pickMarker.setCullHint(Spatial.CullHint.Inherit);
                        pickMarker.setLocalTranslation(hit.getDistance(), 0, 0);
                    },
                    () -> pickMarker.setCullHint(Spatial.CullHint.Always)

            );
        });
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

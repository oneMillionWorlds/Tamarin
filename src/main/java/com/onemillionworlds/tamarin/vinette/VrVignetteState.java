package com.onemillionworlds.tamarin.vinette;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;

/**
 * This is used to create VR vignettes where the players view is
 * vignetted down (usually as a comfort feature while moving or when
 * the player head clips a physics object)
 */
public class VrVignetteState extends BaseAppState{

    private final Node vignetteNode = new Node("VignetteNode");

    ViewportConfigurator viewportConfigurator;

    Material vignetteMaterial;

    float vignetteAmount = 0.5f;
    float edgeWidth = 0.1f;
    ColorRGBA vignetteColor = ColorRGBA.Black;

    @Override
    protected void initialize(Application app){
        XrBaseAppState xrState = getState(XrBaseAppState.ID, XrBaseAppState.class);

        XrBaseAppState.CameraResolution mainCameraResolution = xrState.getCameraResolution();

        Camera guiCamera = new Camera(mainCameraResolution.width(), mainCameraResolution.height());
        guiCamera.setParallelProjection(true);
        //guiCamera.setFrustum(0, 1, -mainCameraResolution.width() / 2f, mainCameraResolution.width() / 2f, mainCameraResolution.height() / 2f, -mainCameraResolution.height() / 2f);
        viewportConfigurator = xrState.addAdditionalViewport(AdditionalViewportRequest
                .builder(vignetteNode)
                .setCamera(guiCamera)
                        .setClearFlagsColor(false)
                        .setClearFlagsDepth(false)
                        .setClearFlagsStencil(false)
                .build()
        );

        Box testBox = new Box(10000,10000,10000);
        Geometry testGeometry = new Geometry("test", testBox);

        vignetteMaterial = new Material(getApplication().getAssetManager(),"Tamarin/Materials/Vignette.j3md");
        vignetteMaterial.setFloat("VignetteAmount", vignetteAmount);
        vignetteMaterial.setFloat("EdgeWidth", edgeWidth);
        vignetteMaterial.setColor("VignetteColor", vignetteColor);
        vignetteMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        /*Material vignetteMaterial = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        vignetteMaterial.setColor("Color", ColorRGBA.Blue);*/
        testGeometry.setMaterial(vignetteMaterial);
        vignetteNode.attachChild(testGeometry);

        vignetteNode.setQueueBucket(RenderQueue.Bucket.Gui);
        vignetteNode.setCullHint(Spatial.CullHint.Never);
    }

    public void setVignetteAmount(float amount){
        if(vignetteAmount != amount){
            vignetteAmount = amount;
            if(vignetteMaterial != null){
                vignetteMaterial.setFloat("VignetteAmount", amount);
            }
            vignetteNode.setCullHint(amount == 0 ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        }
    }

    public void setEdgeWidth(float width){
        edgeWidth = width;
        if(vignetteMaterial!=null){
            vignetteMaterial.setFloat("EdgeWidth", width);
        }

    }

    public void setVignetteColor(ColorRGBA color){
        vignetteColor = color;
        if(vignetteMaterial!=null){
            vignetteMaterial.setColor("VignetteColor", color);
        }
    }

    @Override
    public void update(float tpf) {
        vignetteNode.updateLogicalState(tpf);
        vignetteNode.updateGeometricState();
    }

    @Override
    protected void cleanup(Application app){
        viewportConfigurator.removeViewports();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }
}

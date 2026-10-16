package com.onemillionworlds.tamarin.viewports;

import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.onemillionworlds.tamarin.openxr.EyeSide;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AdditionalViewportData{

    private final Node rootNode;

    private final Map<FrameBuffer, ViewPort> overlayViewPorts = new HashMap<>();

    Consumer<ViewPort> configureViewport;

    private final RenderManager renderManager;

    private final AdditionalViewportRequest additionalViewportRequest;

    private final Camera leftCamera;
    private final Camera rightCamera;

    private final Consumer<ViewPort> standardConfiguration;

    public AdditionalViewportData(AdditionalViewportRequest additionalViewportRequest, RenderManager renderManager, Camera leftCamera, Camera rightCamera){
        this(additionalViewportRequest, renderManager, leftCamera, rightCamera, viewPort -> {});
    }

    /**
     * @param standardConfiguration configuration Tamarin applies to every viewport it creates, applied to each new
     *                              viewport before the request's own configuration.
     */
    public AdditionalViewportData(AdditionalViewportRequest additionalViewportRequest, RenderManager renderManager, Camera leftCamera, Camera rightCamera, Consumer<ViewPort> standardConfiguration){
        rootNode = additionalViewportRequest.getAdditionalRootNode();
        configureViewport = additionalViewportRequest.getConfigureViewport();
        this.renderManager = renderManager;
        this.additionalViewportRequest = additionalViewportRequest;
        this.leftCamera = leftCamera;
        this.rightCamera = rightCamera;
        this.standardConfiguration = standardConfiguration;
    }

    public ViewPort getAssociatedViewport(FrameBuffer fb, EyeSide eyeSide){
        return overlayViewPorts.computeIfAbsent(fb, (f) -> {
            String name = "Overlay Viewport " + Optional.ofNullable(rootNode).map(Spatial::getName).orElse("null") + " " +eyeSide+ " " + overlayViewPorts.size()/2;
            Camera camera = additionalViewportRequest.getCameraOverride().orElse(eyeSide == EyeSide.LEFT ? leftCamera : rightCamera);

            ViewPort newViewport;
            switch(additionalViewportRequest.getType()){
                case MAINVIEW:
                    newViewport = renderManager.createMainView(name, camera);
                    break;
                case POSTVIEW:
                    newViewport = renderManager.createPostView(name, camera);
                    break;
                case PREVIEW:
                    newViewport = renderManager.createPreView(name, camera);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + additionalViewportRequest.getType());
            }
            newViewport.setClearFlags(additionalViewportRequest.isClearFlags_color(), additionalViewportRequest.isClearFlags_depth(), additionalViewportRequest.isClearFlags_stencil());
            if(rootNode!=null){
                newViewport.attachScene(rootNode);
            }
            newViewport.setOutputFrameBuffer(fb);
            standardConfiguration.accept(newViewport);
            this.configureViewport.accept(newViewport);
            return newViewport;
        });
    }

    public void setActiveViewports(FrameBuffer frameBufferLeft, FrameBuffer frameBufferRight){
        overlayViewPorts.values().forEach(v -> v.setEnabled(false));

        ViewPort leftViewport = getAssociatedViewport(frameBufferLeft, EyeSide.LEFT);
        ViewPort rightViewport = getAssociatedViewport(frameBufferRight, EyeSide.RIGHT);
        leftViewport.setEnabled(true);
        rightViewport.setEnabled(true);
    }


    public void updateConfigureViewport(Consumer<ViewPort> configureViewport){
        this.configureViewport = configureViewport;
        overlayViewPorts.values().forEach(configureViewport);
    }

    public void cleanup(){
        overlayViewPorts.values().forEach((vp) -> {
            switch(additionalViewportRequest.getType()){
                case MAINVIEW:
                    renderManager.removeMainView(vp);
                    break;
                case POSTVIEW:
                    renderManager.removePostView(vp);
                    break;
                case PREVIEW:
                    renderManager.removePreView(vp);
                    break;
                default:
                    break;
            }
        });

    }
}

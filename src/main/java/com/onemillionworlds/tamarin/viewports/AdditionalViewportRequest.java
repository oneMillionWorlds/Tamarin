package com.onemillionworlds.tamarin.viewports;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
public class AdditionalViewportRequest{

    private final ViewportType type;

    private final Node additionalRootNode;
    private final Consumer<ViewPort> configureViewport;

    private final boolean clearFlags_color, clearFlags_depth, clearFlags_stencil;

    private boolean updateNode;

    private AdditionalViewportRequest(ViewportType type, Node additionalRootNode, Consumer<ViewPort> configureViewport, boolean clearFlags_color, boolean clearFlags_depth, boolean clearFlags_stencil, boolean updateNode){
        this.type = type;
        this.additionalRootNode = additionalRootNode;
        this.configureViewport = configureViewport;
        this.clearFlags_color = clearFlags_color;
        this.clearFlags_depth = clearFlags_depth;
        this.clearFlags_stencil = clearFlags_stencil;
        this.updateNode = updateNode;
    }

    /**
     * Returns a builder for an additional viewport request.
     * @param additionalRootNode the root node for the scene (this is the only mandatory parameter, the rest have
     *                           defaults appropriate for an overlay viewport)
     * @return the Builder
     */
    public static Builder builder(Node additionalRootNode){
        Builder builder = new Builder();
        builder.setAdditionalRootNode(additionalRootNode);
        return builder;
    }

    public static class Builder{
        private ViewportType type = ViewportType.MAINVIEW;
        private Node additionalRootNode;
        private Consumer<ViewPort> configureViewport = (vp) -> {};
        private boolean clearFlags_color = false;
        private boolean  clearFlags_depth = true;
        private boolean  clearFlags_stencil = false;

        private boolean updateNode = true;

        public Builder setType(ViewportType type){
            this.type = type;
            return this;
        }

        public Builder setAdditionalRootNode(Node additionalRootNode){
            this.additionalRootNode = additionalRootNode;
            return this;
        }

        public Builder setConfigureViewport(Consumer<ViewPort> configureViewport){
            this.configureViewport = configureViewport;
            return this;
        }

        public Builder setClearFlagsColor(boolean clearFlags_color){
            this.clearFlags_color = clearFlags_color;
            return this;
        }

        public Builder setClearFlagsDepth(boolean clearFlags_depth){
            this.clearFlags_depth = clearFlags_depth;
            return this;
        }

        public Builder setClearFlagsStencil(boolean clearFlags_stencil){
            this.clearFlags_stencil = clearFlags_stencil;
            return this;
        }

        /**
         * By default, tamarin will call updateLogicalState and updateGeometricState on the root node of the viewport.
         * If you don't want this to happen, set this to false.
         * @param updateNode update the node?
         * @return the builder
         */
        public Builder setUpdateNode(boolean updateNode){
            this.updateNode = updateNode;
            return this;
        }

        public AdditionalViewportRequest build(){
            return new AdditionalViewportRequest(type, additionalRootNode, configureViewport, clearFlags_color, clearFlags_depth, clearFlags_stencil, updateNode);
        }
    }

    public enum ViewportType{
        PREVIEW,
        MAINVIEW,
        POSTVIEW
    }
}

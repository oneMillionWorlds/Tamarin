package com.onemillionworlds.tamarin.viewports;

import com.jme3.renderer.ViewPort;

import java.util.function.Consumer;

public interface ViewportConfigurator{

    void updateViewportConfiguration(Consumer<ViewPort> configureViewport);

    void removeViewports();
}

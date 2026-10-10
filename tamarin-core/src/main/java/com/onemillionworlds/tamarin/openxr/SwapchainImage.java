package com.onemillionworlds.tamarin.openxr;

import com.jme3.texture.Image;

import java.util.ArrayList;

/**
 * This class really just exists to access the protected constructor of Image.
 */
public class SwapchainImage extends Image{

    public SwapchainImage(int id, Format format, int width, int height){
        super(id);
        data = new ArrayList<>(1);
        setFormat(format);
        setWidth(width);
        setHeight(height);
        // the texture is created and owned by the OpenXR runtime (often as immutable storage), JME must not try to
        // (re)upload it. If it does the first use of each swapchain image gives GL_INVALID_OPERATION from glTexImage2D
        clearUpdateNeeded();
    }
}

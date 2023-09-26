package com.onemillionworlds.tamarin.openxr;

import com.jme3.texture.Image;

import java.nio.ByteBuffer;
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
    }
}

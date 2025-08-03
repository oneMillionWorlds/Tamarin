package com.onemillionworlds.tamarin.openxr;

public enum DrawMode{
    /**
     * Draw directly into the swapchain images (fastest, but may not support features like MSAA because the swapchain images do not
     * support multisampling)
     */
    DIRECT,
    /**
     * Draw into a texture, then copy (blit) the texture into the swapchain images (slower, but may support features like MSAA)
     */
    BLITTED,
    /**
     * Let Tamarin decide which mode to use (Mostly if MSAA is requested will use blitted, otherwise direct)
     */
    AUTOSELECT

}

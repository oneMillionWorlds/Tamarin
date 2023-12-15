package com.onemillionworlds.tamarin.openxr;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.NativeObject;
import lombok.Setter;

import java.lang.ref.WeakReference;

/**
 * A delegating frame buffer pretends to be a framebuffer but forwards all calls to a different framebuffer.
 * <p>
 *    This is done to support triple buffering that OpenXR does (where each frame is rendered to a different framebuffer).
 * </p>
 * <p>
 *    This is done this way because things like the FilterPostProcessor like to steal the output frameBuffer and change the
 *    viewport to output to the filters own buffer. Doing it with the DelegatingFrameBuffer means that even if the
 *    FilterPostProcessor steals the framebuffer, it will still be stealing the one that we can change where it ultimately
 *    ends up; one of the 3 OpenXR framebuffers.
 * </p>
 */
@SuppressWarnings("deprecation")
public class DelegatingFrameBuffer extends FrameBuffer{

    @Setter
    private FrameBuffer delegatedFrameBuffer;
    
    public DelegatingFrameBuffer(){
        //the below is irrelevant as we always delegate to a different framebuffer
        super(1, 1, 1);
    }

    @Override
    public void addColorTarget(FrameBufferBufferTarget colorBuf){
        delegatedFrameBuffer.addColorTarget(colorBuf);
    }

    @Override
    public void addColorTarget(FrameBufferTextureTarget colorBuf){
        delegatedFrameBuffer.addColorTarget(colorBuf);
    }

    @Override
    public void addColorTarget(FrameBufferTextureTarget colorBuf, TextureCubeMap.Face face){
        delegatedFrameBuffer.addColorTarget(colorBuf, face);
    }

    @Override
    public void setDepthTarget(FrameBufferBufferTarget depthBuf){
        delegatedFrameBuffer.setDepthTarget(depthBuf);
    }

    @Override
    public void setDepthTarget(FrameBufferTextureTarget depthBuf){
        delegatedFrameBuffer.setDepthTarget(depthBuf);
    }

    @Override
    public int getNumColorTargets(){
        return delegatedFrameBuffer.getNumColorTargets();
    }

    @Override
    public RenderBuffer getColorTarget(int index){
        return delegatedFrameBuffer.getColorTarget(index);
    }

    @Override
    public RenderBuffer getColorTarget(){
        return delegatedFrameBuffer.getColorTarget();
    }

    @Override
    public RenderBuffer getDepthTarget(){
        return delegatedFrameBuffer.getDepthTarget();
    }

    @Override
    public void setMultiTarget(boolean enabled){
        delegatedFrameBuffer.setMultiTarget(enabled);
    }

    @Override
    public boolean isMultiTarget(){
        return delegatedFrameBuffer.isMultiTarget();
    }

    @Override
    public void setTargetIndex(int index){
        delegatedFrameBuffer.setTargetIndex(index);
    }

    @Override
    public int getTargetIndex(){
        return delegatedFrameBuffer.getTargetIndex();
    }

    @Override
    public void clearColorTargets(){
        delegatedFrameBuffer.clearColorTargets();
    }

    @Override
    public int getHeight(){
        return delegatedFrameBuffer.getHeight();
    }

    @Override
    public int getWidth(){
        return delegatedFrameBuffer.getWidth();
    }

    @Override
    public int getSamples(){
        return delegatedFrameBuffer.getSamples();
    }

    @Override
    public void resetObject(){
        delegatedFrameBuffer.resetObject();
    }

    @Override
    public void deleteObject(Object rendererObject){
        delegatedFrameBuffer.deleteObject(rendererObject);
    }

    @Override
    public NativeObject createDestructableClone(){
        return delegatedFrameBuffer.createDestructableClone();
    }

    @Override
    public long getUniqueId(){
        return delegatedFrameBuffer.getUniqueId();
    }

    @Override
    public void setSrgb(boolean srgb){
        delegatedFrameBuffer.setSrgb(srgb);
    }

    @Override
    public boolean isSrgb(){
        return delegatedFrameBuffer.isSrgb();
    }

    @Override
    public String getName(){
        return delegatedFrameBuffer.getName();
    }

    @Override
    public void setName(String name){
        delegatedFrameBuffer.setName(name);
    }

    @Override
    public void setMipMapsGenerationHint(Boolean v){
        delegatedFrameBuffer.setMipMapsGenerationHint(v);
    }

    @Override
    public Boolean getMipMapsGenerationHint(){
        return delegatedFrameBuffer.getMipMapsGenerationHint();
    }

    @Override
    public void setId(int id){
        delegatedFrameBuffer.setId(id);
    }

    @Override
    public int getId(){
        return delegatedFrameBuffer.getId();
    }

    @Override
    public void setUpdateNeeded(){
        delegatedFrameBuffer.setUpdateNeeded();
    }

    @Override
    public void clearUpdateNeeded(){
        delegatedFrameBuffer.clearUpdateNeeded();
    }

    @Override
    public boolean isUpdateNeeded(){
        return delegatedFrameBuffer.isUpdateNeeded();
    }

    @Override
    protected NativeObject clone(){
        throw new UnsupportedOperationException("Delegating frame buffer does not support cloning");
    }

    @Override
    public void dispose(){
        delegatedFrameBuffer.dispose();
    }

    @Override
    public <T> WeakReference<T> getWeakRef(){
        return delegatedFrameBuffer.getWeakRef();
    }

    @Override
    public void setDepthBuffer(Image.Format format){
        delegatedFrameBuffer.setDepthBuffer(format);
    }

    @Override
    public void setColorBuffer(Image.Format format){
        delegatedFrameBuffer.setColorBuffer(format);
    }

    @Override
    public void setColorTexture(Texture2D tex){
        delegatedFrameBuffer.setColorTexture(tex);
    }

    @Override
    public void setColorTexture(TextureArray tex, int layer){
        delegatedFrameBuffer.setColorTexture(tex, layer);
    }

    @Override
    public void setColorTexture(TextureCubeMap tex, TextureCubeMap.Face face){
        delegatedFrameBuffer.setColorTexture(tex, face);
    }

    @Override
    public void addColorBuffer(Image.Format format){
        delegatedFrameBuffer.addColorBuffer(format);
    }

    @Override
    public void addColorTexture(Texture2D tex){
        delegatedFrameBuffer.addColorTexture(tex);
    }

    @Override
    public void addColorTexture(TextureArray tex, int layer){
        delegatedFrameBuffer.addColorTexture(tex, layer);
    }

    @Override
    public void addColorTexture(TextureCubeMap tex, TextureCubeMap.Face face){
        delegatedFrameBuffer.addColorTexture(tex, face);
    }

    @Override
    public void setDepthTexture(Texture2D tex){
        delegatedFrameBuffer.setDepthTexture(tex);
    }

    @Override
    public void setDepthTexture(TextureArray tex, int layer){
        delegatedFrameBuffer.setDepthTexture(tex, layer);
    }

    @Override
    public int getNumColorBuffers(){
        return delegatedFrameBuffer.getNumColorBuffers();
    }

    @Override
    public RenderBuffer getColorBuffer(int index){
        return delegatedFrameBuffer.getColorBuffer(index);
    }

    @Override
    public RenderBuffer getColorBuffer(){
        return delegatedFrameBuffer.getColorBuffer();
    }

    @Override
    public RenderBuffer getDepthBuffer(){
        return delegatedFrameBuffer.getDepthBuffer();
    }
}

package org.Azkerash.riftfall.facial;

/** Exact rectangular atlas binding in texture pixels and head-local model pixels. No cube UV padding. */
public record AtlasPanel(int textureWidth,int textureHeight,int u,int v,int width,int height,
        float left,float top,float modelWidth,float modelHeight,float depth,boolean mirrorU) {
    public AtlasPanel {
        if(textureWidth<1 || textureHeight<1 || u<0 || v<0 || width<1 || height<1
                || u+width>textureWidth || v+height>textureHeight || modelWidth<=0 || modelHeight<=0)
            throw new IllegalArgumentException("Invalid atlas panel");
    }
    public float u0(){return (float)u/textureWidth;}
    public float v0(){return (float)v/textureHeight;}
    public float u1(){return (float)(u+width)/textureWidth;}
    public float v1(){return (float)(v+height)/textureHeight;}
    public float uLeft(){return mirrorU?u1():u0();}
    public float uRight(){return mirrorU?u0():u1();}
    public float xAt(float textureX){return left+(mirrorU?u+width-textureX:textureX-u)*modelWidth/width;}
    public float yAt(float textureY){return top+(textureY-v)*modelHeight/height;}
}

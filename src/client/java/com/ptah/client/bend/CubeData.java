package com.ptah.client.bend;

public final class CubeData {
    public final int u;
    public final int v;
    public final float x;
    public final float y;
    public final float z;
    public final float sizeX;
    public final float sizeY;
    public final float sizeZ;
    public final float growX;
    public final float growY;
    public final float growZ;
    public final boolean mirror;
    public final int textureWidth;
    public final int textureHeight;

    public CubeData(int u, int v, float x, float y, float z,
                    float sizeX, float sizeY, float sizeZ,
                    float growX, float growY, float growZ,
                    boolean mirror, float textureWidth, float textureHeight) {
        this.u = u;
        this.v = v;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.growX = growX;
        this.growY = growY;
        this.growZ = growZ;
        this.mirror = mirror;
        this.textureWidth = (int) textureWidth;
        this.textureHeight = (int) textureHeight;
    }
}

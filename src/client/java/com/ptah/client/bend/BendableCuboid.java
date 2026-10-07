package com.ptah.client.bend;

import com.ptah.client.compat.MathCompat;
import com.ptah.client.compat.VertexCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BendableCuboid {
    private final Quad[] sides;
    private final RememberingPos[] positions;

    private final BendDir direction;

    private final float smoothMinY;
    private final float smoothMaxY;
    private final float smoothCenterX;
    private final float smoothCenterZ;

    private static final int SMOOTH_CENTERLINE_SAMPLES = 512;

    private static final float SMOOTH_WINDOW = 0.28f;

    private BendableCuboid(Quad[] sides, RememberingPos[] positions, BendDir direction,
                           float smoothMinY, float smoothMaxY,
                           float smoothCenterX, float smoothCenterZ) {
        this.sides = sides;
        this.positions = positions;
        this.direction = direction;

        this.smoothMinY = smoothMinY;
        this.smoothMaxY = smoothMaxY;
        this.smoothCenterX = smoothCenterX;
        this.smoothCenterZ = smoothCenterZ;

        resetPositions();
    }

    private void resetPositions() {
        for (RememberingPos pos : positions) {
            pos.set(pos.original());
        }
    }

    public void applyBendSmooth(float bendAxis, float bendValue) {
        float theta = bendValue;
        float length = smoothMaxY - smoothMinY;
        if (Math.abs(theta) < 1.0e-5f || length < 1.0e-6f) {
            resetPositions();
            return;
        }

        Vector3f axis = new Vector3f((float) Math.cos(bendAxis), 0f, (float) Math.sin(bendAxis));
        axis.rotate(direction.rotation);
        if (axis.lengthSquared() > 1.0e-12f) {
            axis.normalize();
        }

        boolean freeIsMax = direction.inverted;
        float freeEndY = freeIsMax ? smoothMaxY : smoothMinY;
        float nearEndY = freeIsMax ? smoothMinY : smoothMaxY;
        float span = freeEndY - nearEndY;

        final int samples = SMOOTH_CENTERLINE_SAMPLES;
        float[] centerline = centerline(axis, smoothCenterX, smoothCenterZ, nearEndY, span, theta, samples);

        Quaternionf vertexRot = new Quaternionf();
        Vector3f offset = new Vector3f();
        float[] center = new float[3];
        for (RememberingPos pos : positions) {
            Vector3f o = pos.original();
            float angle = phi(o.y, nearEndY, span, theta);
            vertexRot.identity().rotateAxis(angle, axis.x, axis.y, axis.z);
            offset.set(o.x - smoothCenterX, 0f, o.z - smoothCenterZ).rotate(vertexRot);
            sampleCenterline(centerline, samples, o.y, nearEndY, span, center);
            pos.set(new Vector3f(center[0] + offset.x, center[1] + offset.y, center[2] + offset.z));
        }
    }

    private static float[] centerline(Vector3f axis, float centerX, float centerZ, float nearEndY, float span,
                                      float theta, int samples) {
        float sign = span > 0f ? 1f : -1f;
        float[] centerline = new float[(samples + 1) * 3];
        float cx = centerX, cy = nearEndY, cz = centerZ;
        centerline[0] = cx;
        centerline[1] = cy;
        centerline[2] = cz;
        float dy = span / samples;
        float stepLen = Math.abs(dy);
        Quaternionf tangentRot = new Quaternionf();
        Vector3f tangent = new Vector3f();
        for (int i = 0; i < samples; i++) {
            float yMid = nearEndY + dy * (i + 0.5f);
            float angle = phi(yMid, nearEndY, span, theta);
            tangentRot.identity().rotateAxis(angle, axis.x, axis.y, axis.z);
            tangent.set(0f, sign, 0f).rotate(tangentRot);
            cx += tangent.x * stepLen;
            cy += tangent.y * stepLen;
            cz += tangent.z * stepLen;
            int base = (i + 1) * 3;
            centerline[base] = cx;
            centerline[base + 1] = cy;
            centerline[base + 2] = cz;
        }
        return centerline;
    }

    public static Matrix4f freeEndTransform(BendDir direction, float minY, float maxY, float centerX, float centerZ,
                                            float bendAxis, float bendValue) {
        float length = maxY - minY;
        if (Math.abs(bendValue) < 1.0e-5f || length < 1.0e-6f) return null;
        Vector3f axis = new Vector3f((float) Math.cos(bendAxis), 0f, (float) Math.sin(bendAxis));
        axis.rotate(direction.rotation);
        if (axis.lengthSquared() > 1.0e-12f) {
            axis.normalize();
        }
        float freeEndY = direction.inverted ? maxY : minY;
        float nearEndY = direction.inverted ? minY : maxY;
        float span = freeEndY - nearEndY;
        int samples = SMOOTH_CENTERLINE_SAMPLES;
        float[] line = centerline(axis, centerX, centerZ, nearEndY, span, bendValue, samples);
        int end = samples * 3;
        return new Matrix4f()
                .translate(line[end], line[end + 1], line[end + 2])
                .rotate(bendValue, axis.x, axis.y, axis.z)
                .translate(-centerX, -freeEndY, -centerZ);
    }

    private static float phi(float y, float nearEndY, float span, float theta) {
        float t = (y - nearEndY) / span;
        if (t < 0f) t = 0f; else if (t > 1f) t = 1f;
        return theta * smoothStep(t);
    }

    private static float smoothStep(float t) {
        float u = (t - (0.5f - SMOOTH_WINDOW)) / (2f * SMOOTH_WINDOW);
        if (u < 0f) u = 0f; else if (u > 1f) u = 1f;
        return u * u * (3f - 2f * u);
    }

    private static void sampleCenterline(float[] centerline, int samples, float y,
                                         float nearEndY, float span, float[] out) {
        float f = (y - nearEndY) / span * samples;
        if (f < 0f) f = 0f; else if (f > samples) f = samples;
        int i0 = (int) Math.floor(f);
        float fr = f - i0;
        int i1 = Math.min(i0 + 1, samples);
        int b0 = i0 * 3, b1 = i1 * 3;
        out[0] = centerline[b0] + (centerline[b1] - centerline[b0]) * fr;
        out[1] = centerline[b0 + 1] + (centerline[b1 + 1] - centerline[b0 + 1]) * fr;
        out[2] = centerline[b0 + 2] + (centerline[b1 + 2] - centerline[b0 + 2]) * fr;
    }

    public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        for (Quad quad : sides) {
            quad.render(pose, consumer, light, overlay, red, green, blue, alpha);
        }
    }

    public static BendableCuboid build(CubeData data, BendDir direction) {
        List<Quad> planes = new ArrayList<>();
        Map<Vector3f, RememberingPos> positions = new HashMap<>();

        float oX = data.x + data.growX;
        float oY = data.y + data.growY;
        float oZ = data.z + data.growZ;

        float minX = oX;
        float minY = oY;
        float minZ = oZ;
        float maxX = oX + data.sizeX;
        float maxY = oY + data.sizeY;
        float maxZ = oZ + data.sizeZ;

        float pminX = minX - data.growX;
        float pminY = minY - data.growY;
        float pminZ = minZ - data.growZ;
        float pmaxX = maxX + data.growX;
        float pmaxY = maxY + data.growY;
        float pmaxZ = maxZ + data.growZ;
        if (data.mirror) {
            float tmp = pminX;
            pminX = pmaxX;
            pmaxX = tmp;
        }

        Vector3f vertex1 = new Vector3f(pminX, pminY, pminZ);
        Vector3f vertex2 = new Vector3f(pmaxX, pminY, pminZ);
        Vector3f vertex3 = new Vector3f(pmaxX, pmaxY, pminZ);
        Vector3f vertex4 = new Vector3f(pminX, pmaxY, pminZ);
        Vector3f vertex5 = new Vector3f(pminX, pminY, pmaxZ);
        Vector3f vertex6 = new Vector3f(pmaxX, pminY, pmaxZ);
        Vector3f vertex7 = new Vector3f(pmaxX, pmaxY, pmaxZ);
        Vector3f vertex8 = new Vector3f(pminX, pmaxY, pmaxZ);

        int j = data.u;
        int k = (int) (data.u + data.sizeZ);
        int l = (int) (data.u + data.sizeZ + data.sizeX);
        int m = (int) (data.u + data.sizeZ + data.sizeX + data.sizeX);
        int n = (int) (data.u + data.sizeZ + data.sizeX + data.sizeZ);
        int o = (int) (data.u + data.sizeZ + data.sizeX + data.sizeZ + data.sizeX);
        int p = data.v;
        int q = (int) (data.v + data.sizeZ);
        int r = (int) (data.v + data.sizeZ + data.sizeY);

        createAndAddQuads(planes, positions, new Vector3f[]{vertex6, vertex5, vertex2}, k, p, l, q, data);
        createAndAddQuads(planes, positions, new Vector3f[]{vertex3, vertex4, vertex7}, l, q, m, p, data);
        createAndAddQuads(planes, positions, new Vector3f[]{vertex1, vertex5, vertex4}, j, q, k, r, data);
        createAndAddQuads(planes, positions, new Vector3f[]{vertex2, vertex1, vertex3}, k, q, l, r, data);
        createAndAddQuads(planes, positions, new Vector3f[]{vertex6, vertex2, vertex7}, l, q, n, r, data);
        createAndAddQuads(planes, positions, new Vector3f[]{vertex5, vertex6, vertex8}, n, q, o, r, data);

        float baseMinX = minX, baseMaxX = maxX;
        if (data.mirror) {
            baseMinX = maxX;
            baseMaxX = minX;
        }
        return new BendableCuboid(planes.toArray(new Quad[0]),
                positions.values().toArray(new RememberingPos[0]), direction,
                minY, maxY, (baseMinX + baseMaxX) / 2f, (minZ + maxZ) / 2f);
    }

    private static void createAndAddQuads(List<Quad> quads, Map<Vector3f, RememberingPos> positions,
                                          Vector3f[] edges, int u1, int v1, int u2, int v2, CubeData data) {
        int du = u2 < u1 ? 1 : -1;
        int dv = v1 < v2 ? 1 : -1;
        for (int localU = u2; localU != u1; localU += du) {
            for (int localV = v1; localV != v2; localV += dv) {
                int localU2 = localU + du;
                int localV2 = localV + dv;
                RememberingPos rp0 = getOrCreate(positions, transformVector(new Vector3f(edges[0]), new Vector3f(edges[1]), new Vector3f(edges[2]), u2, v1, u1, v2, localU2, localV));
                RememberingPos rp1 = getOrCreate(positions, transformVector(new Vector3f(edges[0]), new Vector3f(edges[1]), new Vector3f(edges[2]), u2, v1, u1, v2, localU2, localV2));
                RememberingPos rp2 = getOrCreate(positions, transformVector(new Vector3f(edges[0]), new Vector3f(edges[1]), new Vector3f(edges[2]), u2, v1, u1, v2, localU, localV2));
                RememberingPos rp3 = getOrCreate(positions, transformVector(new Vector3f(edges[0]), new Vector3f(edges[1]), new Vector3f(edges[2]), u2, v1, u1, v2, localU, localV));
                quads.add(new Quad(new RememberingPos[]{rp3, rp0, rp1, rp2}, localU2, localV, localU, localV2, data.textureWidth, data.textureHeight, data.mirror));
            }
        }
    }

    private static Vector3f transformVector(Vector3f pos, Vector3f vectorU, Vector3f vectorV,
                                            int u1, int v1, int u2, int v2, int u, int v) {
        vectorU.sub(pos);
        vectorU.mul(((float) u - u1) / (u2 - u1));
        vectorV.sub(pos);
        vectorV.mul(((float) v - v1) / (v2 - v1));
        pos.add(vectorU);
        pos.add(vectorV);
        return pos;
    }

    private static RememberingPos getOrCreate(Map<Vector3f, RememberingPos> positions, Vector3f pos) {
        return positions.computeIfAbsent(pos, RememberingPos::new);
    }

    private static final class RememberingPos {
        private final Vector3f origin;
        private Vector3f current;

        RememberingPos(Vector3f origin) {
            this.origin = new Vector3f(origin);
        }

        Vector3f original() {
            return new Vector3f(origin);
        }

        Vector3f current() {
            return current;
        }

        void set(Vector3f value) {
            this.current = value;
        }
    }

    private static final class Vertex {
        final float u;
        final float v;
        final RememberingPos pos;

        Vertex(float u, float v, RememberingPos pos) {
            this.u = u;
            this.v = v;
            this.pos = pos;
        }
    }

    private static final class Quad {
        final Vertex[] vertices;

        Quad(RememberingPos[] corners, float u1, float v1, float u2, float v2,
             float squishU, float squishV, boolean flip) {
            this.vertices = new Vertex[4];
            this.vertices[0] = new Vertex(u2 / squishU, v1 / squishV, corners[0]);
            this.vertices[1] = new Vertex(u1 / squishU, v1 / squishV, corners[1]);
            this.vertices[2] = new Vertex(u1 / squishU, v2 / squishV, corners[2]);
            this.vertices[3] = new Vertex(u2 / squishU, v2 / squishV, corners[3]);
            if (flip) {
                for (int i = 0; i < this.vertices.length / 2; i++) {
                    Vertex tmp = this.vertices[i];
                    this.vertices[i] = this.vertices[this.vertices.length - 1 - i];
                    this.vertices[this.vertices.length - 1 - i] = tmp;
                }
            }
        }

        void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                    float red, float green, float blue, float alpha) {
            Matrix4f positionMatrix = MathCompat.pose(pose);
            Matrix3f normalMatrix = MathCompat.normal(pose);

            Vector3f normal = direction();
            normalMatrix.transform(normal);

            for (int i = 0; i < 4; i++) {
                Vertex vertex = vertices[i];
                Vector3f local = vertex.pos.current();
                Vector3f world = positionMatrix.transformPosition(
                        new Vector3f(local.x / 16f, local.y / 16f, local.z / 16f));
                VertexCompat.raw(consumer, world.x, world.y, world.z, red, green, blue, alpha,
                        vertex.u, vertex.v, overlay, light, normal.x, normal.y, normal.z);
            }
        }

        private Vector3f direction() {
            Vector3f vecA = new Vector3f(vertices[0].pos.current()).sub(vertices[2].pos.current());
            Vector3f vecB = new Vector3f(vertices[1].pos.current()).sub(vertices[3].pos.current());
            vecA.cross(vecB);
            if (vecA.lengthSquared() < 1.0e-6f) {
                return new Vector3f(0f, 0f, 1f);
            }
            return vecA.normalize();
        }
    }
}

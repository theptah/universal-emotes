package com.ptah.client.render;

import com.ptah.client.compat.VertexCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ptah.UniversalEmotesMod;
import com.ptah.animation.AnimationClip;
import com.ptah.animation.BoneAnimation;
import com.ptah.animation.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class R15Model {
    private static R15Model wide;
    private static R15Model slim;

    public static R15Model get(boolean slimModel) {
        if (slimModel) {
            if (slim == null) slim = load("/assets/universal-emotes/rigs/r15_rig_slim.json");
            return slim;
        }
        if (wide == null) wide = load("/assets/universal-emotes/rigs/r15_rig.json");
        return wide;
    }

    private static final float TEX_W = 64f;
    private static final float TEX_H = 64f;
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    public static final Matrix4f BB_TO_MODEL = new Matrix4f().translation(0f, 24f, 0f).scale(-1f, -1f, 1f);

    private static final class Cube {
        float[] origin;
        float[] size;
        float inflate;
        final Map<String, float[]> uv = new LinkedHashMap<>();
    }

    private static final class Bone {
        String name;
        String parent;
        float[] pivot;
        final List<Cube> cubes = new ArrayList<>();
    }

    private final List<Bone> bones;
    private R15Model(List<Bone> bones) { this.bones = bones; }

    private static R15Model load(String resourcePath) {
        List<Bone> out = new ArrayList<>();
        try (InputStream in = R15Model.class.getResourceAsStream(resourcePath)) {
            if (in == null) { UniversalEmotesMod.LOGGER.error("R15 rig missing: {}", resourcePath); return new R15Model(out); }
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement be : arr) {
                JsonObject b = be.getAsJsonObject();
                Bone bone = new Bone();
                bone.name = b.get("name").getAsString();
                bone.parent = (b.has("parent") && !b.get("parent").isJsonNull()) ? b.get("parent").getAsString() : null;
                bone.pivot = arr3(b.getAsJsonArray("pivot"));
                if (b.has("cubes")) {
                    for (JsonElement ce : b.getAsJsonArray("cubes")) {
                        JsonObject c = ce.getAsJsonObject();
                        Cube cube = new Cube();
                        cube.origin = arr3(c.getAsJsonArray("origin"));
                        cube.size = arr3(c.getAsJsonArray("size"));
                        cube.inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
                        JsonObject uv = c.getAsJsonObject("uv");
                        for (String f : FACES) {
                            if (uv == null || !uv.has(f)) continue;
                            JsonArray fa = uv.getAsJsonArray(f);
                            JsonArray uvp = fa.get(0).getAsJsonArray();
                            JsonArray whp = fa.get(1).getAsJsonArray();
                            cube.uv.put(f, new float[]{uvp.get(0).getAsFloat(), uvp.get(1).getAsFloat(),
                                    whp.get(0).getAsFloat(), whp.get(1).getAsFloat()});
                        }
                        bone.cubes.add(cube);
                    }
                }
                out.add(bone);
            }
        } catch (Exception e) {
            UniversalEmotesMod.LOGGER.error("Failed to load R15 rig {}", resourcePath, e);
        }
        return new R15Model(out);
    }

    public void render(AnimationClip clip, float time, boolean hideHead, PoseStack ps, VertexConsumer vc,
                       int light, int overlay, float red, float green, float blue, float alpha) {
        if (bones.isEmpty() || clip == null) return;
        Map<String, Matrix4f> world = buildWorld(clip, time);

        PoseStack.Pose pose = ps.last();
        for (Bone bone : bones) {
            if (bone.cubes.isEmpty()) continue;

            if (hideHead && "Head".equals(bone.name)) continue;
            Matrix4f mm = new Matrix4f(BB_TO_MODEL).mul(world.get(bone.name));
            for (Cube c : bone.cubes) {
                float x0 = c.origin[0] - c.inflate, y0 = c.origin[1] - c.inflate, z0 = c.origin[2] - c.inflate;
                float x1 = c.origin[0] + c.size[0] + c.inflate, y1 = c.origin[1] + c.size[1] + c.inflate, z1 = c.origin[2] + c.size[2] + c.inflate;
                for (Map.Entry<String, float[]> e : c.uv.entrySet()) {
                    float[] uv = e.getValue();
                    float u1 = uv[0] / TEX_W, v1 = uv[1] / TEX_H, u2 = (uv[0] + uv[2]) / TEX_W, v2 = (uv[1] + uv[3]) / TEX_H;
                    float[][] corners;
                    float[] n;
                    switch (e.getKey()) {
                        case "north": corners = new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}}; n = new float[]{0, 0, -1}; break;
                        case "south": corners = new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}}; n = new float[]{0, 0, 1}; break;
                        case "east":  corners = new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}}; n = new float[]{1, 0, 0}; break;
                        case "west":  corners = new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}}; n = new float[]{-1, 0, 0}; break;
                        case "up":    corners = new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}; n = new float[]{0, 1, 0}; break;
                        case "down":  corners = new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}; n = new float[]{0, -1, 0}; break;
                        default: continue;
                    }
                    float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
                    Vector4f nn = new Vector4f(n[0], n[1], n[2], 0f).mul(mm);
                    float nl = (float) Math.sqrt(nn.x * nn.x + nn.y * nn.y + nn.z * nn.z);
                    float nx = nl > 1e-5f ? nn.x / nl : n[0];
                    float ny = nl > 1e-5f ? nn.y / nl : n[1];
                    float nz = nl > 1e-5f ? nn.z / nl : n[2];
                    for (int i = 0; i < 4; i++) {
                        Vector4f p = new Vector4f(corners[i][0], corners[i][1], corners[i][2], 1f).mul(mm);
                        VertexCompat.entity(vc, pose, p.x / 16f, p.y / 16f, p.z / 16f,
                                red, green, blue, alpha, uvs[i][0], uvs[i][1], overlay, light, nx, ny, nz);
                    }
                }
            }
        }
    }

    private Map<String, Matrix4f> buildWorld(AnimationClip clip, float time) {
        Map<String, Matrix4f> world = new LinkedHashMap<>();
        for (Bone bone : bones) {
            Matrix4f m = (bone.parent != null && world.containsKey(bone.parent))
                    ? new Matrix4f(world.get(bone.parent)) : new Matrix4f();
            m.translate(bone.pivot[0], bone.pivot[1], bone.pivot[2]);
            BoneAnimation anim = clip.bones().get(bone.name);
            if (anim != null) {
                if (anim.position() != null && !anim.position().keyframes().isEmpty()) {
                    Vec3 p = anim.position().sample(time);

                    m.translate(-p.x(), p.y(), p.z());
                }
                if (anim.rotation() != null && !anim.rotation().keyframes().isEmpty()) {
                    Vec3 r = anim.rotation().sample(time);
                    m.rotateZ((float) Math.toRadians(r.z()));
                    m.rotateY((float) Math.toRadians(-r.y()));
                    m.rotateX((float) Math.toRadians(-r.x()));
                }
            }
            m.translate(-bone.pivot[0], -bone.pivot[1], -bone.pivot[2]);
            world.put(bone.name, m);
        }
        return world;
    }

    public Map<String, Matrix4f> computeModelMatrices(AnimationClip clip, float time) {
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        if (bones.isEmpty() || clip == null) return out;
        Map<String, Matrix4f> world = buildWorld(clip, time);
        for (Bone bone : bones) {
            out.put(bone.name, new Matrix4f(BB_TO_MODEL).mul(world.get(bone.name)));
        }
        return out;
    }

    public Map<String, Matrix4f> boneFrames(AnimationClip clip, float time) {
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        if (bones.isEmpty() || clip == null) return out;
        Map<String, Matrix4f> world = buildWorld(clip, time);
        for (Bone bone : bones) {
            out.put(bone.name, new Matrix4f(world.get(bone.name)).translate(bone.pivot[0], bone.pivot[1], bone.pivot[2]));
        }
        return out;
    }

    public Vector3f pivotOf(String name) {
        for (Bone b : bones) {
            if (b.name.equals(name)) return new Vector3f(b.pivot[0], b.pivot[1], b.pivot[2]);
        }
        return null;
    }

    public Vector3f restModelPos(String name) {
        Vector3f piv = pivotOf(name);
        if (piv == null) return null;
        return BB_TO_MODEL.transformPosition(piv);
    }

    public Matrix4f capeFollowMatrix(AnimationClip clip, float time) {
        if (bones.isEmpty() || clip == null) return null;
        Matrix4f w = buildWorld(clip, time).get("UpperTorso");
        if (w == null) return null;
        Matrix4f cInv = new Matrix4f(BB_TO_MODEL).invert();
        Matrix4f delta = new Matrix4f(BB_TO_MODEL).mul(w).mul(cInv);

        delta.m30(delta.m30() / 16f);
        delta.m31(delta.m31() / 16f);
        delta.m32(delta.m32() / 16f);
        return delta;
    }

    private static float[] arr3(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}

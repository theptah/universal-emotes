package com.ptah.client.render;

import com.ptah.client.compat.VertexCompat;
import com.ptah.bundle.ModelTextures;
import com.ptah.compat.Ids;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BundleModelGeometry {
    public static final class Quad {
        public final float[] pos = new float[12];
        public final float[] uv = new float[8];
        public final float[] normal = new float[3];
        public final ResourceLocation texture;
        Quad(ResourceLocation texture) { this.texture = texture; }
    }

    public final List<Quad> quads;
    private BundleModelGeometry(List<Quad> quads) {
        List<Quad> sorted = new ArrayList<>(quads);
        sorted.sort(java.util.Comparator.comparing(q -> q.texture.toString()));
        this.quads = List.copyOf(sorted);
    }
    public boolean isEmpty() { return quads.isEmpty(); }

    public void draw(net.minecraft.client.renderer.MultiBufferSource buffers, com.mojang.blaze3d.vertex.PoseStack.Pose pose) {
        ResourceLocation bound = null;
        com.mojang.blaze3d.vertex.VertexConsumer vc = null;
        for (Quad q : quads) {
            if (!q.texture.equals(bound)) {
                bound = q.texture;
                vc = buffers.getBuffer(com.ptah.client.compat.ClientCompat.entityCutoutNoCull(q.texture));
            }
            for (int i = 0; i < 4; i++) {
                VertexCompat.entity(vc, pose, q.pos[i * 3], q.pos[i * 3 + 1], q.pos[i * 3 + 2],
                        255, 255, 255, 255, q.uv[i * 2], q.uv[i * 2 + 1],
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                        q.normal[0], q.normal[1], q.normal[2]);
            }
        }
    }

    private static final Map<String, BundleModelGeometry> CACHE = new ConcurrentHashMap<>();

    public static BundleModelGeometry load(ResourceLocation asset, String textureOverride) {
        if (asset == null) return null;
        String key = asset + "|" + (textureOverride == null ? "" : textureOverride);
        BundleModelGeometry cached = CACHE.get(key);
        if (cached != null) return cached;
        ResourceLocation file = Ids.of(asset.getNamespace(), "models/" + asset.getPath() + ".json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) { warnOnce(key, "render_model: model file not found: " + file); return null; }
            try (var input = resource.get().open()) {
                String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                BundleModelGeometry geo = parse(json, asset.getNamespace(), asset.getPath(), textureOverride);
                if (geo == null || geo.isEmpty()) {
                    warnOnce(key, "render_model: " + file + " has no renderable geometry (cubes / poly_mesh / elements)");
                    return null;
                }
                CACHE.put(key, geo);
                return geo;
            }
        } catch (Exception exception) {
            warnOnce(key, "render_model: could not read " + file + ": " + exception);
            return null;
        }
    }

    private static final java.util.Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) com.ptah.UniversalEmotesMod.LOGGER.warn(message);
    }

    public static void clearCache() { CACHE.clear(); WARNED.clear(); }

    private static ResourceLocation tex(String ns, String ref) {
        if (ref == null || ref.isBlank()) return null;
        ResourceLocation first = null;
        for (String path : ModelTextures.candidates(ref)) {
            ResourceLocation candidate;
            try { candidate = Ids.of(ns, "textures/" + path + ".png"); } catch (Exception e) { continue; }
            if (first == null) first = candidate;
            if (exists(candidate)) return candidate;
        }
        if (ModelTextures.vanilla(ref)) {
            String path = ModelTextures.normalize(ref);
            try {
                ResourceLocation vanilla = Ids.of("minecraft", "textures/" + path + ".png");
                if (exists(vanilla)) return vanilla;
            } catch (Exception ignored) { }
        }
        if (first != null) warnOnce("texture|" + ns + "|" + ref, "render_model: texture not found: " + ref + " (looked for " + first + ")");
        return first;
    }

    private static boolean exists(ResourceLocation location) {
        try { return Minecraft.getInstance().getResourceManager().getResource(location).isPresent(); }
        catch (Exception e) { return false; }
    }

    public static BundleModelGeometry parse(String json, String namespace, String modelPath, String textureOverride) {
        JsonObject data;
        try { data = JsonParser.parseString(json).getAsJsonObject(); } catch (Exception e) { return null; }
        ResourceLocation baseTex = tex(namespace,
                (textureOverride != null && !textureOverride.isBlank()) ? textureOverride : modelPath);
        List<Quad> out = new ArrayList<>();
        try {
            if (data.has("minecraft:geometry") && data.get("minecraft:geometry").isJsonArray()) {
                JsonArray geos = data.getAsJsonArray("minecraft:geometry");
                if (geos.size() > 0) {
                    JsonObject g = geos.get(0).getAsJsonObject();
                    JsonObject d = g.has("description") ? g.getAsJsonObject("description") : new JsonObject();
                    float tw = num(d, "texture_width", 16), th = num(d, "texture_height", 16);
                    buildBedrock(out, g.has("bones") ? g.getAsJsonArray("bones") : new JsonArray(), tw, th, baseTex, namespace);
                    return new BundleModelGeometry(out);
                }
            }

            for (Map.Entry<String, JsonElement> e : data.entrySet()) {
                if (e.getKey().startsWith("geometry.") && e.getValue().isJsonObject()) {
                    JsonObject g = e.getValue().getAsJsonObject();
                    if (g.has("bones")) {
                        float tw = num(g, "texturewidth", 16), th = num(g, "textureheight", 16);
                        buildBedrock(out, g.getAsJsonArray("bones"), tw, th, baseTex, namespace);
                        return new BundleModelGeometry(out);
                    }
                }
            }

            if (data.has("elements") && data.get("elements").isJsonArray()) {
                Map<String, String> textures = new HashMap<>();
                if (data.has("textures") && data.get("textures").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> t : data.getAsJsonObject("textures").entrySet()) {
                        if (t.getValue().isJsonPrimitive()) textures.put(t.getKey(), t.getValue().getAsString());
                    }
                }
                buildJava(out, data.getAsJsonArray("elements"), textures, namespace, baseTex);
                return new BundleModelGeometry(out);
            }
        } catch (Exception e) {
            return out.isEmpty() ? null : new BundleModelGeometry(out);
        }
        return null;
    }

    private static void buildBedrock(List<Quad> out, JsonArray bones, float tw, float th, ResourceLocation baseTex, String namespace) {
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        for (JsonElement be : bones) {
            JsonObject b = be.getAsJsonObject();
            if (b.has("name")) byName.put(b.get("name").getAsString(), b);
        }
        Map<String, Matrix4f> mats = new HashMap<>();
        for (String name : byName.keySet()) resolveBone(name, byName, mats);
        for (Map.Entry<String, JsonObject> e : byName.entrySet()) {
            JsonObject b = e.getValue();
            Matrix4f m = mats.getOrDefault(e.getKey(), new Matrix4f());

            ResourceLocation boneTex = b.has("texture") && b.get("texture").isJsonPrimitive()
                    ? tex(namespace, b.get("texture").getAsString()) : baseTex;
            if (b.has("poly_mesh") && b.get("poly_mesh").isJsonObject()) {
                addPolyMesh(out, b.getAsJsonObject("poly_mesh"), tw, th, boneTex, m);
            }
            if (!b.has("cubes")) continue;
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                float[] origin = arr3(c, "origin", 0, 0, 0);
                float[] size = arr3(c, "size", 0, 0, 0);
                float inflate = num(c, "inflate", 0);
                boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
                float x0 = origin[0] - inflate, y0 = origin[1] - inflate, z0 = origin[2] - inflate;
                float x1 = origin[0] + size[0] + inflate, y1 = origin[1] + size[1] + inflate, z1 = origin[2] + size[2] + inflate;
                float w = Math.abs(size[0]), h = Math.abs(size[1]), dd = Math.abs(size[2]);

                JsonElement uvEl = c.get("uv");
                if (uvEl != null && uvEl.isJsonObject()) {
                    JsonObject uv = uvEl.getAsJsonObject();
                    for (String fn : FACE_NAMES) {
                        if (!uv.has(fn)) continue;
                        JsonObject f = uv.getAsJsonObject(fn);
                        if (!f.has("uv")) continue;
                        float[] o = arr(f, "uv", 2);
                        float[] s = f.has("uv_size") ? arr(f, "uv_size", 2) : new float[]{0, 0};
                        addFace(out, boneTex, fn, x0, y0, z0, x1, y1, z1,
                                o[0], o[1], o[0] + s[0], o[1] + s[1], tw, th, mirror, m);
                    }
                } else if (uvEl != null && uvEl.isJsonArray()) {
                    float[] uv = arr(uvEl.getAsJsonArray(), 2);
                    float U = uv[0], V = uv[1];
                    addFace(out, boneTex, "up",    x0, y0, z0, x1, y1, z1, U + dd,          V,      U + dd + w,      V + dd, tw, th, mirror, m);
                    addFace(out, boneTex, "down",  x0, y0, z0, x1, y1, z1, U + dd + w,      V + dd, U + dd + 2 * w,  V,      tw, th, mirror, m);
                    addFace(out, boneTex, "east",  x0, y0, z0, x1, y1, z1, U,              V + dd, U + dd,          V + dd + h, tw, th, mirror, m);
                    addFace(out, boneTex, "north", x0, y0, z0, x1, y1, z1, U + dd,          V + dd, U + dd + w,      V + dd + h, tw, th, mirror, m);
                    addFace(out, boneTex, "west",  x0, y0, z0, x1, y1, z1, U + dd + w,      V + dd, U + 2 * dd + w,  V + dd + h, tw, th, mirror, m);
                    addFace(out, boneTex, "south", x0, y0, z0, x1, y1, z1, U + 2 * dd + w,  V + dd, U + 2 * dd + 2 * w, V + dd + h, tw, th, mirror, m);
                }
            }
        }
    }

    private static void addPolyMesh(List<Quad> out, JsonObject pm, float tw, float th, ResourceLocation texture, Matrix4f m) {
        if (texture == null || !pm.has("positions") || !pm.has("polys") || !pm.get("polys").isJsonArray()) return;
        JsonArray positions = pm.getAsJsonArray("positions");
        JsonArray uvs = pm.has("uvs") && pm.get("uvs").isJsonArray() ? pm.getAsJsonArray("uvs") : new JsonArray();
        boolean normalized = !pm.has("normalized_uvs") || pm.get("normalized_uvs").getAsBoolean();
        float su = normalized ? 1f : 1f / tw, sv = normalized ? 1f : 1f / th;
        for (JsonElement pe : pm.getAsJsonArray("polys")) {
            if (!pe.isJsonArray()) continue;
            JsonArray poly = pe.getAsJsonArray();
            int count = Math.min(4, poly.size());
            if (count < 3) continue;
            Quad q = new Quad(texture);
            Vector4f[] world = new Vector4f[4];
            for (int i = 0; i < 4; i++) {
                JsonArray corner = poly.get(Math.min(i, count - 1)).getAsJsonArray();
                int pi = corner.get(0).getAsInt();
                int ui = corner.size() > 2 ? corner.get(2).getAsInt() : -1;
                if (pi < 0 || pi >= positions.size()) return;
                float[] p = arr(positions.get(pi).getAsJsonArray(), 3);
                Vector4f v = new Vector4f(p[0], p[1], p[2], 1.0f).mul(m);
                world[i] = v;
                q.pos[i * 3] = v.x / 16.0f;
                q.pos[i * 3 + 1] = v.y / 16.0f;
                q.pos[i * 3 + 2] = v.z / 16.0f;
                if (ui >= 0 && ui < uvs.size()) {
                    float[] uv = arr(uvs.get(ui).getAsJsonArray(), 2);
                    q.uv[i * 2] = uv[0] * su;
                    q.uv[i * 2 + 1] = 1.0f - uv[1] * sv;
                }
            }

            float ax = world[1].x - world[0].x, ay = world[1].y - world[0].y, az = world[1].z - world[0].z;
            float bx = world[2].x - world[0].x, by = world[2].y - world[0].y, bz = world[2].z - world[0].z;
            float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-9f) { q.normal[0] = nx / len; q.normal[1] = ny / len; q.normal[2] = nz / len; }
            else { q.normal[1] = 1f; }
            out.add(q);
        }
    }

    private static Matrix4f resolveBone(String name, Map<String, JsonObject> byName, Map<String, Matrix4f> mats) {
        Matrix4f cached = mats.get(name);
        if (cached != null) return cached;
        JsonObject b = byName.get(name);
        Matrix4f m = new Matrix4f();
        if (b == null) { mats.put(name, m); return m; }
        if (b.has("parent")) {
            String parent = b.get("parent").getAsString();
            if (byName.containsKey(parent) && !parent.equals(name)) m.set(resolveBone(parent, byName, mats));
        }
        float[] pivot = arr3(b, "pivot", 0, 0, 0);
        m.translate(pivot[0], pivot[1], pivot[2]);
        if (b.has("rotation")) {
            float[] r = arr3(b, "rotation", 0, 0, 0);

            m.rotateZ((float) Math.toRadians(r[2]));
            m.rotateY((float) Math.toRadians(-r[1]));
            m.rotateX((float) Math.toRadians(-r[0]));
        }
        m.translate(-pivot[0], -pivot[1], -pivot[2]);
        mats.put(name, m);
        return m;
    }

    private static void buildJava(List<Quad> out, JsonArray elements, Map<String, String> textures, String ns, ResourceLocation baseTex) {
        for (JsonElement ee : elements) {
            JsonObject el = ee.getAsJsonObject();
            if (!el.has("from") || !el.has("to") || !el.has("faces")) continue;
            float[] from = arr(el, "from", 3), to = arr(el, "to", 3);
            float x0 = Math.min(from[0], to[0]), y0 = Math.min(from[1], to[1]), z0 = Math.min(from[2], to[2]);
            float x1 = Math.max(from[0], to[0]), y1 = Math.max(from[1], to[1]), z1 = Math.max(from[2], to[2]);
            Matrix4f m = new Matrix4f();
            if (el.has("rotation") && el.get("rotation").isJsonObject()) {
                JsonObject rot = el.getAsJsonObject("rotation");
                float[] o = arr3(rot, "origin", 0, 0, 0);
                float angle = num(rot, "angle", 0);
                String axis = rot.has("axis") ? rot.get("axis").getAsString() : "y";
                m.translate(o[0], o[1], o[2]);
                switch (axis) {
                    case "x" -> m.rotateX((float) Math.toRadians(angle));
                    case "z" -> m.rotateZ((float) Math.toRadians(angle));
                    default -> m.rotateY((float) Math.toRadians(angle));
                }
                m.translate(-o[0], -o[1], -o[2]);
            }
            JsonObject faces = el.getAsJsonObject("faces");
            for (String fn : FACE_NAMES) {
                if (!faces.has(fn)) continue;
                JsonObject f = faces.getAsJsonObject(fn);
                float[] uv = f.has("uv") ? arr(f, "uv", 4) : defaultUv(fn, x0, y0, z0, x1, y1, z1);
                ResourceLocation faceTex = baseTex;
                if (f.has("texture")) {
                    String slot = f.get("texture").getAsString();
                    ResourceLocation resolved = resolveSlot(slot, textures, ns);
                    if (resolved != null) faceTex = resolved;
                }

                addFace(out, faceTex, fn, x0, y0, z0, x1, y1, z1, uv[0], uv[1], uv[2], uv[3], 16, 16, false, m);
            }
        }
    }

    private static ResourceLocation resolveSlot(String ref, Map<String, String> textures, String ns) {
        String value = ModelTextures.resolveSlot(ref, textures);
        return value == null ? null : tex(ns, value);
    }

    private static float[] defaultUv(String fn, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (fn) {
            case "up", "down" -> new float[]{x0, z0, x1, z1};
            case "north", "south" -> new float[]{x0, 16 - y1, x1, 16 - y0};
            default -> new float[]{z0, 16 - y1, z1, 16 - y0};
        };
    }

    private static final String[] FACE_NAMES = {"north", "south", "east", "west", "up", "down"};

    private static void addFace(List<Quad> out, ResourceLocation texture, String face,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                float x1p, float y1p, float x2p, float y2p, float tw, float th,
                                boolean mirror, Matrix4f m) {
        if (texture == null) return;
        float[][] c; float[] n;
        switch (face) {
            case "north" -> { c = new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}}; n = new float[]{0, 0, -1}; }
            case "south" -> { c = new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}}; n = new float[]{0, 0, 1}; }
            case "east"  -> { c = new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}}; n = new float[]{1, 0, 0}; }
            case "west"  -> { c = new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}}; n = new float[]{-1, 0, 0}; }
            case "up"    -> { c = new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}; n = new float[]{0, 1, 0}; }
            case "down"  -> { c = new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}; n = new float[]{0, -1, 0}; }
            default -> { return; }
        }
        float u1 = x1p / tw, v1 = y1p / th, u2 = x2p / tw, v2 = y2p / th;
        if (mirror) { float t = u1; u1 = u2; u2 = t; }
        Quad q = new Quad(texture);

        float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        for (int i = 0; i < 4; i++) {
            Vector4f p = new Vector4f(c[i][0], c[i][1], c[i][2], 1.0f).mul(m);
            q.pos[i * 3] = p.x / 16.0f;
            q.pos[i * 3 + 1] = p.y / 16.0f;
            q.pos[i * 3 + 2] = p.z / 16.0f;
            q.uv[i * 2] = uvs[i][0];
            q.uv[i * 2 + 1] = uvs[i][1];
        }

        Vector4f nn = new Vector4f(n[0], n[1], n[2], 0.0f).mul(m);
        float len = (float) Math.sqrt(nn.x * nn.x + nn.y * nn.y + nn.z * nn.z);
        if (len > 1e-5f) { q.normal[0] = nn.x / len; q.normal[1] = nn.y / len; q.normal[2] = nn.z / len; }
        else { q.normal[0] = n[0]; q.normal[1] = n[1]; q.normal[2] = n[2]; }
        out.add(q);
    }

    private static float num(JsonObject o, String k, float def) {
        try { return (o != null && o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsFloat() : def; } catch (Exception e) { return def; }
    }
    private static float[] arr(JsonObject o, String k, int n) {
        float[] r = new float[n];
        if (o != null && o.has(k) && o.get(k).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(k);
            for (int i = 0; i < n && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        }
        return r;
    }
    private static float[] arr(JsonArray a, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        return r;
    }
    private static float[] arr3(JsonObject o, String k, float dx, float dy, float dz) {
        if (o != null && o.has(k) && o.get(k).isJsonArray()) return arr(o, k, 3);
        return new float[]{dx, dy, dz};
    }
}

package com.ptah.client.render;

import com.ptah.animation.AnimationClip;

public final class R15RenderState {
    public static final class Request {
        public final AnimationClip clip;
        public final float time;
        public final boolean hideHead;
        public final boolean slim;
        final Object model;
        public Request(AnimationClip clip, float time, boolean hideHead, boolean slim, Object model) {
            this.clip = clip;
            this.time = time;
            this.hideHead = hideHead;
            this.slim = slim;
            this.model = model;
        }
    }

    private static final ThreadLocal<Request> PENDING = new ThreadLocal<>();
    private R15RenderState() { }

    public static void set(AnimationClip clip, float time, boolean hideHead, boolean slim, Object model) {
        if (clip == null) { PENDING.remove(); return; }
        PENDING.set(new Request(clip, time, hideHead, slim, model));
    }

    public static void clear() { PENDING.remove(); }

    public static Request consume(Object model) {
        Request r = PENDING.get();
        if (r == null || r.model != model) return null;
        PENDING.remove();
        return r;
    }
}

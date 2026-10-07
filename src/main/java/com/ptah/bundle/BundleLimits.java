package com.ptah.bundle;

public record BundleLimits(long maxZipBytes, long maxExpandedBytes, long maxJsonBytes, int maxEmotes,
                           int maxEvents, int maxKeyframesPerBone, float maxAnimationSeconds,
                           int maxCollectionParts) {
    public static final BundleLimits DEFAULT = new BundleLimits(
            100L * 1024L * 1024L, 250L * 1024L * 1024L, 4L * 1024L * 1024L,
            256, 10_000, 20_000, 30.0f * 60.0f, 10);
}

package com.ptah.animation;

public record BoneAnimation(String bone, KeyframeTrack rotation, KeyframeTrack position, BendTrack bend) { }

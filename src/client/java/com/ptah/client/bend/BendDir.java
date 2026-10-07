package com.ptah.client.bend;

import org.joml.Quaternionf;

public enum BendDir {
    DOWN(new Quaternionf().rotationX((float) Math.PI), false),

    UP(new Quaternionf(), true);

    final Quaternionf rotation;

    final boolean inverted;

    BendDir(Quaternionf rotation, boolean inverted) {
        this.rotation = rotation;
        this.inverted = inverted;
    }
}

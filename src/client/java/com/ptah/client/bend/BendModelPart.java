package com.ptah.client.bend;

public interface BendModelPart {
    void universalEmotes$setBend(BendDir dir, float axis, float value);

    void universalEmotes$clearBend();

    void universalEmotes$copyBendFrom(BendModelPart other);
}

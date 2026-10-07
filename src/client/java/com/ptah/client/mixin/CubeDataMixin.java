package com.ptah.client.mixin;

import com.ptah.client.bend.BendCube;
import com.ptah.client.bend.CubeData;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.19.4
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.19.4
import java.util.Set;

@Mixin(ModelPart.Cube.class)
public abstract class CubeDataMixin implements BendCube {
    @Unique
    private CubeData universalEmotes$cubeData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void universalEmotes$captureData(int texCoordU, int texCoordV,
                                             float originX, float originY, float originZ,
                                             float dimensionX, float dimensionY, float dimensionZ,
                                             float growX, float growY, float growZ,
                                             boolean mirror, float texScaleU, float texScaleV,
                                             //? if >=1.19.4
                                             Set<Direction> visibleFaces,
                                             CallbackInfo ci) {
        this.universalEmotes$cubeData = new CubeData(texCoordU, texCoordV, originX, originY, originZ,
                dimensionX, dimensionY, dimensionZ, growX, growY, growZ, mirror, texScaleU, texScaleV);
    }

    @Override
    public CubeData universalEmotes$getCubeData() {
        return this.universalEmotes$cubeData;
    }
}

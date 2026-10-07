package com.ptah.client.screen;

//? if >=1.21.6 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.jetbrains.annotations.Nullable;

public final class PreviewEffectsPip {
    private PreviewEffectsPip() { }

    public static void register() {
        SpecialGuiElementRegistry.register(context -> new Renderer(context.vertexConsumers()));
    }

    public record State(GuiEntityRenderState entity) implements PictureInPictureRenderState {
        @Override public int x0() { return entity.x0(); }
        @Override public int y0() { return entity.y0(); }
        @Override public int x1() { return entity.x1(); }
        @Override public int y1() { return entity.y1(); }
        @Override public float scale() { return entity.scale(); }
        @Override public @Nullable ScreenRectangle scissorArea() { return entity.scissorArea(); }
        @Override public @Nullable ScreenRectangle bounds() { return entity.bounds(); }
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        private final EntityPart entityPart;

        Renderer(MultiBufferSource.BufferSource bufferSource) {
            super(bufferSource);
            this.entityPart = new EntityPart(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher());
        }

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack) {
            poseStack.pushPose();
            entityPart.draw(state.entity(), poseStack);
            poseStack.popPose();
            EmoteWheelPreviewEffects.renderInBlockSpace(poseStack, this.bufferSource, state.entity().translation().y);
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0f;
        }

        @Override
        protected String getTextureLabel() {
            return "universal-emotes wheel preview";
        }
    }

    private static final class EntityPart extends GuiEntityRenderer {
        EntityPart(MultiBufferSource.BufferSource bufferSource, EntityRenderDispatcher dispatcher) {
            super(bufferSource, dispatcher);
        }

        void draw(GuiEntityRenderState state, PoseStack poseStack) {
            renderToTexture(state, poseStack);
        }
    }
}
*///?}

package whocraft.tardis_refined.client.renderer.vortex;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.opengl.GL11;
import whocraft.tardis_refined.client.model.blockentity.door.interior.ShellDoorModel;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModelCollection;
import whocraft.tardis_refined.common.block.door.InternalDoorBlock;
import whocraft.tardis_refined.common.blockentity.door.GlobalDoorBlockEntity;

import java.util.SortedMap;

import static com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS;
import static net.minecraft.client.renderer.RenderStateShard.*;
import static whocraft.tardis_refined.client.renderer.blockentity.door.GlobalDoorRenderer.renderDoor;

public class RenderTargetHelper {

    public RenderTarget renderTarget;

    @Environment(EnvType.CLIENT)
    public static boolean getIsStencilEnabled(RenderTarget renderTarget) {
        return ((RenderTargetStencil) renderTarget).tr$getisStencilEnabled();
    }

    @Environment(EnvType.CLIENT)
    public static void setIsStencilEnabled(RenderTarget renderTarget, boolean cond) {
        ((RenderTargetStencil) renderTarget).tr$setisStencilEnabledAndReload(cond);
    }

    private static final RenderTargetHelper RENDER_TARGET_HELPER = new RenderTargetHelper();
    public static StencilBufferStorage stencilBufferStorage = new StencilBufferStorage();

    public static void renderVortex(GlobalDoorBlockEntity blockEntity, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        stack.pushPose();
        stack.translate(0.5F, 1.5F, 0.5F);
        stack.mulPose(Axis.ZP.rotationDegrees(180F));

        Minecraft.getInstance().getMainRenderTarget().unbindWrite();
        RENDER_TARGET_HELPER.start();
        MultiBufferSource.BufferSource imBuffer = stencilBufferStorage.getVertexConsumer();

        BlockState blockstate = blockEntity.getBlockState();
        float rotation = blockstate.getValue(InternalDoorBlock.FACING).toYRot();
        stack.mulPose(Axis.YP.rotationDegrees(rotation));
        ResourceLocation theme = blockEntity.theme();
        boolean isOpen = blockstate.getValue(InternalDoorBlock.OPEN);
        stack.translate(0, 0, -0.01);

        ShellDoorModel currentModel = ShellModelCollection.getInstance().getShellEntry(theme).getShellDoorModel(blockEntity.pattern());
        currentModel.setDoorPosition(isOpen);

        currentModel.renderFrame(blockEntity, isOpen, true, stack, imBuffer.getBuffer(RenderType.entityCutout(currentModel.getInteriorDoorTexture(blockEntity))), packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        imBuffer.endBatch();

        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        GL11.glColorMask(true, true, true, false);

        //RENDER HERE

        stack.pushPose();
        Direction facing = blockstate.getValue(InternalDoorBlock.FACING);

        float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f;

        switch (facing) {
            case NORTH:
                offsetZ = 1.0f;
                break;
            case SOUTH:
                offsetZ = -1.0f;
                break;
            case EAST:
                offsetX = -1.0f;
                break;
            case WEST:
                offsetX = 1.0f;
                break;
            case UP:
                offsetY = -1.0f;
                break;
            case DOWN:
                offsetY = 1.0f;
                break;
        }

        stack.pushPose();
        stack.translate(offsetX, offsetY, offsetZ);

        renderDoor(blockEntity, stack, imBuffer, packedLight);

        stack.popPose();
        stack.popPose();


        GL11.glColorMask(false, false, false, true);

        GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        GL11.glStencilMask(0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        stack.pushPose();
        currentModel.renderPortalMask(blockEntity, isOpen, true, stack, imBuffer.getBuffer(RenderType.entityTranslucentCull(currentModel.getInteriorDoorTexture(blockEntity))), packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        imBuffer.endBatch();
        stack.popPose();
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RENDER_TARGET_HELPER.renderTarget.blitToScreen(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        RENDER_TARGET_HELPER.end();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        stack.popPose();
    }


    public void start() {
        Window window = Minecraft.getInstance().getWindow();

        if (renderTarget != null && (renderTarget.width != window.getWidth() || renderTarget.height != window.getHeight())) {
            renderTarget = null;
        }

        if (renderTarget == null) {
            renderTarget = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
        }

        renderTarget.bindWrite(false);
        renderTarget.checkStatus();

        if (!getIsStencilEnabled(renderTarget)) {
            setIsStencilEnabled(renderTarget, true);
        }
    }

    public void end() {
        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.unbindWrite();
    }


    @Environment(value = EnvType.CLIENT)
    public static class StencilBufferStorage extends RenderBuffers {

        private final SortedMap<RenderType, BufferBuilder> typeBufferBuilder = Util.make(new Object2ObjectLinkedOpenHashMap(), map -> {
            put(map, getConsumer());
        });

        public static RenderType getConsumer() {
            RenderType.CompositeState parameters = RenderType.CompositeState.builder()
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLayeringState(NO_LAYERING).createCompositeState(false);
            return RenderType.create("vortex", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    QUADS, 256, false, true, parameters);
        }

        private final MultiBufferSource.BufferSource consumer = MultiBufferSource.immediateWithBuffers(typeBufferBuilder, new BufferBuilder(256));

        private static void put(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> builderStorage, RenderType layer) {
            builderStorage.put(layer, new BufferBuilder(layer.bufferSize()));
        }

        public MultiBufferSource.BufferSource getVertexConsumer() {
            return this.consumer;
        }
    }
}

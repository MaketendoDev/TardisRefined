package whocraft.tardis_refined.client.renderer.blockentity.door;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.opengl.GL11;
import whocraft.tardis_refined.client.model.blockentity.door.interior.ShellDoorModel;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModelCollection;
import whocraft.tardis_refined.client.renderer.vortex.RenderTargetHelper;
import whocraft.tardis_refined.common.block.door.InternalDoorBlock;
import whocraft.tardis_refined.common.blockentity.door.GlobalDoorBlockEntity;

import static com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE;
import static com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA;
import static com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA;
import static com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ZERO;

public class GlobalDoorRenderer implements BlockEntityRenderer<GlobalDoorBlockEntity>, BlockEntityRendererProvider<GlobalDoorBlockEntity> {
    private static final RenderTargetHelper RENDER_TARGET_HELPER = new RenderTargetHelper();
    protected static ShellDoorModel currentModel;

    private static final int STENCIL_PASS_VALUE = 1;

    public GlobalDoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static boolean renderDoor(GlobalDoorBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int packedLight) {
        BlockState blockstate = blockEntity.getBlockState();
        float rotation = blockstate.getValue(InternalDoorBlock.FACING).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        ResourceLocation theme = blockEntity.theme();
        boolean isOpen = blockstate.getValue(InternalDoorBlock.OPEN);
        poseStack.translate(0, 0, -0.01);

        currentModel = ShellModelCollection.getInstance().getShellEntry(theme).getShellDoorModel(blockEntity.pattern());
        currentModel.setDoorPosition(isOpen);

        currentModel.renderFrame(blockEntity, isOpen, true, poseStack, bufferSource.getBuffer(RenderType.entityTranslucentCull(currentModel.getInteriorDoorTexture(blockEntity))), packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        return isOpen;
    }

    @Override
    public void render(GlobalDoorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));

        MultiBufferSource.BufferSource imBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        try {
            boolean isOpen = renderDoor(blockEntity, poseStack, imBuffer, packedLight);

          /*  if (isOpen) {
                RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, false);
                Minecraft.getInstance().getMainRenderTarget().unbindWrite();
                RENDER_TARGET_HELPER.start();

                enableStencilTest(blockEntity, partialTick, poseStack, imBuffer, packedLight, packedOverlay);

                renderVortexEffect(blockEntity, partialTick, poseStack, imBuffer, packedLight, packedOverlay);

                disableStencilTest(blockEntity, partialTick, poseStack, imBuffer, packedLight, packedOverlay);

                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ZERO, ONE);
                RENDER_TARGET_HELPER.renderTarget.blitToScreen(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false);

                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();

                RENDER_TARGET_HELPER.end();
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                imBuffer.endBatch();
            }*/
        } finally {
            imBuffer.endBatch();
        }

        poseStack.popPose();
    }

    private void renderVortexEffect(GlobalDoorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int packedLight, int packedOverlay) {
        RenderSystem.stencilFunc(GL11.GL_EQUAL, STENCIL_PASS_VALUE, 0xFF);
        RenderSystem.colorMask(true, true, true, true);

        //NOTE This is just for testing, if we can see a door through the door, we're set!

        BlockState blockstate = blockEntity.getBlockState();
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

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        renderDoor(blockEntity, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    private void enableStencilTest(GlobalDoorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int packedLight, int packedOverlay) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GL11.glStencilFunc(GL11.GL_ALWAYS, STENCIL_PASS_VALUE, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        RenderSystem.depthMask(false);
        BlockState blockstate = blockEntity.getBlockState();
        boolean isOpen = blockstate.getValue(InternalDoorBlock.OPEN);

        currentModel.renderPortalMask(blockEntity, isOpen, true, poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucentCull(currentModel.getInteriorDoorTexture(blockEntity))),
                packedLight, OverlayTexture.NO_OVERLAY, 0f, 0f, 0f, 1f);

        bufferSource.endBatch();
        RenderSystem.depthMask(true);

        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, STENCIL_PASS_VALUE, 0xFF);
    }

    private void disableStencilTest(GlobalDoorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int packedLight, int packedOverlay) {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GL11.glColorMask(false, false, false, false);
        RenderSystem.depthMask(false);

        BlockState blockstate = blockEntity.getBlockState();
        boolean isOpen = blockstate.getValue(InternalDoorBlock.OPEN);

        currentModel.renderPortalMask(blockEntity, isOpen, true, poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucentCull(currentModel.getInteriorDoorTexture(blockEntity))),
                packedLight, OverlayTexture.NO_OVERLAY, 0f, 0f, 0f, 1f);

        bufferSource.endBatch();

        RenderSystem.depthMask(true);
        GL11.glColorMask(true, true, true, true);
    }

    @Override
    public boolean shouldRenderOffScreen(GlobalDoorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public BlockEntityRenderer<GlobalDoorBlockEntity> create(Context context) {
        return new GlobalDoorRenderer(context);
    }
}
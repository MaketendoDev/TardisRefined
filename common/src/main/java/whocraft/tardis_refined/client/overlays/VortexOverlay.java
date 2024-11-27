package whocraft.tardis_refined.client.overlays;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModel;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModelCollection;
import whocraft.tardis_refined.client.renderer.vortex.VortexRenderer;
import whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;

import static whocraft.tardis_refined.client.renderer.vortex.VortexRenderer.renderShell;
import static whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen.globalShellBlockEntity;

public class VortexOverlay {

    private static final VortexRenderer VORTEX = new VortexRenderer(VortexRenderer.VortexTypes.CLOUDS);

    private static double tardisOffsetX = 0.0D;

    private static double tardisOffsetY = 0.0D;

    public static void update() {


        if (globalShellBlockEntity != null) {
            double speed = 2.0D;
            Minecraft mc = Minecraft.getInstance();

            double radius = (double) mc.getWindow().getHeight() / 20;

            if (mc.screen == null) { // Ensure no screen (like inventory) is open
                if (mc.options.keyUp.isDown() && tardisOffsetY > -radius) {
                    tardisOffsetY -= speed;
                }
                if (mc.options.keyDown.isDown() && tardisOffsetY < radius) {
                    tardisOffsetY += speed;
                }
                if (mc.options.keyLeft.isDown() && tardisOffsetX > -radius) {
                    tardisOffsetX -= speed;
                }
                if (mc.options.keyRight.isDown() && tardisOffsetX < radius) {
                    tardisOffsetX += speed;
                }
            }
        } else {
            ShellSelectionScreen.generateDummyGlobalShell();
        }
    }



    public static void renderOverlay(GuiGraphics gg) {

        TardisPlayerInfo.get(Minecraft.getInstance().player).ifPresent(tardisPlayerInfo -> {
            /*Activation Logic*/
            TardisClientData tardisClientData = TardisClientData.getInstance(tardisPlayerInfo.getPlayerPreviousPos().getDimensionKey());
            if(!tardisPlayerInfo.isViewingTardis()) return;
            if(!tardisPlayerInfo.isRenderVortex()) return;

            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = gg.pose();
            float width = gg.guiWidth();
            float height = gg.guiHeight();

            /*Perspective Rendering*/
            RenderSystem.backupProjectionMatrix();

            Matrix4f perspective = new Matrix4f();
            perspective.perspectiveOrigin(new Vector3f());
            perspective.perspective((float) Math.toRadians(70), width / height, 0.01f, 9999);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);

            //Vortex
            pose.pushPose();
            pose.translate(0, 0, 11000);
            pose.scale(10,10,10);
            pose.mulPose(Axis.XP.rotationDegrees(00F));
            VORTEX.renderVortex(gg);
            pose.popPose();

            //Box
            pose.pushPose();
            VortexOverlay.update();
            pose.translate(0, 0, 10990);
            pose.scale(1,1,1);
            renderShell(gg, 0, 0, 1, tardisClientData.getThrottleStage());
            pose.popPose();

            //Restore Ortho view
            RenderSystem.restoreProjectionMatrix();

        });

    }

}

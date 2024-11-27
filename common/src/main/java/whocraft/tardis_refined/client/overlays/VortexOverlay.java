package whocraft.tardis_refined.client.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.client.renderer.vortex.VortexRenderer;
import whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;

import static whocraft.tardis_refined.client.renderer.vortex.ShellRenderer.renderShell;
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
            //if(!tardisPlayerInfo.isViewingTardis()) return;
            //if(!tardisPlayerInfo.isRenderVortex()) return;

            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = gg.pose();
            float width = gg.guiWidth();
            float height = gg.guiHeight();

            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition().subtract(mc.player.position()).subtract(0, 1.62, 0);

            /*Perspective Rendering*/
            RenderSystem.backupProjectionMatrix();

            Matrix4f perspective = new Matrix4f();
            perspective.perspective((float) Math.toRadians(mc.options.fov().get()), width / height, 0.1f, 9999);
            perspective.translate(0, 0, 11000);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);

            //Vortex
            pose.pushPose();

            float xRot = -mc.getCameraEntity().getXRot();
            float yRot = mc.getCameraEntity().getYRot();

            pose.mulPose(Axis.XP.rotationDegrees(xRot));
            pose.mulPose(Axis.YP.rotationDegrees(yRot));
            pose.translate(-camPos.x, -camPos.y, -camPos.z);

            pose.pushPose();
            pose.scale(100, 100, 100);
            VORTEX.renderVortex(gg, 1);
            pose.popPose();

            //Box
            VortexOverlay.update();

            renderShell(gg, 0, 0, 1, tardisClientData.getThrottleStage());

            pose.popPose();

            //Restore Ortho view
            RenderSystem.restoreProjectionMatrix();

        });

    }

}

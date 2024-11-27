package whocraft.tardis_refined.client.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.client.renderer.vortex.VortexRenderer;
import whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;

import java.util.Objects;

import static whocraft.tardis_refined.client.renderer.vortex.ShellRenderer.renderShell;
import static whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen.globalShellBlockEntity;

public class VortexOverlay {

    private static final VortexRenderer VORTEX = new VortexRenderer(VortexRenderer.VortexTypes.CLOUDS);

    private static double tardisX = 0.0D;
    private static double tardisY = 0.0D;
    private static double velX = 0.0D;
    private static double velY = 0.0D;
    private static float DEMAT = 0.0f;
    private static float IMMERSION = 0.0f;

    public static void update(GuiGraphics gg) {
        if (globalShellBlockEntity == null) {
            ShellSelectionScreen.generateDummyGlobalShell();
            return;
        }

        double speed = 0.1D;
        Minecraft mc = Minecraft.getInstance();
        float width = gg.guiWidth();
        float height = gg.guiHeight();

        double radius = 0.1;
        radius *= 0.5;

        float yRot = Objects.requireNonNull(mc.getCameraEntity()).getYRot();

        if (mc.screen == null) { // Ensure no screen (like inventory) is open
            if (mc.options.keyUp.isDown()) velY += speed;

            if (mc.options.keyDown.isDown()) velY -= speed;

            if (mc.options.keyLeft.isDown()) velX -= speed;

            if (mc.options.keyRight.isDown()) velX += speed;
        }

        if (DEMAT > 1) DEMAT = 1;
        if (DEMAT < 0) DEMAT = 0;

        if (DEMAT >= 1) {
            IMMERSION += (System.currentTimeMillis() - LAST_TIME) / (1000.0f + (5000.0f * IMMERSION));
        } else {
            IMMERSION *= 0.9f;
        }

        if (IMMERSION > 1) IMMERSION = 1;
        if (IMMERSION < 0) IMMERSION = 0;


        tardisX += velX;
        tardisY += velY;
        velX *= 0.9;
        velY *= 0.9;

        if (tardisX * tardisX + tardisY * tardisY > radius * radius) {
            tardisX *= 0.9;
            tardisY *= 0.9;
        }
        tardisX *= 0.999;
        tardisY *= 0.999;
    }


    private static long LAST_TIME = System.currentTimeMillis();

    public static void renderOverlay(GuiGraphics gg) {

        TardisPlayerInfo.get(Minecraft.getInstance().player).ifPresent(tardisPlayerInfo -> {
            /*Activation Logic*/
            TardisClientData tardisClientData = TardisClientData.getInstance(tardisPlayerInfo.getPlayerPreviousPos().getDimensionKey());
            if (!tardisPlayerInfo.isViewingTardis()) return;
            if (!tardisPlayerInfo.isRenderVortex()) return;

            /*
                Needs tweaking, but am not quite sure how to fix.
             */

            if (tardisClientData.isTakingOff() || tardisClientData.isFlying())
                DEMAT += (System.currentTimeMillis() - LAST_TIME) / 12000.0f;
            if (tardisClientData.isLanding() && !tardisClientData.isFlying())
                DEMAT -= (System.currentTimeMillis() - LAST_TIME) / 12000.0f;
            VortexOverlay.update(gg);
            LAST_TIME = System.currentTimeMillis();

            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = gg.pose();
            float width = gg.guiWidth();
            float height = gg.guiHeight();

            float demat_transparency = Mth.cos(DEMAT * (Mth.PI) / (2f)) * (Mth.cos(16f * Mth.PI * DEMAT) * 0.5f + 0.5f) * (-DEMAT * 0.5f + 0.5f) - DEMAT * 0.5f + 0.5f;

            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition().subtract(mc.player.position()).subtract(0, 1.62, 0);

            /*Perspective Rendering*/
            RenderSystem.backupProjectionMatrix();

            Matrix4f perspective = new Matrix4f();
            perspective.perspective((float) Math.toRadians(mc.options.fov().get()), width / height, 0.1f, 9999);
            perspective.translate(0, 0, 11000);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);

            pose.pushPose();

            float mul = IMMERSION;
            float mulinv = 1 - IMMERSION;

            float xRot = -mc.getCameraEntity().getXRot() * mulinv;
            float yRot = mc.getCameraEntity().getYRot() % 360;

            while (yRot > 180) yRot -= 360;
            while (yRot < -180) yRot += 360;

            yRot *= mulinv;

            pose.mulPose(Axis.XP.rotationDegrees(xRot));
            pose.mulPose(Axis.YP.rotationDegrees(yRot));

            pose.translate(-camPos.x * mulinv, -camPos.y * mulinv, -camPos.z * mulinv);

            //Vortex
            pose.pushPose();
            pose.scale(100, 100, 100);

            VORTEX.renderVortex(gg, 1 - demat_transparency);
            pose.popPose();


            //Box
            pose.translate(tardisX * mul, tardisY * mul, -5 * mul);
            renderShell(gg, IMMERSION, 1 - demat_transparency, tardisClientData.getThrottleStage());

            pose.popPose();

            //Restore Ortho view
            RenderSystem.restoreProjectionMatrix();
        });
    }
}

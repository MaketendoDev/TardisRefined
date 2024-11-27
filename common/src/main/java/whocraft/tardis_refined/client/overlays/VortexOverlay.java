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

import java.util.Objects;

import static whocraft.tardis_refined.client.renderer.vortex.ShellRenderer.renderShell;
import static whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen.globalShellBlockEntity;

public class VortexOverlay {

    private static final VortexRenderer VORTEX = new VortexRenderer(VortexRenderer.VortexTypes.CLOUDS);

    private static double tardisX = 0.0D;
    private static double tardisY = 0.0D;
    private static double velX = 0.0D;
    private static double velY = 0.0D;

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
            if (mc.options.keyUp.isDown())
                velY += speed;

            if (mc.options.keyDown.isDown())
                velY -= speed;

            if (mc.options.keyLeft.isDown())
                velX -= speed;

            if (mc.options.keyRight.isDown())
                velX += speed;
        }

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


    public static void renderOverlay(GuiGraphics gg) {

        TardisPlayerInfo.get(Minecraft.getInstance().player).ifPresent(tardisPlayerInfo -> {
            /*Activation Logic*/
            TardisClientData tardisClientData = TardisClientData.getInstance(tardisPlayerInfo.getPlayerPreviousPos().getDimensionKey());
            if (!tardisPlayerInfo.isViewingTardis()) return;
            if (!tardisPlayerInfo.isRenderVortex()) return;

            VortexOverlay.update(gg);

            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = gg.pose();
            float width = gg.guiWidth();
            float height = gg.guiHeight();

            /*
            THIS FLOAT CONTROLS SEVERAL THINGS.
            from 0 to 1, the perspective will match third person while the vortex fades in. from 1 to 2 it will fade out some of the perspective calculations like pitch and yaw and fade in some of the animations for the Shell
             */


           /* long long_speed = (long) (6f * 1000L);
            long time = System.currentTimeMillis() + (long) (1000L * 0);
            float zzz = (time % long_speed) / (6f * 1000.0f);


            float control = 1 + Mth.sin(zzz * Mth.DEG_TO_RAD * 360);*/

            float control = 2;


            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition().subtract(mc.player.position()).subtract(0, 1.62, 0);

            /*Perspective Rendering*/
            RenderSystem.backupProjectionMatrix();

            Matrix4f perspective = new Matrix4f();
            perspective.perspective((float) Math.toRadians(mc.options.fov().get()), width / height, 0.1f, 9999);
            perspective.translate(0, 0, 11000);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);

            pose.pushPose();

            float mul = Math.max(control - 1, 0);
            float mulinv = 1 - Math.max(control - 1, 0);

            float xRot = -mc.getCameraEntity().getXRot() * mulinv;
            float yRot = mc.getCameraEntity().getYRot() * mulinv;

            pose.mulPose(Axis.XP.rotationDegrees(xRot));
            pose.mulPose(Axis.YP.rotationDegrees(yRot));

            pose.translate(-camPos.x * mulinv, -camPos.y * mulinv, -camPos.z * mulinv);

            //Vortex
            pose.pushPose();
            pose.scale(100, 100, 100);
            VORTEX.renderVortex(gg, control);
            pose.popPose();


            //Box
            pose.translate(tardisX * mul, tardisY * mul, -5 * mul);
            renderShell(gg, Math.max(control - 1, 0), tardisClientData.getThrottleStage());

            pose.popPose();

            //Restore Ortho view
            RenderSystem.restoreProjectionMatrix();

        });

    }

}

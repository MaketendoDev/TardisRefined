package whocraft.tardis_refined.client.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import whocraft.tardis_refined.client.renderer.vortex.VortexRenderer;

public class VortexOverlay {

    private static final VortexRenderer VORTEX = new VortexRenderer(VortexRenderer.VortexTypes.CLOUDS);

    public static void renderOverlay(GuiGraphics gg) {

        Minecraft mc = Minecraft.getInstance();

        RenderSystem.backupProjectionMatrix();

        PoseStack pose = gg.pose();

        float width = gg.guiWidth();
        float height = gg.guiHeight();

        pose.pushPose();

        pose.translate(0, 0, 11000);

        Matrix4f perspective = new Matrix4f();
        perspective.perspectiveOrigin(new Vector3f());
        perspective.perspective((float) Math.toRadians(70), width / height, 0.1f, 100);

        RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);

        VORTEX.renderVortex(pose);

        pose.popPose();

        RenderSystem.restoreProjectionMatrix();
    }

}

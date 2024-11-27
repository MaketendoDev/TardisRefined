package whocraft.tardis_refined.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class RenderHelper {

    public static Tesselator tesselator;

    public static void renderFilledBox(PoseStack stack, VertexConsumer vertexConsumer, AABB box, float red, float green, float blue, float alpha, int combinedLightIn) {
        Matrix4f matrix = stack.last().pose();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();

        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();

        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();

        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();

        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();

        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
        vertexConsumer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).uv2(combinedLightIn).endVertex();
    }


    public static void drawGlowingBox(PoseStack poseStack, VertexConsumer consumer, float length, float height, float width, float red, float green, float blue, float alpha, int combinedLightIn) {
        AABB box = new AABB(-length / 2F, -height / 2f, -width / 2F, length / 2F, height / 2f, width / 2F);
        renderFilledBox(poseStack, consumer, box, 1F, 1F, 1F, alpha, combinedLightIn);

        for (int i = 0; i < 3; i++) {
            renderFilledBox(poseStack, consumer, box.inflate(i * 0.5F * 0.0625F), red, green, blue, (1F / i / 2) * alpha, combinedLightIn);
        }
    }

    public static void rotateZYX(PoseStack poseStack, float x, float y, float z) {
        poseStack.mulPose((new Quaternionf()).rotationZYX(Mth.DEG_TO_RAD * z, Mth.DEG_TO_RAD * y, Mth.DEG_TO_RAD * x));
    }

    public static Tesselator beginTextureColor(ResourceLocation texture, VertexFormat.Mode mode, boolean cull) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        if (cull) RenderSystem.enableCull();
        else RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        tesselator = Tesselator.getInstance();
        tesselator.getBuilder().begin(mode, DefaultVertexFormat.POSITION_TEX_COLOR);
        return tesselator;
    }

    public static void vertexUVColor(@NotNull PoseStack pose, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        tesselator.getBuilder().vertex(pose.last().pose(), x, y, z).uv(u, v).color(r, g, b, a).endVertex();
    }
}

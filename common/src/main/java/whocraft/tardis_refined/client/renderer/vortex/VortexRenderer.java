package whocraft.tardis_refined.client.renderer.vortex;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import whocraft.tardis_refined.TardisRefined;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Time Vortex Renderer
 *
 * @author Edrax
 **/
@Environment(EnvType.CLIENT)
public class VortexRenderer {

    public enum VortexTypes {
        CLOUDS(new ResourceLocation(TardisRefined.MODID, "textures/vortex/clouds.png"), true);


        public int sides = 9, rows = 12;
        float twist = 10;
        public boolean decals = true;
        public boolean lightning = false;
        public final ResourceLocation texture;

        VortexTypes(ResourceLocation texture) {
            this.texture = texture;
        }

        VortexTypes(ResourceLocation texture, boolean lightning) {
            this.texture = texture;
            this.lightning = lightning;
        }

        VortexTypes(ResourceLocation texture, int sides, int rows, float twist, boolean lightning, boolean decals) {
            this.texture = texture;
            this.lightning = lightning;
            this.sides = sides;
            this.rows = rows;
            this.twist = twist;
            this.decals = decals;
        }
    }

    public static float SPEED = 1;

    private final VortexTypes vortexType;

    public VortexRenderer(VortexTypes type) {
        this.vortexType = type;
    }

    private final List<VortexQuad> vortex_quads = new ArrayList<>();

    /**
     * Renders the Time Vortex
     */
    public void renderVortex(PoseStack pose) {

        if (SPEED > 1) SPEED *= 0.9999999999f;
        if (SPEED < 1.25f) SPEED = 3;
        this.vortexType.sides = 9;

        pose.pushPose();

        rotate(pose, 90.0f, 0, 0.0f);
        pose.scale(1, this.vortexType.rows, 1);
        rotate(pose, 0, 360 * timing(5000), 0);

        for (int row = -this.vortexType.rows; row < this.vortexType.rows; row++) {
            Tesselator tesselator = beginTextureColor(Mode.TRIANGLE_STRIP);
            pose.pushPose();
            pose.translate(0, o(row), 0);
            rotate(pose, 0, row * this.vortexType.twist, 0);

            renderCylinder(pose, row);

            pose.popPose();
            tesselator.end();
        }

        if (this.vortexType.decals) {
            Tesselator tesselator = beginTextureColor(Mode.QUADS);
            for (int i = 0; i < 16 / (1 + SPEED); i++) {
                pose.pushPose();
                if (vortex_quads.size() < i + 1) {
                    vortex_quads.add(new VortexQuad(this.vortexType));
                    break;
                }
                vortex_quads.get(i).renderQuad(pose, i * 0.1f * SPEED * SPEED);
                pose.popPose();
            }
            tesselator.end();
        }

        pose.popPose();

    }

    private void renderCylinder(PoseStack poseStack, int row) {

        float length = 1f / this.vortexType.rows;

        float oA = o(row + 1), oB = o(row);

        float radiusA = wobbleRadius(oA);
        float radiusB = wobbleRadius(oB);

        for (int s = 0; s <= this.vortexType.sides; s++) {
            float angle = 2 * Mth.PI * s / this.vortexType.sides;

            float xA = radiusA * Mth.cos(angle);
            float zA = radiusA * Mth.sin(angle);
            xA += xWobble(oA) * Mth.sin(oA);
            zA += zWobble(oA) * Mth.sin(oA);

            float xB = radiusB * Mth.cos(angle);
            float zB = radiusB * Mth.sin(angle);
            xB += xWobble(oB) * Mth.sin(oB);
            zB += zWobble(oB) * Mth.sin(oB);

            float u = (float) s / this.vortexType.sides * 0.5f;

            float timeOffset = timing(SPEED);
            float uvOffset = length * row;
            float vA = length + uvOffset + timeOffset;
            float vB = 0.0f + uvOffset + timeOffset;

            float bA = radiusFunc(oA);
            float bB = radiusFunc(oB);
            poseStack.pushPose();
            vertexUVColor(poseStack, xA, length, zA, u, vA, bA, bA, bA, 1);
            rotate(poseStack, 0, -this.vortexType.twist, 0);
            vertexUVColor(poseStack, xB, 0, zB, u, vB, bB, bB, bB, 1);
            poseStack.popPose();
        }
    }

    private static Tesselator tesselator;

    private Tesselator beginTextureColor(Mode mode) {
        return beginTextureColor(this.vortexType.texture, mode);
    }

    private static Tesselator beginTextureColor(ResourceLocation texture, Mode mode) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        tesselator = Tesselator.getInstance();
        tesselator.getBuilder().begin(mode, DefaultVertexFormat.POSITION_TEX_COLOR);
        return tesselator;
    }

    private static void vertexUVColor(@NotNull PoseStack pose, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        tesselator.getBuilder().vertex(pose.last().pose(), x, y, z).uv(u, v).color(r, g, b, a).endVertex();
    }

    /**
     * Will generate a float which will go from 0 to 1 within specified seconds
     *
     * @param speed  Speed at which the float will move
     * @param offset Add an offset in seconds
     * @return 0 to 1 float
     */
    private static float timingWithOffset(float speed, float offset) {
        if (speed == 0) return 1;
        long long_speed = (long) (speed * 1000L);
        long time = System.currentTimeMillis() + (long) (1000L * offset);
        return (time % long_speed) / (speed * 1000.0f);
    }

    private static float timing(float speed) {
        return timingWithOffset(speed, 0);
    }

    private float o(int row) {
        return row / (float) this.vortexType.rows;
    }

    private static float radiusFunc(float o) {
        return -(o * o) + 1;
    }

    private static float wobbleRadius(float o) {
        return radiusFunc(o) * (1 + (0.05f) * Mth.sin(Mth.DEG_TO_RAD * 360 * (o + timing(687))) * Mth.sin(Mth.DEG_TO_RAD * 360 * (o + timing(9852))));
    }

    private static float xWobble(float o) {
        float f = SPEED;//for debug purposes. will increase the speed at which the vortex will "Wobble"
        return (Mth.sin(o * 1 + timing((int) (0.999 * f)) * 2 * Mth.PI) + Mth.sin(o * 0.5f + timing((int) (1.778 * f)) * 2 * Mth.PI)) * 2 / SPEED;
    }

    private static float zWobble(float o) {
        float f = SPEED;//for debug purposes. will increase the speed at which the vortex will "Wobble"
        return (Mth.cos(o * 1 + timing((int) (1.256 * f)) * 2 * Mth.PI) + Mth.cos(o * 0.5f + timing((int) (1.271 * f)) * 2 * Mth.PI)) * 2 / SPEED;
    }


    private static void rotate(PoseStack poseStack, float x, float y, float z) {
        poseStack.mulPose((new Quaternionf()).rotationZYX(Mth.DEG_TO_RAD * z, Mth.DEG_TO_RAD * y, Mth.DEG_TO_RAD * x));
    }


    private static final RandomSource RAND = RandomSource.create();

    private static class VortexQuad {

        public boolean valid = true, lightning = false;
        private float prev_tO = -1;
        private float u = 0, v = 0;
        private final float uvSize = 0.125f;
        float lightning_a;
        private final VortexTypes vortexType;

        public VortexQuad(VortexTypes type) {
            this.vortexType = type;
        }

        private void rndQuad() {
            valid = true;
            prev_tO = 1;
            rndUV();
            lightning = RAND.nextBoolean() && this.vortexType.lightning;
        }

        private void rndUV() {
            u = RAND.nextIntBetweenInclusive(0, 3) * uvSize;
            v = RAND.nextIntBetweenInclusive(0, 3) * uvSize;
        }


        public void renderQuad(PoseStack poseStack, float time_offset) {

            if (!valid) rndQuad();

            float tO = 1 - (timingWithOffset(SPEED * 2, time_offset) * 2);

            if (tO > prev_tO || !valid) {
                valid = false;
                return;
            }

            if (lightning && System.currentTimeMillis() % 5 == 0) if (lightning && Math.random() > 0.95f) {
                lightning_a = 1;
                rndUV();
            }

            float u0 = 0.5f + u, v0 = v + (lightning ? 0.5f : 0);
            float u1 = u0 + uvSize, v1 = v0 + uvSize;

            float x = xWobble(tO) * Mth.sin(tO), z = zWobble(tO) * Mth.sin(tO);
            float s = wobbleRadius(tO);
            float bA = lightning ? 1 : radiusFunc(tO);

            float alpha = lightning ? lightning_a : bA;

            poseStack.pushPose();
            rotate(poseStack, 0, -this.vortexType.twist, 0);
            rotate(poseStack, 0, tO * this.vortexType.rows * this.vortexType.twist, 0);
            vertexUVColor(poseStack, x - s, tO, z + s, u0, v1, bA, bA, bA, alpha);
            vertexUVColor(poseStack, x + s, tO, z + s, u1, v1, bA, bA, bA, alpha);
            vertexUVColor(poseStack, x + s, tO, z - s, u1, v0, bA, bA, bA, alpha);
            vertexUVColor(poseStack, x - s, tO, z - s, u0, v0, bA, bA, bA, alpha);

            poseStack.popPose();
            prev_tO = tO;
            lightning_a *= 0.9f;
        }
    }
}

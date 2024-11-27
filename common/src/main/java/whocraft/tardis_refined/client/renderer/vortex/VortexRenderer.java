package whocraft.tardis_refined.client.renderer.vortex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.renderer.RenderHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom Time Vortex Renderer
 *
 * @author Edrax
 **/
@Environment(EnvType.CLIENT)
public class VortexRenderer {

    public VortexRenderer(VortexTypes type) {
        this.vortexType = type;
    }

    public enum VortexTypes {
        CLOUDS(new ResourceLocation(TardisRefined.MODID, "textures/vortex/clouds.png"), 9, 12, 10, true, true, new VortexGradientTint().add(0f, 0, 0.5f, 1).add(-1f, 1, 0.5f, 0).add(0f, 0.5f, 0.75f, 1f));

        public int sides = 9, rows = 12;
        float twist = 10;
        public boolean decals = true;
        public boolean lightning = false;
        public final ResourceLocation texture;
        public final VortexGradientTint gradient;

        VortexTypes(ResourceLocation texture, int sides, int rows, float twist, boolean lightning, boolean decals, VortexGradientTint gradient) {
            this.texture = texture;
            this.lightning = lightning;
            this.sides = sides;
            this.rows = rows;
            this.twist = twist;
            this.decals = decals || lightning;
            this.gradient = gradient;
        }
    }

    private final RenderHelper.DynamicTimeKeep time = new RenderHelper.DynamicTimeKeep(2);
    private final VortexTypes vortexType;
    private final List<VortexQuad> vortex_quads = new ArrayList<>();
    private float opacity = 1;

    /**
     * Renders the Time Vortex
     */
    public void renderVortex(PoseStack pose, float opacity) {
        this.opacity = Math.min(opacity, 1);
        this.vortexType.rows = 12;
        this.vortexType.gradient.offset = 0;
        pose.pushPose();

        RenderHelper.rotateZYX(pose, 90.0f, 180, 0.0f);
        pose.scale(1, this.vortexType.rows, 1);

        for (int row = -this.vortexType.rows; row < this.vortexType.rows; row++) {
            Tesselator tesselator = beginTextureColor(Mode.TRIANGLE_STRIP);
            pose.pushPose();
            pose.translate(0, o(row), 0);
            RenderHelper.rotateZYX(pose, 0, row * this.vortexType.twist, 0);

            renderCylinder(pose, row);

            pose.popPose();
            tesselator.end();
        }

        if (this.vortexType.decals) {
            Tesselator tesselator = beginTextureColor(Mode.QUADS);
            for (int i = 0; i < 16 / (1 + time.speed); i++) {
                pose.pushPose();
                if (vortex_quads.size() < i + 1) {
                    vortex_quads.add(new VortexQuad(this.vortexType, this.time));
                    break;
                }
                vortex_quads.get(i).renderQuad(pose, (float) (i * 0.1f * time.speed * time.speed), this.opacity);
                pose.popPose();
            }
            tesselator.end();
        }
        pose.popPose();
    }

    public void renderVortex(GuiGraphics guiGraphics, float opacity) {
        PoseStack pose = guiGraphics.pose();
        renderVortex(pose, opacity);
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
            xA += xWobble(oA, (float) time.speed) * Mth.sin(oA);
            zA += zWobble(oA, (float) time.speed) * Mth.sin(oA);

            float xB = radiusB * Mth.cos(angle);
            float zB = radiusB * Mth.sin(angle);
            xB += xWobble(oB, (float) time.speed) * Mth.sin(oB);
            zB += zWobble(oB, (float) time.speed) * Mth.sin(oB);

            float u = (float) s / this.vortexType.sides * 0.5f;

            float timeOffset = time.getFloat();
            float uvOffset = length * row;
            float vA = length + uvOffset + timeOffset;
            float vB = 0.0f + uvOffset + timeOffset;

            float bA = radiusFunc(oA);
            float bB = radiusFunc(oB);

            vertexUVColor(poseStack, xA, length, zA, u, vA, bA, bA, bA, 1.0f, oA);
            RenderHelper.rotateZYX(poseStack, 0, -this.vortexType.twist, 0);
            vertexUVColor(poseStack, xB, 0, zB, u, vB, bB, bB, bB, 1, oB);
            RenderHelper.rotateZYX(poseStack, 0, this.vortexType.twist, 0);
        }

    }

    private Tesselator beginTextureColor(Mode mode) {
        return RenderHelper.beginTextureColor(this.vortexType.texture, mode, false);
    }

    private void vertexUVColor(@NotNull PoseStack pose, float x, float y, float z, float u, float v, float r, float g, float b, float a, float o) {
        float[] color = this.vortexType.gradient.getRGBf(o);
        RenderHelper.vertexUVColor(pose, x, y, z, u, v, r * color[0], g * color[1], b * color[2], a * this.opacity);
    }

    private static float timingWithOffset(float speed, float offset) {
        if (speed == 0) return 1;
        long long_speed = (long) (speed * 1000L);
        long time = System.currentTimeMillis() + (long) (1000L * offset);
        return (time % long_speed) / (speed * 1000.0f);
    }

    private static float timing(float speed) {
        return timingWithOffset(speed, 0.0f);
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

    private static float xWobble(float o, float SPEED) {
        return (Mth.sin(o * 1 + timing((int) (0.999 * SPEED)) * 2 * Mth.PI) + Mth.sin(o * 0.5f + timing((int) (1.778 * SPEED)) * 2 * Mth.PI)) * 2 / SPEED;
    }

    private static float zWobble(float o, float SPEED) {
        return (Mth.cos(o * 1 + timing((int) (1.256 * SPEED)) * 2 * Mth.PI) + Mth.cos(o * 0.5f + timing((int) (1.271 * SPEED)) * 2 * Mth.PI)) * 2 / SPEED;
    }


    private static final RandomSource RAND = RandomSource.create();

    private static class VortexQuad {

        public boolean valid = true, lightning = false;
        private float prev_tO = -1;
        private float u = 0, v = 0;
        private final float uvSize = 0.125f;
        float lightning_a;
        private final VortexTypes vortexType;
        private final RenderHelper.DynamicTimeKeep time;

        public VortexQuad(VortexTypes type, RenderHelper.DynamicTimeKeep time) {
            this.vortexType = type;
            this.time = time;
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


        public void renderQuad(PoseStack poseStack, float time_offset, float opacity) {

            if (!valid) rndQuad();

            float tO = 1 - (time.getFloat(time_offset) * 2);

            if (tO > prev_tO || !valid) {
                valid = false;
                return;
            }

            if (lightning && System.currentTimeMillis() % 5 == 0) if (lightning && Math.random() > 0.95f) {
                lightning_a = 1;
                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.playSound(Math.random() < 0.5F ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.LIGHTNING_BOLT_IMPACT, (float) Math.random(), (float) Math.random());
                rndUV();
            }

            float u0 = 0.5f + u, v0 = v + (lightning ? 0.5f : 0);
            float u1 = u0 + uvSize, v1 = v0 + uvSize;

            float x = xWobble(tO, (float) time.speed) * Mth.sin(tO), z = zWobble(tO, (float) time.speed) * Mth.sin(tO);
            float s = wobbleRadius(tO);
            float val = lightning ? 1 : radiusFunc(tO);

            float alpha = lightning ? lightning_a : val;
            alpha *= opacity;
            poseStack.pushPose();
            RenderHelper.rotateZYX(poseStack, 0, -this.vortexType.twist, 0);
            RenderHelper.rotateZYX(poseStack, 0, tO * this.vortexType.rows * this.vortexType.twist, 0);
            vertexUVColor(poseStack, x - s, tO, z + s, u0, v1, val, alpha, tO, !lightning);
            vertexUVColor(poseStack, x + s, tO, z + s, u1, v1, val, alpha, tO, !lightning);
            vertexUVColor(poseStack, x + s, tO, z - s, u1, v0, val, alpha, tO, !lightning);
            vertexUVColor(poseStack, x - s, tO, z - s, u0, v0, val, alpha, tO, !lightning);

            poseStack.popPose();
            prev_tO = tO;
            lightning_a *= 0.9f;
        }

        private void vertexUVColor(@NotNull PoseStack pose, float x, float y, float z, float u, float v, float val, float a, float o, boolean tint) {
            float[] color = this.vortexType.gradient.getRGBf(o);
            if (tint)
                RenderHelper.vertexUVColor(pose, x, y, z, u, v, val * color[0], val * color[1], val * color[2], a);
            else RenderHelper.vertexUVColor(pose, x, y, z, u, v, val, val, val, a);
        }
    }

    public static class VortexGradientTint {
        private final Map<Float, float[]> gradient_map = new HashMap<>();
        public float offset = 0;

        public VortexGradientTint() {
        }

        /**
         * Adds a color to the gradient map
         *
         * @param pos position in the gradient the color should go in. can be from -1 to 1
         * @param r   RED 0 to 1
         * @param g   GREEN 0 to 1
         * @param b   BLUE 0 to 1
         * @return The VortexGradient with the color added
         */
        public VortexGradientTint add(float pos, float r, float g, float b) {
            this.gradient_map.put(pos, new float[]{r, g, b});
            return this;
        }

        public float[] getRGBf(float pos_original) {
            float r = 1, g = 1, b = 1;
            float[] out = new float[]{r, g, b};
            if (gradient_map.isEmpty()) return out;
            if (gradient_map.size() == 1) {
                for (float p : gradient_map.keySet()) {
                    out = gradient_map.get(p);
                }
                return out;
            }

            float pos = pos_original + offset;
            if (pos > 1) pos -= 2;
            if (pos < -1) pos += 2;

            float first = 0, second = 0, smallest_dist = 9999, second_smallest_dist = 1000;

            for (float p : gradient_map.keySet()) {
                float dist = Mth.abs(pos - p);
                if (dist < smallest_dist) {
                    second_smallest_dist = smallest_dist;
                    smallest_dist = dist;
                    second = first;
                    first = p;
                }
            }

            if (gradient_map.get(first) == null || gradient_map.get(second) == null) return out;

            r = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[0], gradient_map.get(second)[0]);
            g = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[1], gradient_map.get(second)[1]);
            b = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[2], gradient_map.get(second)[2]);
            return new float[]{r, g, b};
        }
    }
}

package whocraft.tardis_refined.client.renderer.vortex;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModel;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModelCollection;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;
import whocraft.tardis_refined.patterns.ShellPattern;
import whocraft.tardis_refined.patterns.ShellPatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static whocraft.tardis_refined.client.screen.selections.ShellSelectionScreen.globalShellBlockEntity;

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
        public final VortexGradientTint gradient = new VortexGradientTint(true).add(0, 0, 0.5f, 1).add(-1, 1, 0.5f, 0);

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
    public void renderVortex(GuiGraphics guiGraphics) {
        PoseStack pose = guiGraphics.pose();
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


    public static void renderShell(GuiGraphics guiGraphics, int x, int y, float scale, int throttle) {
        TardisPlayerInfo.get(Minecraft.getInstance().player).ifPresent(tardisPlayerInfo -> {
            TardisClientData tardisClientData = TardisClientData.getInstance(tardisPlayerInfo.getPlayerPreviousPos().getDimensionKey());
            ResourceLocation shellPattern = tardisClientData.getShellPattern();
            ResourceLocation shellTheme = tardisClientData.getShellTheme();

            ShellPattern fullPattern = ShellPatterns.getPatternOrDefault(shellTheme, shellPattern);

            ShellModel model = ShellModelCollection.getInstance().getShellEntry(shellTheme).getShellModel(fullPattern);
            model.setDoorPosition(false);
            Lighting.setupFor3DItems();
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            // Position the shell and apply scale
            pose.translate((float) x, y, 0);
            pose.scale(scale, scale, scale);
            rotate(pose,180,0,0);

            // Time-based calculations for loopable motion and rotation
            long time = System.currentTimeMillis();
            float timeFactor = (time % 4000L) / 4000.0f * (float) (2 * Math.PI);

            // Chaotic but loopable rotations
            float xRotation = (float) Math.sin(timeFactor * 2) * 15.0f; // Wobble on X-axis
            float yRotation = ((timeFactor * 360 / (float) (2 * Math.PI)) % 360) * throttle; // Continuous spin on Y-axis
            float zRotation = (float) Math.cos(timeFactor * 3) * 10.0f; // Wobble on Z-axis

            // Apply rotations
            pose.mulPose(Axis.XP.rotationDegrees(xRotation));
            pose.mulPose(Axis.YP.rotationDegrees(yRotation));
            pose.mulPose(Axis.ZP.rotationDegrees(zRotation));

            VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(model.renderType(model.getShellTexture(ShellPatterns.getPatternOrDefault(shellTheme, shellPattern), false)));
            model.renderShell(globalShellBlockEntity, false, false, pose, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            if(fullPattern.exteriorDoorTexture().emissive()){
                VertexConsumer vertexConsumerLighting = guiGraphics.bufferSource().getBuffer(RenderType.eyes(model.getShellTexture(ShellPatterns.getPatternOrDefault(shellTheme, shellPattern), true)));
                model.renderShell(globalShellBlockEntity, false, false, pose, vertexConsumerLighting, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            }

            guiGraphics.flush();
            pose.popPose();
            Lighting.setupFor3DItems();
        });

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
            vertexUVColor(poseStack, xA, length, zA, u, vA, bA, bA, bA, 1, true);
            rotate(poseStack, 0, -this.vortexType.twist, 0);
            vertexUVColor(poseStack, xB, 0, zB, u, vB, bB, bB, bB, 1, true);
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

    private void vertexUVColor(@NotNull PoseStack pose, float x, float y, float z, float u, float v, float r, float g, float b, float a, boolean tint) {
        float[] color = this.vortexType.gradient.getRGBf(y);
        if (tint) vertexUVColor(pose, x, y, z, u, v, r * color[0], g * color[1], b * color[2], a);
        else vertexUVColor(pose, x, y, z, u, v, r, g, b, a);
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
                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.playSound(Math.random() < 0.5F ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.LIGHTNING_BOLT_IMPACT, (float) Math.random(), (float) Math.random());
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

    private static class VortexGradientTint {
        private Map<Float, float[]> gradient_map = new HashMap<>();
        private boolean loop = false;

        public VortexGradientTint(boolean loop) {
            this.loop = loop;
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

        public float[] getRGBf(float pos) {
            float r = 1, g = 1, b = 1;
            float[] out = new float[]{r, g, b};
            if (gradient_map.keySet().size() <= 0) return out;
            if (gradient_map.keySet().size() == 1) {
                for (float p : gradient_map.keySet()) {
                    out = gradient_map.get(p);
                }
                return out;
            }


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

            r = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[0], gradient_map.get(second)[0]);
            g = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[1], gradient_map.get(second)[1]);
            b = Mth.lerp(smallest_dist / (smallest_dist + second_smallest_dist), gradient_map.get(first)[2], gradient_map.get(second)[2]);
            return new float[]{r, g, b};
        }
    }
}

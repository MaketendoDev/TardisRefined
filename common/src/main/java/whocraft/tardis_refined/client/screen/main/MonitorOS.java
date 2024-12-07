package whocraft.tardis_refined.client.screen.main;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModel;
import whocraft.tardis_refined.client.model.blockentity.shell.ShellModelCollection;
import whocraft.tardis_refined.client.overlays.VortexOverlay;
import whocraft.tardis_refined.client.screen.ScreenHelper;
import whocraft.tardis_refined.client.screen.components.CommonTRWidgets;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.common.blockentity.shell.GlobalShellBlockEntity;
import whocraft.tardis_refined.common.tardis.themes.ShellTheme;
import whocraft.tardis_refined.patterns.ShellPattern;
import whocraft.tardis_refined.patterns.ShellPatterns;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class MonitorOS extends Screen {

    public static ResourceLocation FRAME = new ResourceLocation(TardisRefined.MODID, "textures/gui/monitor/frame_brass.png");
    protected static final int frameWidth = 256, frameHeight = 180;
    public ResourceLocation backdrop = null;
    protected static final int monitorWidth = 230, monitorHeight = 130;

    public static ResourceLocation NOISE = new ResourceLocation(TardisRefined.MODID, "textures/gui/monitor/noise.png");

    private double displayOffset;

    public MonitorOS LEFT;
    public MonitorOS RIGHT;
    public MonitorOS PREVIOUS;

    public static final ResourceLocation BUTTON_LOCATION = new ResourceLocation(TardisRefined.MODID, "save");
    public static final ResourceLocation BCK_LOCATION = new ResourceLocation(TardisRefined.MODID, "back");
    public int shakeX, shakeY, age, transitionStartTime = -1;
    public float shakeAlpha;
    private MonitorOSRun onSubmit;
    private MonitorOSRun onCancel;
    public List<Renderable> renderables;

    public MonitorOS(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        ObjectSelectionList<SelectionListEntry> list = createSelectionList();
        this.addRenderableWidget(list);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        int hPos = (width - monitorWidth) / 2;
        int vPos = (height - monitorHeight) / 2;

        guiGraphics.enableScissor(0, 0, width, vPos + shakeY);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        guiGraphics.enableScissor(0, vPos + shakeY, hPos + shakeX, height - vPos + shakeY);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        guiGraphics.enableScissor(width - hPos + shakeX, vPos + shakeY, width, height - vPos + shakeY);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        guiGraphics.enableScissor(0, height - vPos + shakeY, width, height);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

    }

    public void renderVortex(@NotNull GuiGraphics guiGraphics) {
        PoseStack poseStack = guiGraphics.pose();

        int hPos = (width - monitorWidth) / 2;
        int vPos = (height - monitorHeight) / 2;

        guiGraphics.enableScissor(hPos, vPos, width - hPos, height - vPos);
        RenderSystem.backupProjectionMatrix();
        assert minecraft != null;
        Matrix4f perspective = new Matrix4f();
        perspective.perspective((float) Math.toRadians(minecraft.options.fov().get()), (float) width / (float) height, 0.01f, 9999, false, perspective);
        perspective.translate(0, 0, 11000f);
        RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(20));
        VortexOverlay.VORTEX.time.speed = 0.3;
        VortexOverlay.VORTEX.renderVortex(guiGraphics, 1, false);
        RenderSystem.restoreProjectionMatrix();
        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    public void renderBackdrop(@NotNull GuiGraphics guiGraphics) {
        int hPos = (width - monitorWidth) / 2;
        int vPos = (height - monitorHeight) / 2;
        if (backdrop == null) renderVortex(guiGraphics);
        else guiGraphics.blit(backdrop, hPos + shakeX, vPos + shakeY, 0, 0, frameWidth, frameHeight);
        int b = height - vPos, r = width - hPos;
        guiGraphics.fill(hPos + shakeX, vPos + shakeY, r, b, 0x40000000);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderBackdrop(guiGraphics);
        PoseStack poseStack = guiGraphics.pose();

        doRender(guiGraphics, mouseX, mouseY, partialTick);
        ScreenHelper.renderWidthScaledText(title.getString(), guiGraphics, Minecraft.getInstance().font, width / 2f, 5 + (height - monitorHeight) / 2f, Color.LIGHT_GRAY.getRGB(), 300, true);

        int hPos = (width - monitorWidth) / 2;
        int vPos = (height - monitorHeight) / 2;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (shakeAlpha + 1 - partialTick) / 100.0f);
        guiGraphics.blit(NOISE, hPos + shakeX, vPos + shakeY, (int) (Math.random() * 230 * 3), (int) ((System.currentTimeMillis() % 1000L) / 1000.0), monitorWidth, monitorHeight);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (LEFT != null) {
            poseStack.pushPose();
            poseStack.translate(50, 0, 0);
            LEFT.render(guiGraphics, mouseX, mouseY, partialTick);
            poseStack.popPose();
        }

        renderFrame(guiGraphics, mouseX, shakeY, partialTick);
    }

    public void doRender(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    public void renderFrame(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        int hPos = (width - frameWidth) / 2;
        int vPos = -13 + (height - monitorHeight) / 2;

        guiGraphics.blit(FRAME, hPos + shakeX, vPos + shakeY, 0, 0, frameWidth, frameHeight);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (transitionStartTime >= 0 && age - transitionStartTime >= 10) {
            transitionStartTime = -1;
            if (LEFT != null) {
                LEFT.RIGHT = this;
                LEFT.PREVIOUS = this;
                Minecraft.getInstance().setScreen(LEFT);
                return;
            }
            if (RIGHT != null) {
                RIGHT.LEFT = this;
                RIGHT.PREVIOUS = this;
                Minecraft.getInstance().setScreen(RIGHT);
                return;
            }
        }

        if (minecraft == null || minecraft.level == null) return;
        RandomSource rand = minecraft.level.random;
        boolean isCrashed = TardisClientData.getInstance(minecraft.level.dimension()).isCrashing();

        this.shakeAlpha--;

        if (isCrashed) this.shakeAlpha = 99;
        if (shakeAlpha < 0) shakeAlpha = 0;

        if (shakeAlpha > 0) {
            this.shakeX = (int) (this.shakeAlpha * (Math.random() - 0.5));
            this.shakeY = (int) (this.shakeAlpha * (Math.random() - 0.5));
        }
    }

    public void switchScreenToLeft(MonitorOS next) {
        this.LEFT = next;
        this.RIGHT = null;
        transition();
    }

    public void transition() {
        transitionStartTime = age;
    }

    public void switchToScreen(MonitorOS previous) {
        this.PREVIOUS = previous;
        if (minecraft != null) minecraft.setScreen(this);
    }

    public void setEvents(MonitorOSRun onSubmit, MonitorOSRun onCancel) {
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
    }

    public void addSubmitButton(int x, int y) {
        if (onSubmit != null) {
            SpriteIconButton spriteiconbutton = this.addRenderableWidget(CommonTRWidgets.imageButton(20, Component.translatable("Submit"), (arg) -> {
                this.onSubmit.onPress();
            }, true, BUTTON_LOCATION));
            spriteiconbutton.setPosition(x, y);
        }
    }

    public void addCancelButton(int x, int y) {
        if (onCancel != null) {
            SpriteIconButton spriteiconbutton = this.addRenderableWidget(CommonTRWidgets.imageButton(20, Component.translatable("Cancel"), (arg) -> {
                this.onCancel.onPress();
            }, true, BCK_LOCATION));
            spriteiconbutton.setPosition(x, y);
        }

    }

    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public interface MonitorOSRun {
        void onPress();
    }

    public static class MonitorOSExtension extends MonitorOS {

        public MonitorOSExtension(Component title, ResourceLocation currentShellTheme) {
            super(title);
            this.currentShellTheme = currentShellTheme;
            this.patternCollection = ShellPatterns.getPatternCollectionForTheme(this.currentShellTheme);
            this.themeList = ShellTheme.SHELL_THEME_REGISTRY.keySet().stream().toList();
            generateDummyGlobalShell();
        }

        @Override
        protected void init() {
            if (currentShellTheme == null) this.currentShellTheme = this.themeList.get(0);
            this.pattern = this.patternCollection.get(0);
            super.init();
        }

        public static GlobalShellBlockEntity globalShellBlockEntity;
        public ResourceLocation currentShellTheme;
        public ShellPattern pattern;
        public final List<ResourceLocation> themeList;
        public List<ShellPattern> patternCollection;

        public void renderShell(GuiGraphics guiGraphics, int x, int y, float scale) {
            ShellModel model = ShellModelCollection.getInstance().getShellEntry(this.currentShellTheme).getShellModel(pattern);
            model.setDoorPosition(false);
            Lighting.setupForEntityInInventory();
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate((float) x, y, 100.0F);
            pose.scale(-scale, scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(-15F));
            pose.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() % 5400L) / 15L));

            VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(model.renderType(model.getShellTexture(pattern, false)));
            model.renderShell(globalShellBlockEntity, false, false, pose, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.flush();
            pose.popPose();
            Lighting.setupFor3DItems();
        }

        public static void generateDummyGlobalShell() {
            globalShellBlockEntity = new GlobalShellBlockEntity(BlockPos.ZERO, TRBlockRegistry.GLOBAL_SHELL_BLOCK.get().defaultBlockState());
            assert Minecraft.getInstance().level != null;
            globalShellBlockEntity.setLevel(Minecraft.getInstance().level);
            ResourceKey<Level> generatedLevelKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(TardisRefined.MODID, UUID.randomUUID().toString()));
            globalShellBlockEntity.setTardisId(generatedLevelKey);
            globalShellBlockEntity.setShellTheme(ShellTheme.POLICE_BOX.getId());
            globalShellBlockEntity.setPattern(ShellPatterns.DEFAULT);
        }
    }
}

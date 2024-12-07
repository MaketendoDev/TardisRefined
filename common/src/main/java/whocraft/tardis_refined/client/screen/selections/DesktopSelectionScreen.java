package whocraft.tardis_refined.client.screen.selections;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.client.screen.main.MonitorOS;
import whocraft.tardis_refined.common.network.messages.C2SChangeDesktop;
import whocraft.tardis_refined.common.tardis.TardisDesktops;
import whocraft.tardis_refined.common.tardis.themes.DesktopTheme;
import whocraft.tardis_refined.common.util.MiscHelper;
import whocraft.tardis_refined.constants.ModMessages;
import whocraft.tardis_refined.registry.TRSoundRegistry;

public class DesktopSelectionScreen extends MonitorOS {

    public static ResourceLocation MONITOR_TEXTURE = new ResourceLocation(TardisRefined.MODID, "textures/gui/desktop.png");
    public static ResourceLocation MONITOR_TEXTURE_OVERLAY = new ResourceLocation(TardisRefined.MODID, "textures/gui/desktop_overlay.png");
    public static ResourceLocation previousImage = TardisDesktops.FACTORY_THEME.getPreviewTexture();
    protected int imageWidth = 256;
    protected int imageHeight = 173;
    private DesktopTheme currentDesktopTheme;
    private int leftPos, topPos;

    public DesktopSelectionScreen() {
        super(Component.translatable(ModMessages.UI_DESKTOP_SELECTION));
    }

    public static void selectDesktop(DesktopTheme theme) {
        new C2SChangeDesktop(Minecraft.getInstance().player.level().dimension(), theme).send();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void init() {
        this.setEvents(() -> {
            DesktopSelectionScreen.selectDesktop(currentDesktopTheme);
        }, () -> {
            Minecraft.getInstance().setScreen(null);
        });
        this.currentDesktopTheme = grabDesktop();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        addSubmitButton(width / 2 + 90, (height) / 2 + 35);
        addCancelButton(width / 2 + 40, (height) / 2 + 35);

        super.init();
    }

    private DesktopTheme grabDesktop() {
        for (DesktopTheme desktop : TardisDesktops.getRegistry().values()) {
            return desktop;
        }
        return null;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);

        PoseStack poseStack = guiGraphics.pose();

        /*Render Back drop*/
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(MONITOR_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);


        /*Render Interior Image*/
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(width / 2 - 110, height / 2 - 72, 0);
        poseStack.scale(0.31333333F, 0.31333333F, 0.313333330F);

        guiGraphics.blit(currentDesktopTheme.getPreviewTexture(), 0, 0, 0, 0, 400, 400, 400, 400);

        double alpha = (100.0D - this.age * 3.0D) / 100.0D;
        RenderSystem.enableBlend();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) alpha);
        guiGraphics.blit(previousImage, (int) ((Math.random() * 14) - 2), (int) ((Math.random() * 14) - 2), 400, 400, 400, 400);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) alpha);
        RenderSystem.setShaderTexture(0, NOISE);
        guiGraphics.blit(NOISE, 0, 0, this.shakeX, this.shakeY, 400, 400);
        RenderSystem.disableBlend();
        poseStack.popPose();


        /*Render Back drop*/
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(MONITOR_TEXTURE_OVERLAY, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        super.render(guiGraphics, mouseX, mouseY, partialTick);


    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {

    }

    @Override
    public ObjectSelectionList createSelectionList() {
        int leftPos = width / 2 + 45;
        GenericMonitorSelectionList<SelectionListEntry> selectionList = new GenericMonitorSelectionList<>(this.minecraft, 57, 80, leftPos, this.topPos + 30, this.topPos + this.imageHeight - 60, 12);
        selectionList.setRenderBackground(false);

        for (DesktopTheme desktop : TardisDesktops.getRegistry().values()) {


            Component name = Component.literal(MiscHelper.getCleanName(desktop.getIdentifier().getPath()));

            // Check for if the tellraw name is incomplete, or fails to pass.
            try {
                var json = Component.Serializer.fromJson(new StringReader(desktop.getName()));
                name = json;
            } catch (Exception ex) {
                TardisRefined.LOGGER.error("Could not process Name for datapack desktop " + desktop.getIdentifier().toString());
            }

            selectionList.children().add(new SelectionListEntry(name, (entry) -> {
                previousImage = currentDesktopTheme.getPreviewTexture();
                this.currentDesktopTheme = desktop;

                for (Object child : selectionList.children()) {
                    if (child instanceof SelectionListEntry current) {
                        current.setChecked(false);
                    }
                }
                entry.setChecked(true);
                age = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(TRSoundRegistry.STATIC.get(), (float) Math.random()));
            }, leftPos));
        }

        return selectionList;
    }


}
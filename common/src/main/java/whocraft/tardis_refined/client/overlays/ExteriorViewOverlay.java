package whocraft.tardis_refined.client.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Transformation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.TRKeybinds;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;
import whocraft.tardis_refined.constants.ModMessages;

public class ExteriorViewOverlay {

    public static ResourceLocation IMAGE = new ResourceLocation(TardisRefined.MODID, "textures/gui/external_view.png");


    public static void renderOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();

        TardisPlayerInfo.get(mc.player).ifPresent(tardisPlayerInfo -> {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            if(!tardisPlayerInfo.isViewingTardis()) return;
            if(mc.getDebugOverlay().showDebugScreen()) return;

            Font fontRenderer = mc.font;
            int x = 10;
            int y = 10;

            // Create the message with a keybind
            MutableComponent ascendKey = Component.translatable(TRKeybinds.EXIT_EXTERIOR_VIEW.getDefaultKey().getName());
            MutableComponent message = Component.translatable(ModMessages.EXIT_EXTERNAL_VIEW, ascendKey)
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);

            // Render a semi-transparent background for better text readability
            guiGraphics.fill(x - 5, y - 5, x + fontRenderer.width(message.getString()) + 5, y + 15, 0x88000000);

            // Render the text with a shadow
            MultiBufferSource.BufferSource renderImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            fontRenderer.drawInBatch(
                    message.getString(),
                    x,
                    y,
                    ChatFormatting.WHITE.getColor(),
                    false,
                    Transformation.identity().getMatrix(),
                    renderImpl,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );
            renderImpl.endBatch();

            // Pop pose to reset transformations
            poseStack.popPose();
        });
    }
}
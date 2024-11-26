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
import whocraft.tardis_refined.client.TardisClientData;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;
import whocraft.tardis_refined.common.tardis.control.flight.ThrottleControl;
import whocraft.tardis_refined.common.tardis.manager.TardisPilotingManager;
import whocraft.tardis_refined.constants.ModMessages;

public class ExteriorViewOverlay {

    public static ResourceLocation IMAGE = new ResourceLocation(TardisRefined.MODID, "textures/gui/external_view.png");


    public static void renderOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();

        TardisPlayerInfo.get(mc.player).ifPresent(tardisPlayerInfo -> {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            // Exit if the player is not viewing the TARDIS or the debug screen is active
            if (!tardisPlayerInfo.isViewingTardis()) {
                poseStack.popPose();
                return;
            }

            TardisClientData tardisClientData = TardisClientData.getInstance(
                    tardisPlayerInfo.getPlayerPreviousPos().getDimensionKey()
            );
            int throttleStage = tardisClientData.getThrottleStage();
            int maxThrottleStage = TardisPilotingManager.MAX_THROTTLE_STAGE;
            int throttlePercentage = maxThrottleStage != 0
                    ? (int) ((double) throttleStage / maxThrottleStage * 100)
                    : 0;

            Font fontRenderer = mc.font;
            int x = 10; // X position for the overlay
            int y = 10; // Y position for the overlay

            // Create a translatable component for the exit keybind
            MutableComponent ascendKey = Component.translatable(TRKeybinds.EXIT_EXTERIOR_VIEW.getDefaultKey().getName());
            MutableComponent message = Component.translatable(ModMessages.EXIT_EXTERNAL_VIEW, ascendKey)
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);

            // Display throttle percentage
            MutableComponent throttleMessage = Component.literal("Throttle: " + throttlePercentage + "%")
                    .withStyle(ChatFormatting.GREEN);

            // Render a semi-transparent background for better readability
            int width = Math.max(fontRenderer.width(message.getString()), fontRenderer.width(throttleMessage.getString()));
            guiGraphics.fill(x - 5, y - 5, x + width + 5, y + 30, 0x88000000);

            // Render the text with shadow
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

            // Draw the main message
            fontRenderer.drawInBatch(
                    message.getString(),
                    x,
                    y,
                    ChatFormatting.WHITE.getColor(),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );

            // Draw the throttle percentage below the main message
            fontRenderer.drawInBatch(
                    throttleMessage.getString(),
                    x,
                    y + 15,
                    ChatFormatting.WHITE.getColor(),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );

            bufferSource.endBatch();

            // Reset transformations
            poseStack.popPose();
        });
    }

}
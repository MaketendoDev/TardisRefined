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


            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

            // Render the text
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

            int backdropWidth = fontRenderer.width(message.getString());


            // Only render journey progress if the TARDIS is flying
            if (tardisClientData.isFlying()) {
                float journeyProgress = tardisClientData.getJourneyProgress();
                MutableComponent journeyMessage = Component.literal("Journey Progress: " + String.format("%.2f", journeyProgress) + "%")
                        .withStyle(ChatFormatting.YELLOW);

                 backdropWidth = Math.max(backdropWidth, fontRenderer.width(journeyMessage.getString()));

                // Render a semi-transparent background for better readability
                guiGraphics.fill(x - 5, y - 5, x + backdropWidth + 5, y + 30, 0x88000000);


                // Render extended backdrop for journey progress
                guiGraphics.fill(x - 5, y + 30, x + backdropWidth + 5, y + 60, 0x88000000);

                fontRenderer.drawInBatch(
                        journeyMessage.getString(),
                        x,
                        y + 30,
                        ChatFormatting.WHITE.getColor(),
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        15728880
                );

                // Render the journey progress bar below the journey message
                int progressBarX = x - 5;
                int progressBarY = y + 42;
                int progressBarWidth = backdropWidth + 10; // Match backdrop width
                int progressWidth = (int) (progressBarWidth * journeyProgress / 100.0f);

                // Background of the progress bar
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 10, 0xFF555555); // Gray background
                // Foreground of the progress bar
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 10, 0xFF00FF00);   // Green progress
            } else {
                guiGraphics.fill(x - 5, y - 5, x + backdropWidth + 5, y + 30, 0x88000000);
            }
            bufferSource.endBatch();

            poseStack.popPose();
        });
    }



}
package whocraft.tardis_refined.common.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import whocraft.tardis_refined.common.capability.player.TardisPlayerInfo;

import java.util.Objects;

@Mixin(PlayerRenderer.class)
public class PlayerRenderMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {

       TardisPlayerInfo.get(Minecraft.getInstance().player).ifPresent(tardisPlayerInfo -> {
           if (tardisPlayerInfo.isViewingTardis() && Objects.equals(Minecraft.getInstance().player.getStringUUID(), tardisPlayerInfo.getPlayer().getStringUUID())) {
               ci.cancel();
           }
       });

    }

}

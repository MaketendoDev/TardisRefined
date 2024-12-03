package whocraft.tardis_refined.client.renderer.vortex;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

public class RenderTargetHelper {

    public RenderTarget renderTarget;

    @Environment(EnvType.CLIENT)
    public static boolean getIsStencilEnabled(RenderTarget renderTarget) {
        return ((RenderTargetStencil) renderTarget).tr$getisStencilEnabled();
    }

    @Environment(EnvType.CLIENT)
    public static void setIsStencilEnabled(RenderTarget renderTarget, boolean cond) {
        ((RenderTargetStencil) renderTarget).tr$setisStencilEnabledAndReload(cond);
    }

    public void start() {
        Window window = Minecraft.getInstance().getWindow();

        if (renderTarget != null && (renderTarget.width != window.getWidth() || renderTarget.height != window.getHeight())) {
            renderTarget = null;
        }

        if (renderTarget == null) {
            renderTarget = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
        }

        renderTarget.bindWrite(false);
        renderTarget.checkStatus();

        if (!getIsStencilEnabled(renderTarget)) {
            setIsStencilEnabled(renderTarget, true);
        }
    }

    public void end() {
        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.unbindWrite();
    }


}

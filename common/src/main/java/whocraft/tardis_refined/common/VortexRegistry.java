package whocraft.tardis_refined.common;

import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.renderer.vortex.VortexRenderer;
import whocraft.tardis_refined.common.tardis.themes.Theme;
import whocraft.tardis_refined.registry.DeferredRegistry;
import whocraft.tardis_refined.registry.RegistrySupplierHolder;

public class VortexRegistry implements Theme {

    /**
     * Registry Key for the Vortex registry.
     */
    public static final ResourceKey<Registry<VortexRegistry>> VORTEX_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(TardisRefined.MODID, "vortex"));

    /**
     * Deferred Registry for Vortex entries.
     */
    public static final DeferredRegistry<VortexRegistry> VORTEX_DEFERRED_REGISTRY = DeferredRegistry.createCustom(TardisRefined.MODID, VORTEX_REGISTRY_KEY, true);

    /**
     * Registry instance containing all Vortex entries.
     */
    public static final Registry<VortexRegistry> VORTEX_REGISTRY = VORTEX_DEFERRED_REGISTRY.getRegistry().get();

    // Vortex entries
    public static final RegistrySupplierHolder<VortexRegistry, VortexRegistry> CLOUDS = registerVortex(new ResourceLocation(TardisRefined.MODID,"clouds"), new ResourceLocation(TardisRefined.MODID, "textures/vortex/clouds.png"), 9, 12, 10f, true, true, VortexRenderer.BlueOrngGradient, false);
    public static final RegistrySupplierHolder<VortexRegistry, VortexRegistry> WAVES = registerVortex(new ResourceLocation(TardisRefined.MODID,"waves"), new ResourceLocation(TardisRefined.MODID,"textures/vortex/waves.png"), 9, 12, 20f, true, true, VortexRenderer.BlueOrngGradient, false);
    public static final RegistrySupplierHolder<VortexRegistry, VortexRegistry> STARS = registerVortex(new ResourceLocation(TardisRefined.MODID,"stars"), new ResourceLocation(TardisRefined.MODID,"textures/vortex/stars.png"), 9, 12, 5f, true, true, VortexRenderer.PastelGradient, true);
    public static final RegistrySupplierHolder<VortexRegistry, VortexRegistry> FLOW = registerVortex(new ResourceLocation(TardisRefined.MODID,"flow"), new ResourceLocation(TardisRefined.MODID,"textures/vortex/clouds.png"), 9, 12, 5f, true, true, VortexRenderer.ModernVortex, true);
    public static final RegistrySupplierHolder<VortexRegistry, VortexRegistry> SPACE = registerVortex(new ResourceLocation(TardisRefined.MODID,"space"), new ResourceLocation(TardisRefined.MODID,"textures/vortex/stars_2.png"), 9, 12, 5f, true, true, VortexRenderer.ModernVortex, false);

    private final ResourceLocation texture;
    private final int sides;
    private final int rows;
    private final float twist;
    private final boolean lightning;
    private final boolean decals;
    private final VortexRenderer.VortexGradientTint gradient;
    private final boolean movingGradient;
    private ResourceLocation translationKey;

    public VortexRegistry(ResourceLocation id,ResourceLocation texture, int sides, int rows, float twist, boolean lightning, boolean decals, VortexRenderer.VortexGradientTint gradient, boolean movingGradient) {
        this.texture = texture;
        this.sides = sides;
        this.rows = rows;
        this.twist = twist;
        this.lightning = lightning;
        this.decals = decals;
        this.gradient = gradient;
        this.movingGradient = movingGradient;
        this.translationKey = id;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getSides() {
        return sides;
    }

    public int getRows() {
        return rows;
    }

    public float getTwist() {
        return twist;
    }

    public boolean hasLightning() {
        return lightning;
    }

    public boolean hasDecals() {
        return decals;
    }

    public VortexRenderer.VortexGradientTint getGradient() {
        return gradient;
    }

    public boolean isMovingGradient() {
        return movingGradient;
    }

    private static RegistrySupplierHolder<VortexRegistry, VortexRegistry> registerVortex(ResourceLocation id, ResourceLocation texturePath, int sides, int rows, float twist, boolean lightning, boolean decals, VortexRenderer.VortexGradientTint gradient, boolean movingGradient) {
        return VORTEX_DEFERRED_REGISTRY.registerHolder(id.getPath(), () -> new VortexRegistry(
                id,
                texturePath,
                sides,
                rows,
                twist,
                lightning,
                decals,
                gradient,
                movingGradient
        ));
    }

    @Override
    public String getTranslationKey() {
        return Util.makeDescriptionId("vortex", this.translationKey);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getTranslationKey());
    }
}

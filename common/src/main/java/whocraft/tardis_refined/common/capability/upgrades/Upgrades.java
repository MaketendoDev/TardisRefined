package whocraft.tardis_refined.common.capability.upgrades;

import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.registry.DeferredRegistry;
import whocraft.tardis_refined.registry.RegistrySupplier;

public class Upgrades {

    public static final DeferredRegistry<Upgrade> UPGRADE_DEFERRED_REGISTRY = DeferredRegistry.create(TardisRefined.MODID, Upgrade.UPGRADES.getRegistryKey());

    public static final RegistrySupplier<Upgrade> BASE = UPGRADE_DEFERRED_REGISTRY.register("base", () -> new Upgrade()
            .setCost(0)
            .setPosition(0, 0));

    public static final RegistrySupplier<Upgrade> HEALTH_1 = UPGRADE_DEFERRED_REGISTRY.register("health_1", () -> new Upgrade(BASE)
            .setCost(1)
            .setPosition(0, -1));


}

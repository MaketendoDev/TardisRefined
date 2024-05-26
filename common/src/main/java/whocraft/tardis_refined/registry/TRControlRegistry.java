package whocraft.tardis_refined.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.common.tardis.control.Control;
import whocraft.tardis_refined.common.tardis.control.flight.*;
import whocraft.tardis_refined.common.tardis.control.ship.MonitorControl;
import whocraft.tardis_refined.common.tardis.control.ship.ToggleDoorControl;

public class TRControlRegistry {
	/** Registry Key for the Controls registry. For addon mods, use this as the registry key*/
	public static final ResourceKey<Registry<Control>> CONTROL_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(TardisRefined.MODID, "control"));

	/** Global instance of the Control custom registry created by Tardis Refined*/
	public static final CustomRegistry<Control> CONSOLE_THEMES_REGISTRY = CustomRegistry.create(CONTROL_REGISTRY_KEY);

	/** Tardis Refined instance of the Controls registry. Addon Mods: DO NOT USE THIS, it is only for Tardis Refined use only*/
	public static final DeferredRegistry<Control> CONTROL_DEFERRED_REGISTRY = DeferredRegistry.createCustom(TardisRefined.MODID, CONSOLE_THEMES_REGISTRY);


	// Tardis refined controls
	public static final RegistrySupplier<Control> DOOR_TOGGLE = register(new ToggleDoorControl(new ResourceLocation(TardisRefined.MODID, "door_toggle")));
	public static final RegistrySupplier<Control> X = register(new CoordinateControl(CoordinateButton.X, TardisRefined.MODID));
	public static final RegistrySupplier<Control> Y = register(new CoordinateControl(CoordinateButton.Y, TardisRefined.MODID));
	public static final RegistrySupplier<Control> Z = register(new CoordinateControl(CoordinateButton.Z, TardisRefined.MODID));
	public static final RegistrySupplier<Control> INCREMENT = register(new IncrementControl(new ResourceLocation(TardisRefined.MODID, "increment")));
	public static final RegistrySupplier<Control> ROTATE = register(new RotationControl(new ResourceLocation(TardisRefined.MODID, "rotate")));
	public static final RegistrySupplier<Control> RANDOM = register(new RandomControl(new ResourceLocation(TardisRefined.MODID, "random")));
	public static final RegistrySupplier<Control> THROTTLE = register(new ThrottleControl(new ResourceLocation(TardisRefined.MODID, "throttle")));
	public static final RegistrySupplier<Control> MONITOR = register(new MonitorControl(new ResourceLocation(TardisRefined.MODID, "monitor")));
	public static final RegistrySupplier<Control> DIMENSION = register(new DimensionalControl(new ResourceLocation(TardisRefined.MODID, "dimension")));
	public static final RegistrySupplier<Control> FAST_RETURN = register(new FastReturnControl(new ResourceLocation(TardisRefined.MODID, "fast_return")));
	public static final RegistrySupplier<Control> READOUT = register(new ReadoutControl(new ResourceLocation(TardisRefined.MODID, "read_out")));
	public static final RegistrySupplier<Control> GENERIC_NO_SHOW = register(new GenericControl(new ResourceLocation(TardisRefined.MODID, "generic_no_show"), "control.tardis_refined.generic_control"));
	public static final RegistrySupplier<Control> HANDBRAKE = register(new HandbrakeControl(new ResourceLocation(TardisRefined.MODID, "hand_brake")));
	public static final RegistrySupplier<Control> FUEL = register(new FuelToggleControl(new ResourceLocation(TardisRefined.MODID, "fuel")));

	public static Control get(ResourceLocation id){
		Control potentialTheme = CONSOLE_THEMES_REGISTRY.get(id);
		if(potentialTheme != null){
			return potentialTheme;
		}
		return THROTTLE.get();
	}

	public static ResourceLocation getKey(Control control){
		return CONSOLE_THEMES_REGISTRY.getKey(control);
	}

	/**
	 * Register methods for Tardis Refined only
	 * @return the registered control
	 */
	private static RegistrySupplier<Control> register(Control control) {
		return register(control, control.getId().getPath());
	}
	private static RegistrySupplier<Control> register(Control control, String id) {
		return CONTROL_DEFERRED_REGISTRY.register(id, () -> control);
	}
}


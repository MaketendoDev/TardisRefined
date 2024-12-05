package whocraft.tardis_refined.common.tardis.control.flight;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.entity.Control;
import whocraft.tardis_refined.common.tardis.manager.TardisPilotingManager;
import whocraft.tardis_refined.common.tardis.themes.ConsoleTheme;
import whocraft.tardis_refined.common.util.PlayerUtil;

public class RotationControl extends whocraft.tardis_refined.common.tardis.control.Control {
    public RotationControl(ResourceLocation id) {
        super(id);
    }

    public RotationControl(ResourceLocation id, String langId) {
        super(id, langId);
    }

    @Override
    public boolean onRightClick(TardisLevelOperator operator, ConsoleTheme theme, Control control, Player player) {
        return this.rotateDir(operator, theme, control, player, true);
    }

    @Override
    public boolean onLeftClick(TardisLevelOperator operator, ConsoleTheme theme, Control control, Player player) {
        return this.rotateDir(operator, theme, control, player, false);
    }

    private boolean rotateDir(TardisLevelOperator operator, ConsoleTheme theme, Control control, Player player, boolean clockwise) {
        if (!operator.getLevel().isClientSide()) {
            TardisPilotingManager pilotManager = operator.getPilotingManager();

            Direction dir = pilotManager.getTargetLocation().getDirection();
            pilotManager.getTargetLocation().setDirection(clockwise ? dir.getClockWise() : dir.getCounterClockWise());
            var direction = pilotManager.getTargetLocation().getDirection().getSerializedName();
            PlayerUtil.sendMessage(player, Component.translatable(direction), true);
            return true;
        }
        return false;
    }
}

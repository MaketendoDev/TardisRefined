package whocraft.tardis_refined.common.capability.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TardisPilot {

    void updatePlayerAbilities(ServerPlayer player, Abilities abilities, boolean isWatcher);

    void setupPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator, TardisNavLocation spectateTarget);

    void endPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator);
    UUID getViewedTardis();
    void setViewedTardis(UUID uuid);
    boolean isViewingTardis();
    CompoundTag saveData();
    void loadData(CompoundTag tag);
    Player getPlayer();
    void syncToClients(@Nullable ServerPlayer serverPlayerEntity);

}

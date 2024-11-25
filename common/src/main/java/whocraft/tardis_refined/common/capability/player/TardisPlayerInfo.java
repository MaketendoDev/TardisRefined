package whocraft.tardis_refined.common.capability.player;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import whocraft.tardis_refined.common.blockentity.door.TardisInternalDoor;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.network.messages.player.SyncTardisPlayerInfoMessage;
import whocraft.tardis_refined.common.tardis.TardisArchitectureHandler;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;
import whocraft.tardis_refined.common.util.Platform;
import whocraft.tardis_refined.common.util.TardisHelper;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TardisPlayerInfo implements TardisPilot {

    private Player player;
    private UUID viewedTardis;
    private TardisNavLocation observationPosition;
    private BlockPos playerPreviousPos = BlockPos.ZERO;

    public TardisPlayerInfo(Player player) {
        this.player = player;
    }


    public void setPlayer(Player player) {
        this.player = player;
    }

    public TardisNavLocation getObservationPosition() {
        return observationPosition;
    }

    public void setObservationPosition(TardisNavLocation observationPosition) {
        this.observationPosition = observationPosition;
    }

    @ExpectPlatform
    public static Optional<TardisPlayerInfo> get(LivingEntity player) {
        throw new AssertionError();
    }


    @Override
    public void updatePlayerAbilities(ServerPlayer player, Abilities abilities, boolean isWatcher) {

        if(isWatcher) {
            abilities.mayfly = false;
            abilities.instabuild = false;
            abilities.invulnerable = true;
            abilities.flying = true;

            player.setNoGravity(true);
        } else {
            player.gameMode.getGameModeForPlayer().updatePlayerAbilities(abilities);
            player.setNoGravity(false);
        }
    }

    @Override
    public void setupPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator, TardisNavLocation spectateTarget) {

        // Set the player's viewed TARDIS UUID
        UUID uuid = UUID.fromString(tardisLevelOperator.getLevelKey().location().getPath());

        if(!isViewingTardis()) {
            setPlayerPreviousPos(player.blockPosition());
        }

        setViewedTardis(uuid);

        if (tardisLevelOperator.getPilotingManager().getCurrentLocation() != null) {

            TardisNavLocation sourceLocation = tardisLevelOperator.getPilotingManager().getCurrentLocation();

            TardisHelper.teleportEntityTardis(tardisLevelOperator, player, sourceLocation, spectateTarget, false);
            updatePlayerAbilities(serverPlayer, serverPlayer.getAbilities(), true);
            serverPlayer.onUpdateAbilities();
            syncToClients(null);
        }


    }

    public static void updateTardisForAllPlayers(TardisLevelOperator tardisLevelOperator, TardisNavLocation tardisNavLocation){
        if(Platform.getServer() == null) return;
        Platform.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
            TardisPlayerInfo.get(serverPlayer).ifPresent(tardisPlayerInfo -> {
                if (tardisPlayerInfo.isViewingTardis()) {
                    if (Objects.equals(tardisPlayerInfo.getViewedTardis().toString(), UUID.fromString(tardisLevelOperator.getLevelKey().location().getPath()).toString())) {
                        tardisPlayerInfo.setupPlayerForInspection(serverPlayer, tardisLevelOperator, tardisNavLocation);
                    }
                }
            });
        });
    }

    public BlockPos getPlayerPreviousPos() {
        return playerPreviousPos;
    }

    public void setPlayerPreviousPos(BlockPos playerPreviousPos) {
        this.playerPreviousPos = playerPreviousPos;
    }

    @Override
    public void endPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator) {

        TardisInternalDoor internalDoor = tardisLevelOperator.getInternalDoor();

        BlockPos targetPosition = getPlayerPreviousPos();
        Direction doorDirection = internalDoor != null ? internalDoor.getTeleportRotation() : serverPlayer.getDirection();
        ServerLevel tardisDimensionLevel = serverPlayer.server.getLevel(tardisLevelOperator.getLevelKey());

        TardisNavLocation targetLocation = new TardisNavLocation(targetPosition, doorDirection, tardisDimensionLevel);
        TardisNavLocation sourceLocation = tardisLevelOperator.getPilotingManager().getCurrentLocation();

        TardisHelper.teleportEntityTardis(tardisLevelOperator, serverPlayer, sourceLocation, targetLocation, true);

        updatePlayerAbilities(serverPlayer, serverPlayer.getAbilities(), false);
        serverPlayer.onUpdateAbilities();

        // Clear the viewed TARDIS UUID
        setViewedTardis(null);

        syncToClients(null);
    }

    @Override
    @Nullable
    public UUID getViewedTardis() {
        return viewedTardis;
    }

    @Override
    public void setViewedTardis(@Nullable UUID uuid) {
        this.viewedTardis = uuid;
    }

    @Override
    public boolean isViewingTardis() {
        return viewedTardis != null;
    }

    @Override
    public CompoundTag saveData() {
        CompoundTag tag = new CompoundTag();
        if (viewedTardis != null) {
            tag.putUUID("ViewedTardis", viewedTardis);
        }

        CompoundTag playerPos = NbtUtils.writeBlockPos(playerPreviousPos);
        tag.put("PlayerPos", playerPos);

        return tag;
    }

    @Override
    public void loadData(CompoundTag tag) {

        if(tag.contains("PlayerPos")){
            playerPreviousPos = NbtUtils.readBlockPos(tag.getCompound("PlayerPos"));
        }

        if (tag.hasUUID("ViewedTardis")) {
            this.viewedTardis = tag.getUUID("ViewedTardis");
        } else {
            this.viewedTardis = null;
        }

    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void syncToClients(@Nullable ServerPlayer serverPlayerEntity) {
        if (player != null && player.level().isClientSide)
            throw new IllegalStateException("Don't sync client -> server");

        CompoundTag nbt = saveData();

        SyncTardisPlayerInfoMessage message = new SyncTardisPlayerInfoMessage(this.player.getId(), nbt);
        if (serverPlayerEntity == null) {
            message.sendToAll();
        } else {
            message.send(serverPlayerEntity);
        }
    }
}

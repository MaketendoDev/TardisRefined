package whocraft.tardis_refined.common.capability.player;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.network.messages.player.SyncTardisPlayerInfoMessage;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;
import whocraft.tardis_refined.common.tardis.manager.TardisPilotingManager;
import whocraft.tardis_refined.common.util.Platform;
import whocraft.tardis_refined.common.util.TardisHelper;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TardisPlayerInfo implements TardisPilot {

    private Player player;
    private UUID viewedTardis;
    private TardisNavLocation playerPreviousPos = TardisNavLocation.ORIGIN;
    private boolean renderVortex = false;

    public TardisPlayerInfo(Player player) {
        this.player = player;
    }


    public void setPlayer(Player player) {
        this.player = player;
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
            abilities.mayBuild = false;
            abilities.invulnerable = true;
            abilities.flying = true;
            player.setNoGravity(true);
        } else {
            player.gameMode.getGameModeForPlayer().updatePlayerAbilities(abilities);
            player.setNoGravity(false);
        }
    }

    @Override
    public void setupPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator, TardisNavLocation spectateTarget, boolean timeVortex) {

        // Set the player's viewed TARDIS UUID
        UUID uuid = UUID.fromString(tardisLevelOperator.getLevelKey().location().getPath());

        if(!isViewingTardis()) {
            setPlayerPreviousPos(new TardisNavLocation(player.blockPosition(), Direction.NORTH, tardisLevelOperator.getLevelKey()));
        }

        setViewedTardis(uuid);

        if (spectateTarget != null) {

            TardisNavLocation sourceLocation = tardisLevelOperator.getPilotingManager().getCurrentLocation();

            TardisHelper.teleportEntityTardis(tardisLevelOperator, player, sourceLocation, spectateTarget, false);
            updatePlayerAbilities(serverPlayer, serverPlayer.getAbilities(), true);
            setRenderVortex(timeVortex);
            serverPlayer.onUpdateAbilities();
            syncToClients(null);
        }


    }

    public static void updateTardisForAllPlayers(TardisLevelOperator tardisLevelOperator, TardisNavLocation tardisNavLocation, boolean timeVortex) {
        if(Platform.getServer() == null) return;
        Platform.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
            TardisPlayerInfo.get(serverPlayer).ifPresent(tardisPlayerInfo -> {
                if (tardisPlayerInfo.isViewingTardis()) {
                    if (Objects.equals(tardisPlayerInfo.getViewedTardis().toString(), UUID.fromString(tardisLevelOperator.getLevelKey().location().getPath()).toString())) {
                        tardisPlayerInfo.setupPlayerForInspection(serverPlayer, tardisLevelOperator, tardisNavLocation, timeVortex);
                    }
                }
            });
        });
    }

    public TardisNavLocation getPlayerPreviousPos() {
        return playerPreviousPos;
    }

    public void setPlayerPreviousPos(TardisNavLocation playerPreviousPos) {
        this.playerPreviousPos = playerPreviousPos;
    }

    @Override
    public void endPlayerForInspection(ServerPlayer serverPlayer, TardisLevelOperator tardisLevelOperator) {


        BlockPos targetPosition = getPlayerPreviousPos().getPosition();
        ServerLevel tardisDimensionLevel = serverPlayer.server.getLevel(tardisLevelOperator.getLevelKey());

        TardisNavLocation console = tardisLevelOperator.getPilotingManager().getCurrentLocation();

        TardisNavLocation targetLocation = new TardisNavLocation(targetPosition, Direction.NORTH, tardisDimensionLevel);
        TardisNavLocation sourceLocation = tardisLevelOperator.getPilotingManager().getCurrentLocation();

        TardisHelper.teleportEntityTardis(tardisLevelOperator, serverPlayer, sourceLocation, targetLocation, true);

        updatePlayerAbilities(serverPlayer, serverPlayer.getAbilities(), false);
        serverPlayer.onUpdateAbilities();

        serverPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(console.getPosition().getX(), console.getPosition().getY(), console.getPosition().getZ()));
        setRenderVortex(false);
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

        CompoundTag playerPos = playerPreviousPos.serialise();
        tag.put("TardisPlayerPos", playerPos);

        tag.putBoolean("RenderVortex", renderVortex);

        return tag;
    }

    public boolean isRenderVortex() {
        return renderVortex;
    }

    public void setRenderVortex(boolean renderVortex) {
        this.renderVortex = renderVortex;
        syncToClients(null);
    }

    @Override
    public void loadData(CompoundTag tag) {

        if (tag.contains("TardisPlayerPos")) {
            playerPreviousPos = TardisNavLocation.deserialize(tag.getCompound("TardisPlayerPos"));
        }

        if (tag.hasUUID("ViewedTardis")) {
            this.viewedTardis = tag.getUUID("ViewedTardis");
        } else {
            this.viewedTardis = null;
        }

        renderVortex = tag.getBoolean("RenderVortex");

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

    @Override
    public void tick(TardisLevelOperator tardisLevelOperator, ServerPlayer serverPlayerEntity) {
        TardisPilotingManager pilotManger = tardisLevelOperator.getPilotingManager();
        if(tardisLevelOperator.getLevelKey() == getPlayerPreviousPos().getDimensionKey()) {
            setRenderVortex(pilotManger.isLanding() ||  pilotManger.isTakingOff() || pilotManger.isInFlight());
        }
    }
}

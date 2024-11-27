package whocraft.tardis_refined.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.common.blockentity.console.GlobalConsoleBlockEntity;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.entity.ControlEntity;
import whocraft.tardis_refined.common.tardis.manager.TardisPilotingManager;

import java.util.Optional;

public class FlyTardisAtPOI extends WorkAtPoi {


    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Villager villager) {
        GlobalPos globalPos = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
        TardisLevelOperator tardisLevelOperator = TardisLevelOperator.get(serverLevel).orElse(null);

        return tardisLevelOperator.getPilotingManager().isInFlight() && globalPos.dimension() == serverLevel.dimension();
    }

    @Override
    protected void useWorkstation(ServerLevel serverLevel, Villager villager) {

        TardisLevelOperator.get(serverLevel).ifPresent(tardisLevelOperator -> {
            TardisPilotingManager pilotManager = tardisLevelOperator.getPilotingManager();
            GlobalConsoleBlockEntity console = pilotManager.getCurrentConsole();
            Brain<Villager> brain = villager.getBrain();
            if (console == null) return;

            if (pilotManager.isInFlight()) {
                BlockPos consolePos = console.getBlockPos();
                double distanceToConsoleSqr = consolePos.distToCenterSqr(villager.position().x, villager.position().y, villager.position().z);

                // Ensure the villager is within 2 blocks and not closer than 1 block to the console
                if (distanceToConsoleSqr > 4) { // More than 2 blocks away
                    // Move villager closer to the console
                    villager.moveTo(consolePos.getX() + 0.5, villager.position().y, consolePos.getZ() + 0.5);
                } else if (distanceToConsoleSqr < 1) { // Less than 1 block away
                    // Move villager slightly away from the console
                    villager.moveTo(consolePos.getX() + 1, villager.position().y, consolePos.getZ() + 1);
                }

                for (ControlEntity controlEntity : console.getControlEntityList()) {
                    if (controlEntity.isTickingDown()) {

                        // Adjust bounding box check to ensure proximity, but without intersecting
                        if (controlEntity.level().random.nextBoolean()) {
                            villager.getLookControl().setLookAt(controlEntity.getX(), controlEntity.getY(), controlEntity.getZ());
                            for (int i = 0; i < 5; i++) {
                                controlEntity.realignControl();
                            }
                            villager.setUnhappyCounter(40);
                            System.out.println("Re-aligned: " + controlEntity.getCustomName().getString());
                            return;
                        }
                    }
                }
            }
        });

        super.useWorkstation(serverLevel, villager);
    }



    private static @NotNull Vec3 getOffset(Direction facing) {
        double observePointOffset = 4;

        // Calculate villager position relative to the FACING direction
        Vec3 offset = switch (facing) {
            case NORTH -> new Vec3(0, 0, -observePointOffset); // Stand 1.5 blocks away to the north
            case SOUTH -> new Vec3(0, 0, observePointOffset);  // Stand 1.5 blocks away to the south
            case WEST -> new Vec3(-observePointOffset, 0, 0);  // Stand 1.5 blocks away to the west
            case EAST -> new Vec3(observePointOffset, 0, 0);   // Stand 1.5 blocks away to the east
            default -> Vec3.ZERO;               // Default fallback
        };
        return offset;
    }


    @Override
    protected void start(ServerLevel serverLevel, Villager villager, long l) {
        Brain<Villager> brain = villager.getBrain();
        brain.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, l);
        brain.getMemory(MemoryModuleType.JOB_SITE).ifPresent((globalPos) -> {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(globalPos.pos()));
            villager.moveTo(new Vec3(globalPos.pos().getX(), globalPos.pos().getY(), globalPos.pos().getZ()));
        });

        this.useWorkstation(serverLevel, villager);
    }


    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Villager villager, long l) {
        Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        TardisLevelOperator tardisLevelOperator = TardisLevelOperator.get(serverLevel).orElse(null);
        if (optional.isEmpty()) {
            return false;
        } else {
            GlobalPos globalPos = optional.get();
            return tardisLevelOperator.getPilotingManager().isInFlight() && globalPos.dimension() == serverLevel.dimension() && globalPos.pos().closerToCenterThan(villager.position(), 1.73);
        }
    }
}

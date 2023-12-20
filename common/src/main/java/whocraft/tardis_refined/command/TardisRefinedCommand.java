package whocraft.tardis_refined.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.command.sub.CreateCommand;
import whocraft.tardis_refined.command.sub.InteriorCommand;
import whocraft.tardis_refined.command.sub.LevelCommand;
import whocraft.tardis_refined.command.sub.UpgradesCommand;
import whocraft.tardis_refined.command.sub.export.ExportDesktopCommand;
import whocraft.tardis_refined.common.util.Platform;

public class TardisRefinedCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TardisRefined.MODID).requires(commandSource -> commandSource.hasPermission(Platform.getServer().getOperatorUserPermissionLevel()))
                .then(InteriorCommand.register(dispatcher))
                .then(UpgradesCommand.register(dispatcher))
                .then(Commands.literal("data").then(Commands.literal("export").then(ExportDesktopCommand.register(dispatcher))))
                .then(LevelCommand.register(dispatcher))
                .then(CreateCommand.register(dispatcher)));
    }

}

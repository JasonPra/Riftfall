package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.worldgen.LevelContext;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Preview a full adult and roots without waiting for a sapling to age. Coordinates are the soil block. */
@EventBusSubscriber(modid="riftfall")
public final class LegacyTreeCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("legacytree").requires(source->source.hasPermission(2))
            .then(Commands.argument("soil",BlockPosArgument.blockPos()).executes(context->{
                var source=context.getSource();var pos=BlockPosArgument.getLoadedBlockPos(context,"soil");
                if(!generate(source.getLevel(),pos)){
                    source.sendFailure(Component.literal("Legacy tree needs open space above a soil block."));return 0;
                }
                source.sendSuccess(()->Component.literal("Vermillion Legacy adult planted at "+pos.toShortString()+"."),true);
                return 1;
            })));
    }
    public static boolean generate(ServerLevel level,BlockPos soil) {
        Species species=Species.findSpecies(LegacyTrees.ID);
        if(!species.isValid()||!species.isAcceptableSoil(level.getBlockState(soil)))return false;
        boolean generated=species.generate(new DynamicTreeGenerationContext(LevelContext.create(level),species,soil,soil.mutable(),
            level.getBiome(soil),Direction.NORTH,8,false));
        return generated&&com.dtteam.dynamictrees.tree.TreeHelper.getBranchOpt(level.getBlockState(soil.above()))
            .filter(branch->branch.getFamily()==species.getFamily()).isPresent();
    }
    private LegacyTreeCommands() {}
}

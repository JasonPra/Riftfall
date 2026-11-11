package org.Azkerash.riftfall.worldmana;

import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Read-only diagnostics. Nothing is drawn or synchronized to clients. */
public final class WorldManaCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("riftfall").requires(source->source.hasPermission(2))
            .then(Commands.literal("ambient").then(Commands.literal("inspect").executes(context->{
                var source=context.getSource();var position=net.minecraft.core.BlockPos.containing(source.getPosition());
                var chunk=source.getLevel().getChunkSource().getChunkNow(position.getX()>>4,position.getZ()>>4);
                if(chunk==null){source.sendFailure(Component.literal("This chunk is not loaded."));return 0;}
                var mana=WorldMana.inspect(chunk);var node=LeyLineNetwork.get(source.getLevel()).node(chunk.getPos().x,chunk.getPos().z);
                source.sendSuccess(()->Component.literal(String.format(Locale.ROOT,"Chunk %s: %.3f / %.0f mana; %.2f/s; %d mature crystals; ley node: %s",chunk.getPos(),mana.amount(),mana.capacity(),mana.regenerationPerSecond(),mana.matureCrystals(),node.isPresent()?"yes":"no")),false);return 1;
            })))
            .then(Commands.literal("leylines").then(Commands.literal("list").executes(context->{
                var source=context.getSource();var network=LeyLineNetwork.get(source.getLevel());
                source.sendSuccess(()->Component.literal("Ley-line nodes in "+source.getLevel().dimension().location()+": "+network.size()),false);
                int count=0;for(var node:network.nodes()){
                    if(count++==10)break;
                    source.sendSuccess(()->Component.literal("Chunk ["+node.chunkX()+", "+node.chunkZ()+"] at "+node.x()+", "+node.y()+", "+node.z()+"; parent="+(node.parent()==null?"root":new net.minecraft.world.level.ChunkPos(node.parent()))),false);
                }return network.size();
            }))));
    }
    private WorldManaCommands(){}
}

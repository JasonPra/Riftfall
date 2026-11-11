package org.Azkerash.riftfall.worldmana;

import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Read-only diagnostics for the independent chunk reservoir. */
public final class WorldManaCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("riftfall").requires(source->source.hasPermission(2))
            .then(Commands.literal("ambient").then(Commands.literal("inspect").executes(context->{
                var source=context.getSource();var position=net.minecraft.core.BlockPos.containing(source.getPosition());
                var chunk=source.getLevel().getChunkSource().getChunkNow(position.getX()>>4,position.getZ()>>4);
                if(chunk==null){source.sendFailure(Component.literal("This chunk is not loaded."));return 0;}
                var mana=WorldMana.inspect(chunk);
                source.sendSuccess(()->Component.literal(String.format(Locale.ROOT,"Chunk %s: %.3f / %.0f mana; %.2f/s",chunk.getPos(),mana.amount(),mana.capacity(),mana.regenerationPerSecond())),false);return 1;
            }))));
    }
    private WorldManaCommands(){}
}

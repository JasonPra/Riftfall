package org.Azkerash.riftfall.loot;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.Azkerash.riftfall.*;
@EventBusSubscriber(modid=Riftfall.MODID)
public final class GoblinLootCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("inventory").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("entity",EntityArgument.entity()).executes(c->{
                if(!(EntityArgument.getEntity(c,"entity") instanceof GoblinEntity goblin)){c.getSource().sendFailure(Component.literal("Select a goblin."));return 0;}
                c.getSource().sendSuccess(()->Component.literal(goblin.loot().diagnostics().toString()),false);return 1;
            })))));
        e.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("stash").requires(s->s.hasPermission(2))
            .then(Commands.literal("list").executes(c->{
                var stash=EscapedGoblinStash.get(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("Escaped goblins: "+stash.size()),false);
                stash.ids().stream().limit(100).forEach(id->c.getSource().sendSuccess(()->Component.literal(id.toString()),false));return 1;
            }))
            .then(Commands.literal("inspect").then(Commands.argument("uuid",net.minecraft.commands.arguments.UuidArgument.uuid()).executes(c->{
                var record=EscapedGoblinStash.get(c.getSource().getServer()).record(net.minecraft.commands.arguments.UuidArgument.getUuid(c,"uuid"));
                if(record.isEmpty()){c.getSource().sendFailure(Component.literal("No saved goblin with that UUID."));return 0;}
                c.getSource().sendSuccess(()->Component.literal(record.get().toString()),false);return 1;
            })))));
    }
    private GoblinLootCommands(){}
}

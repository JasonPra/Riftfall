package org.Azkerash.riftfall.stamina;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID)
public final class StaminaCommands {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("stamina").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("target",EntityArgument.entity()).executes(c->{
                if(!(EntityArgument.getEntity(c,"target") instanceof Mob mob)){c.getSource().sendFailure(Component.literal("Target must be a mob"));return 0;}
                c.getSource().sendSuccess(()->Component.literal(MobStamina.uses(mob)?MobStamina.diagnostics(mob).toString():"Undead: stamina exempt"),false);return 1;
            })))));
    }
    private StaminaCommands(){}
}

package org.Azkerash.riftfall.equipment;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.mana.MobMana;
import top.theillusivec4.curios.api.CuriosApi;

/** Operator tools for equipping/test-driving the same inventories that add-ons use. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class MobEquipmentCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("mana").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("entity",EntityArgument.entity()).executes(c->{
                if(!(EntityArgument.getEntity(c,"entity") instanceof Mob mob))return fail(c.getSource(),"Select a mob.");
                MobMana.data(mob);c.getSource().sendSuccess(()->Component.literal("Mana: "+MobMana.diagnostics(mob)),false);return 1;
            })))
            .then(Commands.literal("set").then(Commands.argument("entity",EntityArgument.entity()).then(Commands.argument("amount",DoubleArgumentType.doubleArg(0,1000000)).executes(c->{
                if(!(EntityArgument.getEntity(c,"entity") instanceof Mob mob))return fail(c.getSource(),"Select a mob.");
                MobMana.data(mob).setMana((float)Math.min(MobMana.maximum(mob),DoubleArgumentType.getDouble(c,"amount")));
                c.getSource().sendSuccess(()->Component.literal("Mana: "+MobMana.diagnostics(mob)),false);return 1;
            }))))));
        e.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("equipment").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("entity",EntityArgument.entity()).executes(c->{
                if(!(EntityArgument.getEntity(c,"entity") instanceof Mob mob))return fail(c.getSource(),"Select a mob.");
                CuriosApi.getCuriosInventory(mob).ifPresent(inv->inv.getCurios().forEach((id,slots)->{
                    for(int i=0;i<slots.getSlots();i++){String line=id+"["+i+"]: "+slots.getStacks().getStackInSlot(i);c.getSource().sendSuccess(()->Component.literal(line),false);}
                }));return 1;
            })))
            .then(Commands.literal("curio").then(Commands.argument("entity",EntityArgument.entity())
                .then(Commands.argument("slot",StringArgumentType.word()).then(Commands.argument("index",IntegerArgumentType.integer(0))
                    .then(Commands.argument("item",ItemArgument.item(e.getBuildContext())).executes(c->{
                        if(!(EntityArgument.getEntity(c,"entity") instanceof Mob mob))return fail(c.getSource(),"Select a mob.");
                        var item=ItemArgument.getItem(c,"item").createItemStack(1,false);
                        if(!MobEquipment.equipCurio(mob,StringArgumentType.getString(c,"slot"),IntegerArgumentType.getInteger(c,"index"),item))
                            return fail(c.getSource(),"Slot/index unavailable or item invalid for that Curios slot.");
                        c.getSource().sendSuccess(()->Component.literal("Mob Curios equipment updated."),false);return 1;
                    }))))))));
    }
    private static int fail(net.minecraft.commands.CommandSourceStack source,String message){source.sendFailure(Component.literal(message));return 0;}
    private MobEquipmentCommands(){}
}

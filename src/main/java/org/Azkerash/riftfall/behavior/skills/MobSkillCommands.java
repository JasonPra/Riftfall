package org.Azkerash.riftfall.behavior.skills;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.SkillSlots;

/** Operator tools to equip, inspect and clear any mob's active loadout. */
public final class MobSkillCommands {
    private static SkillSlots slot(String name){
        var slot=SkillSlots.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        if(!MobSkillLoadout.ACTIVE_SLOTS.contains(slot))throw new IllegalArgumentException("Not an active mob skill slot");return slot;
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        var slotArg=Commands.argument("slot",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(
            MobSkillLoadout.ACTIVE_SLOTS.stream().map(s->s.name().toLowerCase(java.util.Locale.ROOT)),b));
        event.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("mobskill").requires(s->s.hasPermission(2))
            .then(Commands.literal("equip").then(Commands.argument("target",EntityArgument.entity()).then(slotArg.then(
                Commands.argument("skill",ResourceLocationArgument.id()).suggests((c,b)->SharedSuggestionProvider.suggestResource(EpicFightRegistries.SKILL.keySet(),b)).executes(c->{
                    if(!(EntityArgument.getEntity(c,"target") instanceof Mob mob)){c.getSource().sendFailure(Component.literal("Target must be a mob"));return 0;}
                    var skill=EpicFightRegistries.SKILL.get(ResourceLocationArgument.getId(c,"skill"));
                    if(skill==null){c.getSource().sendFailure(Component.literal("Unknown skill"));return 0;}
                    try{MobSkillLoadout.equip(mob,slot(StringArgumentType.getString(c,"slot")),skill);}
                    catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
                    c.getSource().sendSuccess(()->Component.literal("Equipped "+skill.getRegistryName()+" on "+mob.getName().getString()),false);return 1;
                })))))
            .then(Commands.literal("clear").then(Commands.argument("target",EntityArgument.entity()).then(Commands.argument("slot",StringArgumentType.word()).executes(c->{
                if(!(EntityArgument.getEntity(c,"target") instanceof Mob mob))return 0;
                try{MobSkillLoadout.clear(mob,slot(StringArgumentType.getString(c,"slot")));}
                catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
                c.getSource().sendSuccess(()->Component.literal("Cleared mob skill slot"),false);return 1;
            }))))
            .then(Commands.literal("inspect").then(Commands.argument("target",EntityArgument.entity()).executes(c->{
                if(!(EntityArgument.getEntity(c,"target") instanceof Mob mob))return 0;
                for(var slot:MobSkillLoadout.ACTIVE_SLOTS){var skill=MobSkillLoadout.equipped(mob,slot);
                    c.getSource().sendSuccess(()->Component.literal(slot.name()+": "+(skill==null?"empty":skill.getRegistryName())+"; ready="+MobSkillLoadout.ready(mob,slot)),false);}
                return 1;
            })))));
    }
    private MobSkillCommands(){}
}

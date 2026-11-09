package org.Azkerash.riftfall.behavior;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.Azkerash.riftfall.Riftfall;

/** Read-only, permission-gated diagnostics. All output goes to the requesting command source. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class BehaviorDebugCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("behavior").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("entity",EntityArgument.entity()).executes(context->{
                var entity=EntityArgument.getEntity(context,"entity");
                if (!(entity instanceof Mob mob) || !mob.getPersistentData().contains("RiftfallBehavior")) {
                    context.getSource().sendFailure(Component.literal("This entity has no active behavior-engine diagnostics."));return 0;
                }
                var data=mob.getPersistentData().getCompound("RiftfallBehavior");var details=data.getCompound("ConfidenceDetails");
                java.util.function.Consumer<String> out=line->context.getSource().sendSuccess(()->Component.literal(line),false);
                out.accept("Behavior: "+data.getString("Action")+" | confidence "+data.getDouble("Confidence")+" | points "+data.getDouble("Points")
                    +" | bold at "+details.getDouble("BoldPoints")+" | normalize by "+details.getDouble("NormalizationPoints"));
                out.accept("Snapshot tick "+data.getLong("ServerTick")+" | observer "+details.getString("ObserverType"));
                out.accept("Health: "+details.getFloat("Health")+" / "+details.getFloat("MaxHealth")+" | main hand: "+details.getString("MainHand")+" | offhand: "+details.getString("OffHand"));
                out.accept("Active effects: "+details.get("ActiveEffects"));
                var factors=details.getCompound("ActiveFactors");
                factors.getAllKeys().stream().sorted().forEach(key->{ var f=factors.getCompound(key);out.accept("Factor "+key+": "+f.getDouble("Input")+" x "+f.getDouble("Weight")+" = "+f.getDouble("Points")); });
                out.accept("Allies: "+details.get("Allies"));out.accept("Excluded candidates: "+details.get("ExcludedCandidates"));out.accept("Witnessed deaths: "+details.get("WitnessedDeaths"));
                out.accept("Witness tuning: radius "+details.getDouble("WitnessRadius")+", duration "+details.getInt("WitnessDurationTicks")+" ticks, cap "+details.getInt("WitnessMaxStacks"));
                out.accept("Situation: "+data.getCompound("Signals"));
                var behaviors=data.getCompound("BehaviorEvaluations");
                behaviors.getAllKeys().stream().sorted().forEach(key->out.accept("Behavior "+key+": "+behaviors.getCompound(key)));
                out.accept("Switch margin: "+data.getDouble("SwitchMargin")+" | previous: "+data.getString("PreviousAction"));
                out.accept("Target memory: "+data.getLong("TargetMemoryRemainingTicks")+" ticks | last seen: "+data.get("LastSeenPosition"));
                out.accept("All configured factor weights: "+details.getCompound("FactorWeights"));
                out.accept("All configured ally weights: "+details.getCompound("UnitWeights"));
                return 1;
            })))));
    }
    private BehaviorDebugCommands() {}
}

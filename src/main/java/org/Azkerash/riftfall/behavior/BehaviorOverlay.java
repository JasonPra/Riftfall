package org.Azkerash.riftfall.behavior;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID)
public final class BehaviorOverlay {
    private static final String ENABLED="RiftfallBehaviorOverlay";
    public static int toggle(ServerPlayer player,Boolean enabled) {
        boolean state=enabled==null?!player.getPersistentData().getBoolean(ENABLED):enabled;
        player.getPersistentData().putBoolean(ENABLED,state);
        if (!state) send(player,List.of());
        player.sendSystemMessage(Component.literal("Behavior holograms "+(state?"ON (nearby engine entities)":"OFF")));return 1;
    }
    private static void send(ServerPlayer player,List<BehaviorOverlayPayload.Entry> entries) {
        PacketDistributor.sendToPlayer(player,new BehaviorOverlayPayload(entries));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount()%5!=0) return;
        for (var player:event.getServer().getPlayerList().getPlayers()) if (player.getPersistentData().getBoolean(ENABLED)) update(player);
    }
    private static void update(ServerPlayer player) {
        if (!player.createCommandSourceStack().hasPermission(2)) { player.getPersistentData().remove(ENABLED);send(player,List.of());return; }
        var mobs=player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(32),
            mob->mob.isAlive() && mob.distanceToSqr(player)<=32*32 && (mob.getPersistentData().contains("RiftfallBehavior") || org.Azkerash.riftfall.stamina.MobStamina.uses(mob) || org.Azkerash.riftfall.mana.MobMana.active(mob)));
        mobs.sort(Comparator.comparingDouble(mob->mob.distanceToSqr(player)));
        var entries=new ArrayList<BehaviorOverlayPayload.Entry>();
        for (var mob:mobs.stream().limit(32).toList()) {
            var data=mob.getPersistentData().getCompound("RiftfallBehavior").copy();
            var stamina=org.Azkerash.riftfall.stamina.MobStamina.diagnostics(mob);data.put("Stamina",stamina);
            data.put("Mana",org.Azkerash.riftfall.mana.MobMana.diagnostics(mob));
            if(!data.contains("Action"))data.putString("Action",stamina.getBoolean("Panting")?"recover stamina":"normal AI");
            entries.add(new BehaviorOverlayPayload.Entry(mob.getId(),mob.getUUID(),lines(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath()+" #"+mob.getId(),data)));
        }
        send(player,entries);
    }
    public static List<String> lines(String name,CompoundTag data) {
        var lines=new ArrayList<String>();var details=data.getCompound("ConfidenceDetails");
        lines.add(name+" | "+data.getString("Action"));
        if(data.contains("Confidence"))lines.add(String.format(Locale.ROOT,"Confidence %.0f%% | %.1f pts | advance %.1f",data.getDouble("Confidence")*100,data.getDouble("Points"),details.getDouble("EngagePoints")));
        if(data.getCompound("Stamina").contains("Current")){
            var stamina=data.getCompound("Stamina");
            int filled=(int)Math.round(10*stamina.getDouble("Current")/Math.max(1,stamina.getDouble("Maximum")));
            lines.add("Stamina ["+"|".repeat(Math.clamp(filled,0,10))+".".repeat(10-Math.clamp(filled,0,10))+"] "+String.format(Locale.ROOT,"%.1f/%.0f | regen x%.2f%s",stamina.getDouble("Current"),stamina.getDouble("Maximum"),stamina.getDouble("RegenMultiplier"),stamina.getBoolean("Panting")?" | PANTING":""));
            lines.add("Stamina: "+stamina.getString("LastExpense")+" | regen delay "+stamina.getInt("DelayTicks"));
        }
        if(data.getCompound("Mana").contains("Current")){
            var mana=data.getCompound("Mana");
            lines.add(String.format(Locale.ROOT,"Mana %.1f/%.0f | regen %.2f/s (x%.2f) | delay %dt",mana.getDouble("Current"),mana.getDouble("Maximum"),mana.getDouble("RegenPerSecond"),mana.getDouble("RegenMultiplier"),mana.getInt("DelayTicks")));
            lines.add("Mana: "+mana.getString("LastExpense"));
            var costs=mana.getCompound("SpellCosts");costs.getAllKeys().stream().sorted().forEach(key->lines.add(key+": "+costs.getInt(key)+" mana"));
        }
        for (String key:List.of("SelfCombatStats","VisibleTargetCombatStats")) if (details.contains(key)) {
            var stats=details.getCompound(key);
            lines.add(String.format(Locale.ROOT,"%s: dmg %.1f armor %.0f tough %.0f hp %.1f",key.equals("SelfCombatStats")?"Self":"Foe",
                stats.getDouble("AttackDamage"),stats.getDouble("Armor"),stats.getDouble("Toughness"),stats.getDouble("Health")));
        }
        var contributions=data.getCompound("Contributions");
        contributions.getAllKeys().stream().sorted().forEach(key->{
            double points=contributions.getDouble(key);
            lines.add((points<0?"\u00a7c":"\u00a7a")+key.replace("factor.","")+String.format(Locale.ROOT," %+.1f",points));
        });
        var notes=data.getCompound("Notes");notes.getAllKeys().stream().sorted().forEach(key->lines.add(key+": "+notes.getString(key)));
        var signals=data.getCompound("Signals");if (signals.getDouble("healing.circle_available")>0)
            lines.add(String.format(Locale.ROOT,"Healing circle %.1fs | %s",signals.getDouble("healing.remaining_ticks")/20,
                signals.getDouble("healing.in_circle")>0?"healing inside":"moving to heal"));
        if(signals.getDouble("panic.active")>0)lines.add(signals.getDouble("panic.escape_allowed")==0?"Panic: scared; escape blocked in rift":
            signals.getDouble("panic.burrowing")>0?String.format(Locale.ROOT,"Burrowing %.0f%% | threat gap %.1f",signals.getDouble("panic.progress")*100,signals.getDouble("panic.distance")):"Panic: fleeing to safe dig ground");
        var memories=details.getList("WitnessedDeaths",10);
        for (int i=0;i<memories.size();i++) lines.add(String.format(Locale.ROOT,"Loss memory %.1fs",memories.getCompound(i).getLong("RemainingTicks")/20D));
        if (data.getLong("TargetMemoryRemainingTicks")>0 && data.getCompound("Signals").getDouble("target.visible")==0)
            lines.add(String.format(Locale.ROOT,"Last seen %.1fs",data.getLong("TargetMemoryRemainingTicks")/20D));
        var bounded=new ArrayList<>(lines.subList(0,Math.min(48,lines.size())));
        if (lines.size()>48) bounded.set(47,"... "+(lines.size()-47)+" more: use behavior inspect");
        return bounded.stream().map(line->line.length()>256?line.substring(0,256):line).toList();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { event.getEntity().getPersistentData().remove(ENABLED); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { event.getEntity().getPersistentData().remove(ENABLED); }
    private BehaviorOverlay() {}
}

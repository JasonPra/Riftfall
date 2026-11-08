package org.Azkerash.riftfall;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import org.Azkerash.riftfall.RiftData.Rift;

final class RiftMobs {
    private static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("riftfall", "excluded_rift_mobs"));
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    record Spawn(Mob mob, int cost) {}

    static boolean allowedId(String id) {
        return !id.startsWith("irons_spellbooks:") && !RiftEcology.BOSS_ONLY.contains(id) && !id.equals("minecraft:silverfish") && !id.equals("minecraft:endermite")
            && (!id.contains("wither") || id.equals("minecraft:wither_skeleton"))
            && !id.contains("guardian") && !id.contains("dragon") && !id.contains("boss") && !id.contains("warden");
    }

    static boolean eligible(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String path = id.getPath();
        if (type.is(EXCLUDED) || type.is(BOSSES) || !allowedId(id.toString()) || path.contains("guardian")
            || path.contains("dragon") || path.contains("boss") || path.contains("warden")) return false;
        return type.getCategory() == MobCategory.MONSTER;
    }

    static int threat(EntityType<?> type) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (id.equals("riftfall:goblin_archer")) return 1;
        if (id.equals("riftfall:goblin_shaman")) return 3;
        if (id.startsWith("irons_spellbooks:")) return 4;
        if (type == EntityType.RAVAGER || type == EntityType.GHAST || type == EntityType.EVOKER) return 4;
        if (type == EntityType.PIGLIN_BRUTE || type == EntityType.SHULKER || type == EntityType.VEX || type == EntityType.VINDICATOR) return 3;
        if (type == EntityType.CREEPER || type == EntityType.ENDERMAN || type == EntityType.WITCH || type == EntityType.BLAZE
            || type == EntityType.HOGLIN || type == EntityType.ZOGLIN || type == EntityType.PHANTOM
            || type == EntityType.SLIME || type == EntityType.MAGMA_CUBE || type == EntityType.WITHER_SKELETON) return 2;
        return 1;
    }

    static Spawn spawn(ServerLevel world, BlockPos pos, int level, double budget, Rift r) { return spawn(world,pos,level,budget,r,false); }

    static Spawn spawn(ServerLevel world, BlockPos pos, int level, double budget, Rift r, boolean goblinsOnly) {
        List<EntityType<?>> pool = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
            if (eligible(type) && (!goblinsOnly || BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("riftfall")) && RiftEcology.matches(type, r) && threat(type) <= level && threat(type) * 10 <= budget) pool.add(type);
        if (pool.isEmpty()) return null;
        for (int attempt = 0; attempt < 8; attempt++) {
            int total=pool.stream().mapToInt(t->RiftEcology.weight(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(),r)).sum();
            int roll=world.random.nextInt(total);
            EntityType<?> type=pool.getFirst();
            for (var candidate : pool) {
                roll-=RiftEcology.weight(BuiltInRegistries.ENTITY_TYPE.getKey(candidate).toString(),r);
                if (roll<0) { type=candidate; break; }
            }
            var entity = type.create(world);
            if (!(entity instanceof Mob mob)) continue;
            mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,world.random.nextFloat()*360,0);
            EventHooks.finalizeMobSpawn(mob,world,world.getCurrentDifficultyAt(pos),MobSpawnType.TRIGGERED,null);
            if (!world.noCollision(mob)) continue;
            mob.setPersistenceRequired(); mob.setCanPickUpLoot(false);
            if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
            if (mob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin hoglin) hoglin.setImmuneToZombification(true);
            mob.getPersistentData().putBoolean("RiftfallPaid",true);
            hostile(mob);
            RiftProgression.equip(mob,level);
            RiftSpellcasting.prepare(mob, level);
            if (world.addFreshEntity(mob)) return new Spawn(mob, threat(type)*10);
        }
        return null;
    }

    static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) if (eligible(type)) {
            if (!event.has(type,yesman.epicfight.registry.entries.EpicFightAttributes.IMPACT))
                event.add(type,yesman.epicfight.registry.entries.EpicFightAttributes.IMPACT,.5);
            if (!event.has(type,yesman.epicfight.registry.entries.EpicFightAttributes.STUN_ARMOR))
                event.add(type,yesman.epicfight.registry.entries.EpicFightAttributes.STUN_ARMOR,0);
        }
    }

    static void team(ServerLevel world, Mob mob, int id) {
        mob.getPersistentData().putInt("RiftfallOwner",id);
        var scoreboard = world.getScoreboard();
        String name = "rift_"+id;
        var team = scoreboard.getPlayerTeam(name);
        if (team == null) team = scoreboard.addPlayerTeam(name);
        team.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(mob.getStringUUID(),team);
    }

    static void hostile(Mob mob) {
        mob.targetSelector.addGoal(0,new NearestAttackableTargetGoal<>(mob,Player.class,true));
    }

    static Mob find(ServerLevel world,Rift r,java.util.UUID id) {
        var entity = world.getEntity(id);
        if (entity instanceof Mob mob && !mob.isRemoved()) return mob;
        Mob pending = r.spawned.get(id);
        return pending != null && !pending.isRemoved() ? pending : null;
    }

    static void control(ServerLevel world, Rift r) {
        for (var id : java.util.Set.copyOf(r.enemies)) {
            Mob mob = find(world,r,id);
            if (mob == null) continue;
            if (id.equals(r.boss) && !RiftBosses.inRoom(r, mob.getX(), mob.getZ())) mob.moveTo(RiftBosses.position(r).getX()+.5,81,RiftBosses.position(r).getZ()+.5,mob.getYRot(),0);
            if (r.boss != null && !id.equals(r.boss) && RiftBosses.inRoom(r,mob.getX(),mob.getZ())
                && !mob.getPersistentData().getBoolean("RiftfallBossMinion")) mob.moveTo(r.arena().getX()+.5,81,2.5,mob.getYRot(),0);
            double margin = Math.max(1,mob.getBbWidth()/2+.2);
            if (Math.abs(mob.getX()-(r.arena().getX()+.5))>r.radius-margin || Math.abs(mob.getZ()-.5)>r.radius-margin
                || mob.getY()<81 || mob.getY()+mob.getBbHeight()>87) {
                mob.moveTo(r.arena().getX()+.5,81,.5,mob.getYRot(),0);
                mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
            var target = world.players().stream().filter(p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && r.contains(p.getX(),p.getZ()))
                .min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(mob))).orElse(null);
            if (target != null) {
                mob.setTarget(target);
                if (mob instanceof NeutralMob neutral) { neutral.setPersistentAngerTarget(target.getUUID()); neutral.setRemainingPersistentAngerTime(1200); }
            }
        }
    }
    private RiftMobs() {}
}

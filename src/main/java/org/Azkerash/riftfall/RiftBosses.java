package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;
import org.Azkerash.riftfall.RiftData.Rift;

final class RiftBosses {
    static final int COST = 200;
    static boolean isBoss(EntityType<?> type) { return RiftEcology.BOSS_ONLY.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()); }
    static String species(String theme) {
        return theme.equals("frozen") ? "irons_spellbooks:ice_spider"
            : java.util.Set.of("woodland","temperate","swamp").contains(theme) ? "irons_spellbooks:archevoker" : "";
    }
    static boolean canBuild(Rift r,int level,double surplus) { return level >= 5 && surplus >= COST && !species(r.theme).isEmpty(); }
    static BlockPos position(Rift r) { return r.arena().offset(0,1,-r.radius+5); }
    static boolean inRoom(Rift r,double x,double z) { return r.contains(x,z) && z < -r.radius/3; }
    static void buildRoom(ServerLevel world,Rift r) {
        int wall = -r.radius/3;
        for (int x=-r.radius+1;x<r.radius;x++) for (int z=-r.radius+1;z<=wall;z++) for (int y=0;y<7;y++) {
            var block = y==0 ? Blocks.POLISHED_BASALT : z==wall && (Math.abs(x)>1 || y>3) ? Blocks.CHISELED_STONE_BRICKS : Blocks.AIR;
            world.setBlockAndUpdate(r.arena().offset(x,y,z), block.defaultBlockState());
        }
    }
    static void spawn(ServerLevel world,Rift r) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(species(r.theme)));
        if (!(type.create(world) instanceof Mob mob)) return;
        BlockPos p=position(r);
        mob.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);
        EventHooks.finalizeMobSpawn(mob,world,world.getCurrentDifficultyAt(p),MobSpawnType.TRIGGERED,null);
        if (!world.noCollision(mob)) { mob.discard(); return; }
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean("RiftfallPaid",true);
        mob.getPersistentData().putBoolean("RiftfallBoss",true);
        RiftMobs.team(world,mob,r.id); RiftMobs.hostile(mob);
        // These encounters already have boss-scale statistics. Do not multiply them
        // by the normal monster growth formula a second time.
        r.boss=mob.getUUID();
        if (world.addFreshEntity(mob)) {
            r.energy-=COST; r.enemies.add(r.boss); r.spawned.put(r.boss,mob); mob.addTag("riftfall_rift_"+r.id);
        } else r.boss=null;
    }
    static void clearBar(Rift r) { if (r.bossBar!=null) { r.bossBar.removeAllPlayers(); r.bossBar=null; } }
    static void tick(ServerLevel world,Rift r) {
        Mob boss = r.boss==null ? null : RiftMobs.find(world,r,r.boss);
        if (r.dormant || r.cleared || boss==null || !boss.isAlive()) { clearBar(r); return; }
        if (r.bossBar==null) r.bossBar=new ServerBossEvent(boss.getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        r.bossBar.setProgress(Math.max(0,Math.min(1,boss.getHealth()/boss.getMaxHealth())));
        for (var player : java.util.List.copyOf(r.bossBar.getPlayers()))
            if (!player.isAlive() || player.level()!=world || !inRoom(r,player.getX(),player.getZ())) r.bossBar.removePlayer(player);
        for (var player : world.players()) if (player.isAlive() && !player.isSpectator() && inRoom(r,player.getX(),player.getZ())) r.bossBar.addPlayer(player);
    }
    private RiftBosses() {}
}

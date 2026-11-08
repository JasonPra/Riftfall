package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import org.Azkerash.riftfall.RiftData.Rift;

final class RiftDungeons {
    static boolean build(ServerLevel world, Rift r) {
        int level = r.level();
        while (level > 1 && RiftProgression.minimumCost(Config.ENEMIES.get(),level) > r.energy) level--;
        if (RiftProgression.minimumCost(Config.ENEMIES.get(),level) > r.energy) return false;
        r.encounterLevel = level;
        r.boss = null; RiftBosses.clearBar(r);
        boolean bossRoom = RiftBosses.canBuild(r,level,r.energy-RiftProgression.minimumCost(Config.ENEMIES.get(),level));
        int count = RiftProgression.enemies(Config.ENEMIES.get(),level);
        r.radius = RiftProgression.radius(count);
        BlockPos c = r.arena();
        RiftEvents.forceArena(world,r,true);
        r.energy -= RiftProgression.fixedCost(Config.ENEMIES.get(),level);
        simpleArena(world,r,false);
        if (bossRoom) RiftBosses.buildRoom(world,r);
        int bossReserve = bossRoom ? RiftBosses.COST : 0;
        r.layoutVersion=2;
        for (int i=0;i<count;i++) {
            for (int attempt=0;attempt<96;attempt++) {
                BlockPos pos=c.offset(world.random.nextInt(r.radius*2-5)-r.radius+3,1,world.random.nextInt(r.radius*2-5)-r.radius+3);
                if (!RiftEvents.safe(world,pos) || (bossRoom && pos.getZ() <= -r.radius/3)) continue;
                // Reserve the minimum cost of all remaining monsters, then spend the
                // surplus on more expensive species if the rift can afford them.
                double budget=r.energy-(count-i-1)*10-bossReserve;
                var spawned=RiftMobs.spawn(world,pos,level,budget,r,r.theme.equals("plains") && level==1 && i<(count*2+2)/3);
                if (spawned == null) continue;
                r.energy-=spawned.cost();
                RiftMobs.team(world,spawned.mob(),r.id);
                spawned.mob().addTag("riftfall_rift_"+r.id);
                r.enemies.add(spawned.mob().getUUID());
                r.spawned.put(spawned.mob().getUUID(),spawned.mob());
                break;
            }
        }
        if (bossRoom) RiftBosses.spawn(world,r);
        if (r.enemies.isEmpty()) {
            r.energy += RiftProgression.fixedCost(Config.ENEMIES.get(),level);
            RiftEvents.forceArena(world,r,false);
            return false;
        }
        r.entityGraceTicks=200; r.emptyEnemyTicks=0;
        r.built=true;
        return true;
    }
    static void simpleArena(ServerLevel world,Rift r,boolean preserveRewards) {
        BlockPos center=r.arena();
        for (int x=-r.radius;x<=r.radius;x++) for (int z=-r.radius;z<=r.radius;z++) for (int y=0;y<=7;y++) {
            BlockPos pos=center.offset(x,y,z);
            if (preserveRewards && world.getBlockState(pos).is(Blocks.CHEST)) continue;
            if (world.getBlockEntity(pos) instanceof Container container) container.clearContent();
            boolean shell=y==0 || y==7 || Math.abs(x)==r.radius || Math.abs(z)==r.radius;
            world.setBlock(pos,(shell ? (y==7 ? Blocks.SEA_LANTERN : Blocks.BEDROCK) : Blocks.AIR).defaultBlockState(),3);
        }
    }
    static void migrate(ServerLevel world,Rift r) {
        if (!r.built || r.dormant || r.layoutVersion>=2) return;
        simpleArena(world,r,true);
        if (!r.cleared && r.boss!=null && r.theme.equals("frozen")) RiftBosses.buildRoom(world,r);
        r.layoutVersion=2;
    }
    private RiftDungeons() {}
}

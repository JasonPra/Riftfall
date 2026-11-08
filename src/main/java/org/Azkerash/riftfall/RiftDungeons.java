package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
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
        int traps = RiftProgression.traps(level);
        r.energy -= RiftProgression.fixedCost(Config.ENEMIES.get(),level);
        for (int x=-r.radius;x<=r.radius;x++) for (int z=-r.radius;z<=r.radius;z++) for (int y=0;y<=7;y++) {
            BlockPos pos = c.offset(x,y,z);
            if (world.getBlockEntity(pos) instanceof Container container) container.clearContent();
            boolean shell = y==0 || y==7 || Math.abs(x)==r.radius || Math.abs(z)==r.radius;
            boolean partition = count>6 && y>0 && y<5
                && ((Math.abs(x)==r.radius/3 && Math.abs(z)>1 && Math.abs(Math.abs(z)-r.radius/2)>1)
                    || (z==0 && Math.abs(x)>r.radius/3+2 && Math.abs(x)<r.radius-2));
            world.setBlock(pos,(shell ? (y==7 ? Blocks.SEA_LANTERN : Blocks.BEDROCK) : partition ? Blocks.STONE_BRICKS : Blocks.AIR).defaultBlockState(),3);
        }
        if (bossRoom) RiftBosses.buildRoom(world,r);
        int bossReserve = bossRoom ? RiftBosses.COST : 0;
        for (int i=0;i<traps;i++) {
            int side = i%2==0 ? -1 : 1;
            Direction facing = side<0 ? Direction.EAST : Direction.WEST;
            BlockPos pos=trapPosition(c,r.radius,i);
            world.setBlockAndUpdate(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,facing));
            if (world.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) dispenser.setItem(0,new ItemStack(Items.ARROW,48));
            world.setBlockAndUpdate(pos.relative(facing),Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        }
        for (int i=0;i<count;i++) {
            for (int attempt=0;attempt<96;attempt++) {
                BlockPos pos=c.offset(world.random.nextInt(r.radius*2-5)-r.radius+3,1,world.random.nextInt(r.radius*2-5)-r.radius+3);
                if (!RiftEvents.safe(world,pos) || (bossRoom && pos.getZ() <= -r.radius/3)) continue;
                // Reserve the minimum cost of all remaining monsters, then spend the
                // surplus on more expensive species if the rift can afford them.
                double budget=r.energy-(count-i-1)*10-bossReserve;
                var spawned=RiftMobs.spawn(world,pos,level,budget,r);
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
    static BlockPos trapPosition(BlockPos center,int radius,int index) {
        int side=index%2==0 ? -1 : 1;
        int row=index/2;
        return center.offset(side*(radius/3+1),1,row==0 ? -radius/2 : row==1 ? radius/2 : 0);
    }
    private RiftDungeons() {}
}

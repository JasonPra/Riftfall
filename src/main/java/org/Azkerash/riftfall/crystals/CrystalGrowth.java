package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.Azkerash.riftfall.worldmana.WorldMana;
import org.Azkerash.riftfall.worldmana.geysers.*;

public final class CrystalGrowth {
    private static boolean loaded(ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    public static BlockState seedState(ServerLevel level,BlockPos target,CrystalTier tier,Direction preferred){
        if(target.getY()<level.getMinBuildHeight()||target.getY()>=level.getMaxBuildHeight()||!loaded(level,target))return null;
        var old=level.getBlockState(target);boolean water=old.getFluidState().is(FluidTags.WATER);
        if(!old.isAir()&&!(old.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock&&water))return null;
        List<Direction> faces=new ArrayList<>();faces.add(preferred);for(var d:Direction.values())if(d!=preferred)faces.add(d);
        for(var facing:faces){
            if(!loaded(level,target.relative(facing.getOpposite())))continue;
            var state=ManaCrystals.block(tier).defaultBlockState().setValue(ManaCrystalBlock.FACING,facing).setValue(ManaCrystalBlock.WATERLOGGED,water);
            if(state.canSurvive(level,target))return state;
        }return null;
    }
    public static boolean spread(ServerLevel level,BlockPos pos,BlockState source,RandomSource random){
        if(!ManaCrystalBlock.adult(source))return false;
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null||!CrystalRules.growth(WorldMana.inspect(chunk).amount())||random.nextDouble()>=CrystalRules.spreadChance(source.getValue(ManaCrystalBlock.WATERLOGGED)))return false;
        var directions=new ArrayList<>(List.of(Direction.values()));Collections.rotate(directions,random.nextInt(6));
        var tier=((ManaCrystalBlock)source.getBlock()).tier().seedTier(random.nextDouble());
        for(var d:directions){
            var target=pos.relative(d);var seed=seedState(level,target,tier,source.getValue(ManaCrystalBlock.FACING));if(seed==null)continue;
            if(!WorldMana.tryExtract(chunk,CrystalRules.cost()))return false;
            if(level.setBlock(target,seed,Block.UPDATE_ALL))return true;
            WorldMana.add(chunk,CrystalRules.cost());
        }return false;
    }
    private static boolean underground(LevelChunk chunk,BlockPos pos){
        return pos.getY()>chunk.getMinBuildHeight()&&pos.getY()<chunk.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX()&15,pos.getZ()&15)-8;
    }
    public static int seedDeposit(ServerLevel level,LevelChunk chunk,GeyserRegion field,boolean initial){
        var cp=chunk.getPos();if(!field.crystalZone(cp.x,cp.z)||!initial&&!CrystalRules.nucleation(WorldMana.inspect(chunk).amount()))return 0;
        var random=RandomSource.create(level.getSeed()^cp.toLong()^0x4352595354414C53L);
        BlockPos anchor=null;int seen=0;var cursor=new BlockPos.MutableBlockPos();var origin=cp.getWorldPosition();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int top=Math.min(level.getMaxBuildHeight()-1,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-8);
            for(int y=level.getMinBuildHeight()+2;y<top;y++){
                cursor.set(origin.getX()+x,y,origin.getZ()+z);if(!chunk.getBlockState(cursor).isAir())continue;
                var host=cursor.below();var stone=chunk.getBlockState(host);
                if(stone.is(ManaCrystals.SUPPORTS)&&stone.isFaceSturdy(level,host,Direction.UP)&&random.nextInt(++seen)==0)anchor=cursor.immutable();
            }
        }
        if(anchor==null)return 0;
        double cost=initial?0:CrystalRules.cost();if(!WorldMana.tryExtract(chunk,cost))return 0;
        int target=initial?CrystalRules.depositSize():1,adults=initial?Math.max(1,target*3/4):0,placed=0;
        long distance=Math.max(Math.abs((long)cp.x-field.centerChunkX()),Math.abs((long)cp.z-field.centerChunkZ()));
        double inward=(field.halfWidth()-distance)/(double)Math.max(1,field.halfWidth()-Math.max(1,field.halfWidth()/3)-1);
        double power=field.totalOutput()/field.chunkCount()/5.0;
        var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();queue.add(anchor);
        while(!queue.isEmpty()&&visited.size()<1024&&placed<target){
            var pos=queue.removeFirst();if(!visited.add(pos)||pos.getX()>>4!=cp.x||pos.getZ()>>4!=cp.z||!underground(chunk,pos))continue;
            var tier=initial?CrystalTier.natural(inward,power,random.nextDouble(),CrystalRules.naturalWhite()):CrystalTier.RED;
            var state=seedState(level,pos,tier,Direction.UP);if(state==null)continue;
            int layers=placed<adults?8:initial?1+random.nextInt(7):1;
            if(level.setBlock(pos,state.setValue(ManaCrystalBlock.LAYERS,layers),Block.UPDATE_ALL)){
                placed++;for(var d:Direction.values())queue.addLast(pos.relative(d));
            }
        }
        if(placed==0)WorldMana.add(chunk,cost);return placed;
    }
    private CrystalGrowth(){}
}

package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Grounded fruit uses the ordinary species sapling and never consumes a whole dropped stack. */
@EventBusSubscriber(modid="riftfall")
public final class HeartwoodFruitFall {
    public static final int PLANT_TICKS=2400;
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        if(event.getEntity() instanceof ItemEntity item&&item.tickCount%20==0)settle(item,20);
    }
    public static boolean settle(ItemEntity item,int ticks){
        if(!(item.level() instanceof ServerLevel level)||item.isRemoved()||!item.getItem().is(LegacyWood.FRUIT.get()))return false;
        var data=item.getPersistentData();
        if(!item.onGround()||item.isInWater()||item.isInLava()||item.getDeltaMovement().horizontalDistanceSqr()>.0025){data.remove("HeartwoodGroundTicks");data.remove("HeartwoodGroundAt");return false;}
        var target=item.blockPosition();long anchor=target.asLong();
        int elapsed=data.contains("HeartwoodGroundAt")&&data.getLong("HeartwoodGroundAt")==anchor?data.getInt("HeartwoodGroundTicks"):0;
        elapsed=(int)Math.min(PLANT_TICKS,(long)elapsed+Math.max(0,ticks));data.putLong("HeartwoodGroundAt",anchor);data.putInt("HeartwoodGroundTicks",elapsed);
        if(elapsed<PLANT_TICKS||(!level.getBlockState(target).canBeReplaced()||level.getBlockState(target).is(net.minecraft.tags.BlockTags.SAPLINGS)||level.getBlockState(target).getBlock() instanceof com.dtteam.dynamictrees.block.sapling.DynamicSaplingBlock))return false;
        var species=Species.findSpecies(LegacyTrees.ID);
        if(!species.isValid()||!species.canSaplingGrowNaturally(level,target)||!species.plantSapling(level,target,false))return false;
        var fruit=item.getItem().copy();fruit.shrink(1);data.remove("HeartwoodGroundTicks");data.remove("HeartwoodGroundAt");
        if(fruit.isEmpty())item.discard();else item.setItem(fruit);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,target.getX()+.5,target.getY()+.25,target.getZ()+.5,5,.18,.15,.18,0);
        return true;
    }
    private HeartwoodFruitFall(){}
}

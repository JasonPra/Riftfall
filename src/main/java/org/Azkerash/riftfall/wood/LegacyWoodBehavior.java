package org.Azkerash.riftfall.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="riftfall")
public final class LegacyWoodBehavior {
    public static final int FAILED_STRIKE_WEAR=4;
    static void deflect(Player player,InteractionHand hand,BlockPos pos){
        if(player.level().isClientSide())return;
        player.level().playSound(null,pos,SoundEvents.ANVIL_HIT,SoundSource.BLOCKS,.6F,.7F);
        var stack=player.getItemInHand(hand);
        if(stack.is(ItemTags.AXES))stack.hurtAndBreak(FAILED_STRIKE_WEAR,player,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
    }
    @SubscribeEvent public static void hit(PlayerInteractEvent.LeftClickBlock event){
        var state=event.getLevel().getBlockState(event.getPos());var player=event.getEntity();
        if(!(state.getBlock() instanceof LegacyLogBlock)||player.isCreative()||LegacyLogBlock.canHarvest(player.getMainHandItem(),state))return;
        event.setCanceled(true);
        if(event.getAction()==PlayerInteractEvent.LeftClickBlock.Action.START)deflect(player,InteractionHand.MAIN_HAND,event.getPos());
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event){
        if(event.getState().getBlock() instanceof LegacyLogBlock&&!event.getPlayer().isCreative()&&!LegacyLogBlock.canHarvest(event.getPlayer().getMainHandItem(),event.getState()))event.setCanceled(true);
    }
    static void sink(ItemEntity item){
        if(!item.getItem().is(LegacyWood.LOG_ITEMS)||!item.isInWater())return;
        var velocity=item.getDeltaMovement();item.setDeltaMovement(velocity.x,Math.min(velocity.y,-.06),velocity.z);
    }
    @SubscribeEvent public static void density(EntityTickEvent.Post event){if(event.getEntity() instanceof ItemEntity item)sink(item);}
    private LegacyWoodBehavior(){}
}

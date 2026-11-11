package org.Azkerash.riftfall.loot;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import org.Azkerash.riftfall.*;

/** 36 storage slots plus worn equipment. Items move between them; equipped items are never mirrored. */
public final class GoblinLoot {
    public static final int STORAGE_SLOTS=36;
    private final GoblinEntity mob;
    private final SimpleContainer inventory=new SimpleContainer(STORAGE_SLOTS);
    private final Set<EquipmentSlot> pickedEquipment=EnumSet.noneOf(EquipmentSlot.class);
    private final Set<UUID> owners=new HashSet<>();
    private boolean dirty=true;
    public GoblinLoot(GoblinEntity mob){this.mob=mob;inventory.addListener(c->dirty=true);}
    public SimpleContainer inventory(){return inventory;}
    public void changed(){dirty=true;}
    public boolean hasLoot(){return !inventory.isEmpty() || pickedEquipment.stream().anyMatch(s->!mob.getItemBySlot(s).isEmpty());}
    private EquipmentSlot upgradeSlot(ItemStack stack){
        var slot=GoblinEquipmentPolicy.slot(mob,stack);if(slot==null)return null;
        var current=mob.getItemBySlot(slot);
        return !EnchantmentHelper.has(current,EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            && GoblinEquipmentPolicy.rank(mob,stack,slot).compareTo(GoblinEquipmentPolicy.rank(mob,current,slot))>0?slot:null;
    }
    private boolean canSwap(){
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        return !BackstabSequence.captured(mob) && (patch==null||!patch.getEntityState().inaction());
    }
    public boolean canPickUp(ItemStack stack){return !mob.isBurrowing() && !mob.isFullyBuried() && (inventory.canAddItem(stack)||canSwap()&&upgradeSlot(stack)!=null);}
    public void pickUp(ItemEntity entity){
        if(mob.level().isClientSide||entity.isRemoved()||entity.hasPickUpDelay()||!canPickUp(entity.getItem()))return;
        var original=entity.getItem();int before=original.getCount();
        if(!inventory.canAddItem(original)){
            var slot=upgradeSlot(original);if(slot==null||!canSwap())return;
            var equipped=original.copyWithCount(1);var old=mob.getItemBySlot(slot);mob.setItemSlot(slot,equipped);mob.setDropChance(slot,0);pickedEquipment.add(slot);storeOrDrop(old);
            mob.onItemPickup(entity);mob.take(entity,1);original.shrink(1);if(original.isEmpty())entity.discard();else entity.setItem(original);
            if(entity.getOwner() instanceof net.minecraft.world.entity.player.Player owner)owners.add(owner.getUUID());mob.setPersistenceRequired();return;
        }
        var remainder=inventory.addItem(original.copy());int accepted=before-remainder.getCount();
        if(accepted<=0)return;
        if(entity.getOwner() instanceof net.minecraft.world.entity.player.Player owner)owners.add(owner.getUUID());
        mob.onItemPickup(entity);mob.take(entity,accepted);
        if(remainder.isEmpty())entity.discard();else entity.setItem(remainder);
        mob.setPersistenceRequired();dirty=true;
    }
    private void storeOrDrop(ItemStack stack){
        if(stack.isEmpty())return;var remaining=inventory.addItem(stack.copy());if(!remaining.isEmpty())mob.spawnAtLocation(remaining);
    }
    public void tick(){
        if(mob.level().isClientSide||!mob.isAlive())return;
        if(mob.tickCount%20==0 && !GoblinEquipmentPolicy.permittedMainhand(mob,mob.getMainHandItem())){
            var invalid=mob.getMainHandItem();mob.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);pickedEquipment.remove(EquipmentSlot.MAINHAND);storeOrDrop(invalid);dirty=true;
        }
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        if(!dirty||mob.isBurrowing()||BackstabSequence.captured(mob)||patch!=null && patch.getEntityState().inaction())return;
        dirty=false;
        for(int i=0;i<inventory.getContainerSize();i++){
            var candidate=inventory.getItem(i);if(candidate.isEmpty())continue;
            var slot=GoblinEquipmentPolicy.slot(mob,candidate);if(slot==null)continue;
            var current=mob.getItemBySlot(slot);
            if(EnchantmentHelper.has(current,EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))continue;
            if(GoblinEquipmentPolicy.rank(mob,candidate,slot).compareTo(GoblinEquipmentPolicy.rank(mob,current,slot))<=0)continue;
            var equipped=inventory.removeItem(i,1);
            // The vacated inventory slot normally receives the old weapon/armor. Unstackable gear frees a slot.
            storeOrDrop(current);mob.setItemSlot(slot,equipped);mob.setDropChance(slot,0);pickedEquipment.add(slot);mob.setPersistenceRequired();
        }
    }
    /** Called independently of normal randomized loot; clear slots before producing each drop. */
    public void dropAll(){
        for(var slot:List.copyOf(pickedEquipment)){
            var stack=mob.getItemBySlot(slot);mob.setItemSlot(slot,ItemStack.EMPTY);if(!stack.isEmpty())mob.spawnAtLocation(stack);
        }
        pickedEquipment.clear();
        for(var stack:inventory.removeAllItems())if(!stack.isEmpty())mob.spawnAtLocation(stack);
        owners.clear();
    }
    public void save(CompoundTag tag){
        var saved=ContainerHelper.saveAllItems(new CompoundTag(),inventory.getItems(),mob.registryAccess());
        var equipped=new ListTag();pickedEquipment.forEach(s->equipped.add(StringTag.valueOf(s.getName())));saved.put("PickedEquipment",equipped);
        var sources=new ListTag();owners.forEach(id->{var source=new CompoundTag();source.putUUID("Player",id);sources.add(source);});saved.put("Owners",sources);
        tag.put("RiftfallInventory",saved);
    }
    public void load(CompoundTag tag){
        inventory.clearContent();pickedEquipment.clear();owners.clear();
        if(tag.contains("RiftfallInventory")){
            var saved=tag.getCompound("RiftfallInventory");ContainerHelper.loadAllItems(saved,inventory.getItems(),mob.registryAccess());
            var equipped=saved.getList("PickedEquipment",8);for(int i=0;i<equipped.size();i++)try{var slot=EquipmentSlot.byName(equipped.getString(i));pickedEquipment.add(slot);mob.setDropChance(slot,0);}catch(IllegalArgumentException ignored){}
            var sources=saved.getList("Owners",10);for(int i=0;i<sources.size();i++)if(sources.getCompound(i).hasUUID("Player"))owners.add(sources.getCompound(i).getUUID("Player"));
        }
        dirty=true;
    }
    public CompoundTag diagnostics(){
        var d=new CompoundTag();d.putInt("Capacity",STORAGE_SLOTS);int used=0;for(var stack:inventory.getItems())if(!stack.isEmpty())used++;
        d.putInt("UsedSlots",used);d.putBoolean("HasPickedUpLoot",hasLoot());save(d);return d;
    }
}

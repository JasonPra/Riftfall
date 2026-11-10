package org.Azkerash.riftfall.equipment;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/** Make attribute-based equipment effects available even to mobs without an Epic Fight patch. */
public final class MobEquipment {
    public static void attributes(EntityAttributeModificationEvent event){
        for(var type:event.getTypes()){
            for(var attribute:EpicFightAttributes.REGISTRY.getEntries())if(!event.has(type,attribute))event.add(type,attribute);
            if(!event.has(type,Attributes.ATTACK_DAMAGE))event.add(type,Attributes.ATTACK_DAMAGE,1);
            if(!event.has(type,Attributes.ATTACK_SPEED))event.add(type,Attributes.ATTACK_SPEED,4);
        }
    }
    /** Normal Curios validation and next-tick hooks apply; does not bypass item restrictions. */
    public static boolean equipCurio(net.minecraft.world.entity.Mob mob,String slot,int index,net.minecraft.world.item.ItemStack stack){
        if(mob.level().isClientSide)return false;
        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(mob).orElse(null);
        if(inventory==null)return false;var handler=inventory.getStacksHandler(slot).orElse(null);
        if(handler==null||index<0||index>=handler.getSlots())return false;
        var context=new top.theillusivec4.curios.api.SlotContext(slot,mob,index,false,true);
        if(!stack.isEmpty() && (!top.theillusivec4.curios.api.CuriosApi.isStackValid(context,stack) || top.theillusivec4.curios.api.CuriosApi.getCurio(stack).map(c->!c.canEquip(context)).orElse(false)))return false;
        handler.getStacks().setStackInSlot(index,stack.copy());return true;
    }
    private MobEquipment(){}
}

package org.Azkerash.riftfall.loot;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import org.Azkerash.riftfall.GoblinEntity;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.*;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Equipment restrictions use Epic Fight categories/parents and single-wield styles, including add-ons. */
public final class GoblinEquipmentPolicy {
    public static final TagKey<Item> DENIED=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("riftfall","goblin_forbidden_weapons"));
    public static final TagKey<Item> ONE_HANDED=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("riftfall","goblin_one_handed_weapons"));
    public static boolean forbiddenCategory(WeaponCategory category){
        var seen=Collections.newSetFromMap(new IdentityHashMap<WeaponCategory,Boolean>());var queue=new ArrayDeque<WeaponCategory>();queue.add(category);
        while(!queue.isEmpty()){
            var c=queue.remove();if(!seen.add(c))continue;
            if(c==WeaponCategories.GREATSWORD||c==WeaponCategories.LONGSWORD||c==WeaponCategories.TACHI||c==WeaponCategories.SPEAR||c==WeaponCategories.TRIDENT)return true;
            String name=c.toString().toLowerCase(Locale.ROOT),compact=name.replace("_","");
            if(List.of("greatsword","polearm","twohand").stream().anyMatch(compact::contains)
                ||Arrays.stream(name.split("[^a-z0-9]+")).anyMatch(Set.of("halberd","glaive","pike","lance","scythe","spear","trident","longsword","tachi")::contains))return true;
            queue.addAll(c.getParents());
        }
        return false;
    }
    public static boolean oneHanded(GoblinEntity mob,ItemStack stack){
        if(stack.isEmpty()||stack.is(DENIED)||stack.getItem() instanceof ProjectileWeaponItem||stack.getItem() instanceof TridentItem)return false;
        var capability=EpicFightCapabilities.getItemStackCapability(stack);
        if(forbiddenCategory(capability.getWeaponCategory()))return false;
        if(stack.is(ONE_HANDED))return true;
        if(!(capability instanceof WeaponCapability weapon))return false;
        var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);if(patch==null)return false;
        var style=weapon.getNaturalSingleWieldStyle(patch);
        if(style==null)style=weapon.getStyle(patch);
        return style!=null && style.canUseOffhand();
    }
    public static boolean permittedMainhand(GoblinEntity mob,ItemStack stack){
        return stack.isEmpty() || mob.isArcher() && stack.getItem() instanceof BowItem && !stack.is(DENIED) || oneHanded(mob,stack);
    }
    public static EquipmentSlot slot(GoblinEntity mob,ItemStack stack){
        if(stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot()!=EquipmentSlot.BODY)return armor.getEquipmentSlot();
        if(stack.getItem() instanceof ShieldItem && !mob.isRanged())return EquipmentSlot.OFFHAND;
        if(mob.isArcher())return stack.getItem() instanceof BowItem && !stack.is(DENIED)?EquipmentSlot.MAINHAND:null;
        return oneHanded(mob,stack)?EquipmentSlot.MAINHAND:null;
    }
    private static double attribute(GoblinEntity mob,ItemStack stack,EquipmentSlot slot,Holder<Attribute> type){
        var instance=new AttributeInstance(type,a->{});instance.setBaseValue(mob.getAttribute(type)==null?type.value().getDefaultValue():mob.getAttributeBaseValue(type));
        stack.forEachModifier(slot,(key,modifier)->{if(key.equals(type))instance.addOrUpdateTransientModifier(modifier);});
        if(slot==EquipmentSlot.MAINHAND && EpicFightCapabilities.getItemStackCapability(stack) instanceof WeaponCapability weapon){
            var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
            if(patch!=null){
                var style=weapon.getNaturalSingleWieldStyle(patch);if(style==null)style=weapon.getStyle(patch);
                var modifiers=weapon.getDamageAttributesInCondition(style);
                if(modifiers!=null && modifiers.get(type)!=null)instance.addOrUpdateTransientModifier(modifiers.get(type));
            }
        }
        return instance.getValue();
    }
    public static EquipmentRank rank(GoblinEntity mob,ItemStack stack,EquipmentSlot slot){
        if(stack.isEmpty())return new EquipmentRank(-1,0,0);
        double enchants=0;for(var entry:stack.getEnchantments().entrySet())enchants+=entry.getIntValue()*(entry.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE)?-10:1);
        double durability=stack.isDamageableItem()?1-(double)stack.getDamageValue()/Math.max(1,stack.getMaxDamage()):1;
        double value;
        if(slot.isArmor()){
            value=attribute(mob,stack,slot,Attributes.ARMOR)*4+attribute(mob,stack,slot,Attributes.ARMOR_TOUGHNESS)
                +attribute(mob,stack,slot,Attributes.KNOCKBACK_RESISTANCE)*2;
        }else if(stack.getItem() instanceof ShieldItem)value=1+Math.log1p(stack.getMaxDamage())*.1;
        else if(stack.getItem() instanceof BowItem){
            var power=mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER);
            value=2+EnchantmentHelper.getItemEnchantmentLevel(power,stack);
        }else {
            value=attribute(mob,stack,slot,Attributes.ATTACK_DAMAGE);
            var sharpness=mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            int level=EnchantmentHelper.getItemEnchantmentLevel(sharpness,stack);if(level>0)value+=.5+.5*level;
        }
        if(mob.isShaman()){
            value+=attribute(mob,stack,slot,io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA)*.02
                +attribute(mob,stack,slot,io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER)*8
                +attribute(mob,stack,slot,io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN)*4;
        }
        return new EquipmentRank(value,enchants,durability);
    }
    private GoblinEquipmentPolicy(){}
}

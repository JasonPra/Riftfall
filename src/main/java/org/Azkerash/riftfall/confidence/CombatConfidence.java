package org.Azkerash.riftfall.confidence;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

/** Generic Minecraft combat inputs. Contains no mob-specific AI or entity-type checks. */
public final class CombatConfidence {
    public enum Loadout {
        UNARMED, SHIELD_ONLY, SWORD, SWORD_SHIELD, BOW, BOW_SHIELD, STAFF, STAFF_SHIELD, OTHER_WEAPON, OTHER_WEAPON_SHIELD;
        public String factor() { return "equipment."+name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final TagKey<Item> STAVES=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("riftfall","confidence/staves"));
    public static final TagKey<Item> WEAPONS=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("riftfall","confidence/weapons"));
    private static boolean shield(ItemStack item) { return item.getItem() instanceof ShieldItem; }
    public static Loadout loadout(ItemStack mainhand,ItemStack offhand) {
        boolean shield=shield(mainhand) || shield(offhand);
        ItemStack weapon=mainhand.isEmpty() || shield(mainhand) ? offhand : mainhand;
        if (weapon.is(STAVES)) return shield?Loadout.STAFF_SHIELD:Loadout.STAFF;
        if (weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem) return shield?Loadout.BOW_SHIELD:Loadout.BOW;
        if (weapon.is(ItemTags.SWORDS) || weapon.getItem() instanceof SwordItem) return shield?Loadout.SWORD_SHIELD:Loadout.SWORD;
        if (weapon.is(WEAPONS) || weapon.getItem() instanceof TieredItem || weapon.getItem() instanceof TridentItem || weapon.getItem() instanceof MaceItem)
            return shield?Loadout.OTHER_WEAPON_SHIELD:Loadout.OTHER_WEAPON;
        return shield?Loadout.SHIELD_ONLY:Loadout.UNARMED;
    }
    /** Presence-based penalties: each distinct effect counts once, regardless of amplifier or duration. */
    public static Map<String,Double> selfStatusFactors(boolean poisoned,boolean withered,int otherHarmfulEffects) {
        if (otherHarmfulEffects<0) throw new IllegalArgumentException("Negative harmful effect count");
        Map<String,Double> factors=new LinkedHashMap<>();
        if (poisoned) factors.put("self.poison",1D);
        if (withered) factors.put("self.wither",1D);
        if (otherHarmfulEffects>0) factors.put("self.harmful_effect",(double)otherHarmfulEffects);
        return factors;
    }
    public static Map<String,Double> factors(LivingEntity actor,LivingEntity target,double woundedFraction,double vulnerableFraction) {
        Map<String,Double> factors=new LinkedHashMap<>();
        factors.put("base",1D);
        factors.put(loadout(actor.getMainHandItem(),actor.getOffhandItem()).factor(),1D);
        if (actor.getHealth()<actor.getMaxHealth()*woundedFraction) factors.put("self.low_health",1D);
        int otherHarmful=(int)actor.getActiveEffects().stream().filter(effect->
            effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL
                && !effect.getEffect().equals(MobEffects.POISON) && !effect.getEffect().equals(MobEffects.WITHER)).count();
        factors.putAll(selfStatusFactors(actor.hasEffect(MobEffects.POISON),actor.hasEffect(MobEffects.WITHER),otherHarmful));
        if (target!=null && target.isAlive()) {
            factors.putAll(CombatStrength.factors(CombatStrength.stats(actor),CombatStrength.stats(target)));
            if (target.getHealth()<=target.getMaxHealth()*vulnerableFraction) factors.put("target.low_health",1D);
            if (target.hasEffect(MobEffects.POISON)) factors.put("target.poison",1D);
            if (target.hasEffect(MobEffects.WITHER)) factors.put("target.wither",1D);
        }
        return factors;
    }
    private CombatConfidence() {}
}

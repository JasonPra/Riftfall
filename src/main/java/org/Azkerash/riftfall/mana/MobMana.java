package org.Azkerash.riftfall.mana;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.Azkerash.riftfall.Riftfall;

/** Server-authoritative resource API, independent of AI. Native Iron's casters retain their MagicData. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class MobMana {
    private static ManaState state(Mob mob){return ((ManaUser)mob).manaState();}
    public static boolean active(Mob mob){return ((ManaUser)mob).hasManaState();}
    private static MagicData raw(Mob mob){return mob instanceof IMagicEntity caster?caster.getMagicData():MagicData.getPlayerMagicData(mob);}
    public static MagicData data(Mob mob){
        var state=state(mob);var magic=raw(mob);if(mob.level().isClientSide)return magic;var attribute=mob.getAttribute(AttributeRegistry.MAX_MANA);
        if(!(mob instanceof IMagicEntity) && attribute!=null){
            double base=ManaConfig.profile(mob).maximum();
            if(Double.isNaN(state.configuredBase)){
                if(attribute.getBaseValue()==AttributeRegistry.MAX_MANA.value().getDefaultValue())attribute.setBaseValue(base);
                state.configuredBase=base;
            }else if(base!=state.configuredBase){
                if(attribute.getBaseValue()==state.configuredBase)attribute.setBaseValue(base);
                state.configuredBase=base;
            }
        }
        if(!state.initialized){state.initialized=true;if(!(mob instanceof IMagicEntity))magic.setMana((float)maximum(mob));}
        return magic;
    }
    public static double maximum(Mob mob){return mob.getAttributeValue(AttributeRegistry.MAX_MANA);}
    public static double regeneration(Mob mob){return ManaConfig.profile(mob).regeneration()*mob.getAttributeValue(AttributeRegistry.MANA_REGEN);}
    public static boolean canCast(Mob mob,AbstractSpell spell,int level){return spell.isEnabled()&&state(mob).budget.canSpend(Math.min(data(mob).getMana(),maximum(mob)),spell.getManaCost(level));}
    /** Charge once at release, after pre-cast conditions pass. Interrupted wind-ups spend no mana. */
    public static boolean spend(Mob mob,AbstractSpell spell,int level){
        var magic=data(mob);magic.setMana((float)Math.clamp(magic.getMana(),0,maximum(mob)));var state=state(mob);int cost=spell.getManaCost(level);
        if(!spell.isEnabled()||!state.budget.canSpend(magic.getMana(),cost))return false;
        magic.setMana((float)state.budget.spend(magic.getMana(),cost,ManaConfig.delay()));
        state.lastExpense=spell.getSpellId()+" level "+level+" (-"+cost+")";return true;
    }
    public static boolean hurtInterrupts(Mob mob,AbstractSpell spell){
        return mob.hurtTime>0 && !(spell.getCastType()==io.redspace.ironsspellbooks.api.spells.CastType.LONG
            && ((io.redspace.ironsspellbooks.item.curios.CurioBaseItem)io.redspace.ironsspellbooks.registries.ItemRegistry.CONCENTRATION_AMULET.get()).isEquippedBy(mob));
    }
    public static int windup(Mob mob,AbstractSpell spell,int level){
        return Math.max(1,(int)Math.round(20D*spell.getEffectiveCastTime(level,mob)/Math.max(1,spell.getCastTime(level))));
    }
    public static int cooldown(Mob mob,int ticks){return Math.max(1,(int)Math.ceil(ticks*Math.max(0,2-io.redspace.ironsspellbooks.api.util.Utils.softCapFormula(mob.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION)))));}
    public static CompoundTag diagnostics(Mob mob){
        var d=new CompoundTag();if(!active(mob))return d;var magic=data(mob);
        if(!Double.isNaN(state(mob).configuredBase))d.putDouble("ConfiguredBase",state(mob).configuredBase);
        d.putDouble("Current",magic.getMana());d.putDouble("Maximum",maximum(mob));d.putDouble("RegenPerSecond",regeneration(mob));
        d.putDouble("RegenMultiplier",mob.getAttributeValue(AttributeRegistry.MANA_REGEN));d.putInt("DelayTicks",state(mob).budget.delay());d.putString("LastExpense",state(mob).lastExpense);
        var costs=mob.getPersistentData().getCompound("RiftfallManaCosts");d.put("SpellCosts",costs.copy());return d;
    }
    public static void describe(Mob mob,AbstractSpell spell,int level){
        var costs=mob.getPersistentData().getCompound("RiftfallManaCosts");costs.putInt(spell.getSpellId()+" L"+level,spell.getManaCost(level));mob.getPersistentData().put("RiftfallManaCosts",costs);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide||!mob.isAlive()||!active(mob))return;
        var magic=data(mob);boolean casting=mob.getPersistentData().getBoolean("RiftfallCasting") || mob instanceof IMagicEntity caster && caster.isCasting();
        magic.setMana((float)state(mob).budget.tick(magic.getMana(),maximum(mob),regeneration(mob),casting));
        if(mob.tickCount%5==0)mob.getPersistentData().put("RiftfallMana",diagnostics(mob));
    }
    public static void save(Mob mob,CompoundTag tag){if(active(mob))tag.put("RiftfallManaPool",diagnostics(mob));}
    public static void load(Mob mob,CompoundTag tag){
        if(!tag.contains("RiftfallManaPool"))return;var saved=tag.getCompound("RiftfallManaPool");if(saved.contains("ConfiguredBase"))state(mob).configuredBase=saved.getDouble("ConfiguredBase");var magic=data(mob);
        magic.setMana((float)Math.clamp(saved.getDouble("Current"),0,1000000));state(mob).budget.restoreDelay(saved.getInt("DelayTicks"));state(mob).lastExpense=saved.getString("LastExpense");
    }
    private MobMana(){}
}

package org.Azkerash.riftfall.stamina;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;

/** Shared stamina API. All mobs inherit a pool; undead retain unlimited resource behavior. */
public final class MobStamina {
    /** Keep the configured base while applying native Epic Fight equipment modifiers in their normal order. */
    public static double maximum(Mob mob){
        var a=mob.getAttribute(yesman.epicfight.registry.entries.EpicFightAttributes.MAX_STAMINA);
        if(a==null)return StaminaConfig.maximum();
        var data=mob.getPersistentData();double previous=data.contains("RiftfallStaminaBase")?data.getDouble("RiftfallStaminaBase"):15;
        if(a.getBaseValue()==previous){
            double configured=StaminaConfig.maximum();if(a.getBaseValue()!=configured)a.setBaseValue(configured);
            if(!data.contains("RiftfallStaminaBase")||data.getDouble("RiftfallStaminaBase")!=configured)data.putDouble("RiftfallStaminaBase",configured);
        }
        // Curios reapplies transient equipment modifiers on its first post-tick after loading.
        return Math.max(a.getValue(),data.getDouble("RiftfallStaminaLoadingMaximum"));
    }
    public static double regeneration(Mob mob){
        var a=mob.getAttribute(yesman.epicfight.registry.entries.EpicFightAttributes.STAMINA_REGEN);
        return StaminaConfig.regeneration()*(a==null?1:a.getValue());
    }
    public static boolean uses(Mob mob){return mob instanceof StaminaUser && !mob.getType().is(EntityTypeTags.UNDEAD);}
    public static StaminaUser user(Mob mob){return (StaminaUser)mob;}
    public static boolean manual(Mob mob){return user(mob).managesStaminaActions();}
    public static void beginSkill(Mob mob){if(uses(mob))user(mob).staminaState().budget().beginSkill();}
    public static void endSkill(Mob mob){if(uses(mob))user(mob).staminaState().budget().endSkill();}
    public static boolean automaticContact(Mob mob,String action){
        if(!uses(mob))return true;if(panting(mob))return false;if(manual(mob))return true;
        var budget=user(mob).staminaState().budget();long now=mob.level().getGameTime();
        if(budget.covered(now)||budget.sameContactTick(now))return true;
        if(!spend(mob,action))return false;budget.contact(now);return true;
    }
    public static boolean automaticAnimation(Mob mob,yesman.epicfight.api.animation.types.AttackAnimation animation){
        if(!uses(mob))return true;if(panting(mob))return false;if(manual(mob))return true;
        var budget=user(mob).staminaState().budget();long now=mob.level().getGameTime();
        // A supported active skill already paid its own explicit cost.
        if(!budget.skillCovered() && !spend(mob,"swipe"))return false;
        budget.animation(now,(int)Math.ceil(animation.getTotalTime()*20)+4);return true;
    }
    public static boolean fastMovement(Mob mob){
        return mob.isSprinting() || !manual(mob) && !mob.getNavigation().isDone() && mob.getMoveControl().getSpeedModifier()>1.1;
    }
    public static boolean panting(Mob mob){return uses(mob)&&((StaminaUser)mob).staminaState().panting();}
    public static boolean spend(Mob mob,String action){return spend(mob,StaminaConfig.cost(action),action);}
    public static boolean spend(Mob mob,double amount,String reason){
        if(!uses(mob))return true;
        boolean paid=((StaminaUser)mob).staminaState().spend(amount,maximum(mob),StaminaConfig.delay(),reason);
        ((StaminaUser)mob).syncPanting(panting(mob));
        if(!paid){mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();}
        return paid;
    }
    /** Reject an attempted sprint if there is not even one tick's worth of stamina available. */
    public static boolean sprintAllowed(Mob mob){
        if(!uses(mob))return true;
        if(((StaminaUser)mob).staminaState().canSpend(StaminaConfig.sprint()/20,maximum(mob)))return true;
        spend(mob,StaminaConfig.sprint()/20,"sprint");return false;
    }
    public static void beforeTick(Mob mob,boolean working){
        if(!uses(mob)||!mob.isAlive())return;
        var user=(StaminaUser)mob;
        user.staminaState().tick(maximum(mob),regeneration(mob),user.staminaFleeing(),user.staminaConfident(),working);
        user.syncPanting(user.staminaState().panting());
        if(panting(mob)){mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();}
    }
    public static void afterTick(Mob mob){
        if(!uses(mob)||!mob.isAlive())return;
        if(fastMovement(mob) && mob.getDeltaMovement().horizontalDistanceSqr()>.0001)spend(mob,StaminaConfig.sprint()/20,"sprint");
        if(panting(mob)){
            mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();
            // Preserve vertical gravity and knockback reactions; no invulnerability is granted.
            if(mob.hurtTime==0)mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);
        }
        if(mob.tickCount%5==0 || panting(mob))mob.getPersistentData().put("RiftfallStamina",diagnostics(mob));
    }
    public static CompoundTag diagnostics(Mob mob){
        var d=new CompoundTag();if(!uses(mob))return d;
        var user=(StaminaUser)mob;var state=user.staminaState();
        d.putDouble("Current",state.current(maximum(mob)));d.putDouble("Maximum",maximum(mob));
        d.putBoolean("Panting",state.panting());d.putInt("DelayTicks",state.recoveryDelay());
        d.putDouble("RegenMultiplier",StaminaState.regenerationMultiplier(state.panting(),user.staminaFleeing(),user.staminaConfident()));
        d.putDouble("BaseRegenPerSecond",regeneration(mob));d.putString("LastExpense",state.lastExpense());return d;
    }
    public static void save(Mob mob,CompoundTag data){if(uses(mob))data.put("RiftfallStamina",diagnostics(mob));}
    public static void load(Mob mob,CompoundTag data){
        if(!uses(mob)||!data.contains("RiftfallStamina"))return;
        var saved=data.getCompound("RiftfallStamina");mob.getPersistentData().putDouble("RiftfallStaminaLoadingMaximum",saved.getDouble("Maximum"));((StaminaUser)mob).staminaState().restore(saved.getDouble("Current"),saved.getBoolean("Panting"),saved.getInt("DelayTicks"),maximum(mob));
    }
    private MobStamina(){}
}

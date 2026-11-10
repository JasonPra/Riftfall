package org.Azkerash.riftfall.stamina;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;

/** Shared stamina API. Nonparticipants and all undead keep their existing unlimited resource behavior. */
public final class MobStamina {
    public static boolean uses(Mob mob){return mob instanceof StaminaUser && !mob.getType().is(EntityTypeTags.UNDEAD);}
    public static boolean panting(Mob mob){return uses(mob)&&((StaminaUser)mob).staminaState().panting();}
    public static boolean spend(Mob mob,String action){return spend(mob,StaminaConfig.cost(action),action);}
    public static boolean spend(Mob mob,double amount,String reason){
        if(!uses(mob))return true;
        boolean paid=((StaminaUser)mob).staminaState().spend(amount,StaminaConfig.maximum(),StaminaConfig.delay(),reason);
        ((StaminaUser)mob).syncPanting(panting(mob));
        if(!paid){mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();}
        return paid;
    }
    /** Reject an attempted sprint if there is not even one tick's worth of stamina available. */
    public static boolean sprintAllowed(Mob mob){
        if(!uses(mob))return true;
        if(((StaminaUser)mob).staminaState().canSpend(StaminaConfig.sprint()/20,StaminaConfig.maximum()))return true;
        spend(mob,StaminaConfig.sprint()/20,"sprint");return false;
    }
    public static void beforeTick(Mob mob,boolean working){
        if(!uses(mob)||!mob.isAlive())return;
        var user=(StaminaUser)mob;
        user.staminaState().tick(StaminaConfig.maximum(),StaminaConfig.regeneration(),user.staminaFleeing(),user.staminaConfident(),working);
        user.syncPanting(user.staminaState().panting());
        if(panting(mob)){mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();}
    }
    public static void afterTick(Mob mob){
        if(!uses(mob)||!mob.isAlive())return;
        if(mob.isSprinting() && mob.getDeltaMovement().horizontalDistanceSqr()>.0001)spend(mob,StaminaConfig.sprint()/20,"sprint");
        if(panting(mob)){
            mob.setSprinting(false);mob.stopUsingItem();mob.getNavigation().stop();
            // Preserve vertical gravity and knockback reactions; no invulnerability is granted.
            if(mob.hurtTime==0)mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);
        }
        mob.getPersistentData().put("RiftfallStamina",diagnostics(mob));
    }
    public static CompoundTag diagnostics(Mob mob){
        var d=new CompoundTag();if(!uses(mob))return d;
        var user=(StaminaUser)mob;var state=user.staminaState();
        d.putDouble("Current",state.current(StaminaConfig.maximum()));d.putDouble("Maximum",StaminaConfig.maximum());
        d.putBoolean("Panting",state.panting());d.putInt("DelayTicks",state.recoveryDelay());
        d.putDouble("RegenMultiplier",StaminaState.regenerationMultiplier(state.panting(),user.staminaFleeing(),user.staminaConfident()));
        d.putDouble("BaseRegenPerSecond",StaminaConfig.regeneration());d.putString("LastExpense",state.lastExpense());return d;
    }
    public static void save(Mob mob,CompoundTag data){if(uses(mob))data.put("RiftfallStamina",diagnostics(mob));}
    public static void load(Mob mob,CompoundTag data){
        if(!uses(mob)||!data.contains("RiftfallStamina"))return;
        var saved=data.getCompound("RiftfallStamina");((StaminaUser)mob).staminaState().restore(saved.getDouble("Current"),saved.getBoolean("Panting"),saved.getInt("DelayTicks"),StaminaConfig.maximum());
    }
    private MobStamina(){}
}

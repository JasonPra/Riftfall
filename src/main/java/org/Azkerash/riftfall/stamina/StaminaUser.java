package org.Azkerash.riftfall.stamina;

/** Inherited by every Mob. Override hooks for species-specific confidence and explicitly priced moves. */
public interface StaminaUser {
    StaminaState staminaState();
    default boolean managesStaminaActions(){return false;}
    default boolean staminaConfident(){return this instanceof net.minecraft.world.entity.Mob mob && (mob.getPersistentData().getBoolean("RiftfallConfident") || mob.getPersistentData().getCompound("RiftfallBehavior").getCompound("Signals").getDouble("bold")>0);}
    default boolean staminaFleeing(){return this instanceof net.minecraft.world.entity.Mob mob && (mob.getPersistentData().getBoolean("RiftfallRetreating") || mob.getPersistentData().getBoolean("RiftfallPanicking") || mob.goalSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).anyMatch(g->g.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal || g.getGoal() instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal<?>));}
    void syncPanting(boolean panting);
}

package org.Azkerash.riftfall.fauna;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.Azkerash.riftfall.GoblinEntity;

/** Territorial magical-beast NPC; native wolf lifecycle, with an independent utility brain. */
public final class RyEntity extends Wolf implements net.minecraft.world.entity.npc.Npc, org.Azkerash.riftfall.confidence.ConfidenceParticipant {
    RyBehavior behavior;
    private BlockPos territory;
    public RyEntity(EntityType<? extends Wolf> type,Level level){super(type,level);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2,new FollowOwnerGoal(this,1,12,4));
        behavior=new RyBehavior(this);
        goalSelector.addGoal(3,behavior.goal());
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(1,new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(1,new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
            e->!isBaby()&&!isAlliedTo(e)&&hostileCreature(e)&&withinTerritory(e.blockPosition(),40)));
    }
    @Override public org.Azkerash.riftfall.confidence.CombatConfidenceController confidenceController() { return behavior.confidence; }
    static boolean hostileCreature(LivingEntity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof AbstractIllager || e instanceof GoblinEntity;
    }
    BlockPos territory() { if(isTame()&&getOwner()!=null)return getOwner().blockPosition();if(territory==null)territory=blockPosition();return territory; }
    boolean withinTerritory(BlockPos pos,double radius){return territory().distSqr(pos)<=radius*radius;}
    @Override public boolean isAlliedTo(Entity other) {
        if(other instanceof RyEntity||other.getType().is(MagicBeastFaction.MEMBERS))return true;
        if(other instanceof LivingEntity living) {
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(living,yesman.epicfight.world.capabilities.entitypatch.MobPatch.class);
            if(patch!=null&&patch.getFaction()==MagicBeastFaction.MAGIC_BEASTS)return true;
        }
        return super.isAlliedTo(other);
    }
    @Override public void setTarget(LivingEntity target) { super.setTarget(target!=null&&isAlliedTo(target)?null:target); }
    @Override public void aiStep() {
        super.aiStep();
        if(!level().isClientSide&&!isNoAi()&&isAlive()&&behavior!=null)behavior.upkeep();
    }
    @Override public RyEntity getBreedOffspring(ServerLevel level,AgeableMob other){
        var child=ManaheartFauna.RY.get().create(level);if(child!=null&&getOwnerUUID()!=null){child.setOwnerUUID(getOwnerUUID());child.setTame(true,true);}return child;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(territory!=null)tag.putLong("RyTerritory",territory.asLong());
        tag.putString("NpcFaction","riftfall:magic_beasts");
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);territory=tag.contains("RyTerritory")?BlockPos.of(tag.getLong("RyTerritory")):null;
        getPersistentData().putBoolean("RiftfallCasting",false);
    }
}

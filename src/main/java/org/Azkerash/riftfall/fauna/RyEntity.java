package org.Azkerash.riftfall.fauna;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import org.Azkerash.riftfall.mana.MobMana;
/** A wind-affine wolf: native pack/taming behavior with a real Iron's Gust cast. */
public final class RyEntity extends Wolf {
 public RyEntity(EntityType<? extends Wolf> type,Level level){super(type,level);}
 @Override public RyEntity getBreedOffspring(ServerLevel level,AgeableMob other){
  var child=ManaheartFauna.RY.get().create(level);if(child!=null&&getOwnerUUID()!=null){child.setOwnerUUID(getOwnerUUID());child.setTame(true,true);}return child;
 }
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);getPersistentData().putBoolean("RiftfallCasting",false);}
 @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(3,new WindGoal());}
 private final class WindGoal extends Goal {
  int cooldown=60,time,windup;boolean released;
  WindGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
  private boolean targetValid(){var target=getTarget();return !isBaby()&&!isOrderedToSit()&&target!=null&&target.isAlive()&&!isAlliedTo(target)&&distanceToSqr(target)<12*12&&getSensing().hasLineOfSight(target);}
  @Override public boolean canUse(){if(cooldown>0){cooldown--;return false;}return targetValid()&&hurtTime==0&&MobMana.canCast(RyEntity.this,SpellRegistry.GUST_SPELL.get(),1);}
  @Override public boolean canContinueToUse(){return time<windup+5&&targetValid()&&!MobMana.hurtInterrupts(RyEntity.this,SpellRegistry.GUST_SPELL.get());}
  @Override public boolean requiresUpdateEveryTick(){return true;}
  @Override public void start(){time=0;released=false;getPersistentData().putBoolean("RiftfallCasting",true);windup=Math.max(12,MobMana.windup(RyEntity.this,SpellRegistry.GUST_SPELL.get(),1));getNavigation().stop();SpellRegistry.GUST_SPELL.get().playSound(SpellRegistry.GUST_SPELL.get().getCastStartSound(),RyEntity.this);}
  @Override public void tick(){
   var target=getTarget();if(target==null)return;getNavigation().stop();getLookControl().setLookAt(target,90,90);lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
   if(++time<windup){if((time&3)==0)((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,getX(),getEyeY(),getZ(),1,.18,.08,.18,.015);return;}
   if(time==windup){var spell=SpellRegistry.GUST_SPELL.get();var data=MobMana.data(RyEntity.this);
    if(spell.checkPreCastConditions(level(),1,RyEntity.this,data)&&MobMana.spend(RyEntity.this,spell,1)){
     spell.onServerPreCast(level(),1,RyEntity.this,data);spell.onCast(level(),1,RyEntity.this,CastSource.MOB,data);spell.onServerCastComplete(level(),1,RyEntity.this,data,false);released=true;
     getPersistentData().putInt("RyWindCasts",getPersistentData().getInt("RyWindCasts")+1);
    }
   }
  }
  @Override public void stop(){getPersistentData().putBoolean("RiftfallCasting",false);cooldown=MobMana.cooldown(RyEntity.this,released?200:60);MobMana.data(RyEntity.this).resetCastingState();}
 }
}

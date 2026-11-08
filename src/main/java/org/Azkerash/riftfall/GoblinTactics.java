package org.Azkerash.riftfall;

import java.util.EnumSet;
import java.util.Comparator;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

final class GoblinTactics {
    static Vec3 behind(Vec3 enemy,Vec3 defender) {
        Vec3 away=defender.subtract(enemy).multiply(1,0,1).normalize();
        return defender.add(away.scale(6));
    }
    static boolean needsHealing(float health,float max) { return health>0 && max-health>=2; }
    static boolean threatened(double squaredDistance) { return squaredDistance<6.5*6.5; }
    static final class Retreat extends Goal {
        final GoblinEntity mob;
        Vec3 destination;
        int ticks;
        Retreat(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            var target=mob.getTarget();
            if (target==null || !target.isAlive()) return false;
            var defender=mob.level().getEntitiesOfClass(GoblinEntity.class,mob.getBoundingBox().inflate(12),
                g->g!=mob && g.isAlive() && !g.isRanged() && mob.isAlliedTo(g)
                    && g.distanceToSqr(target)<mob.distanceToSqr(target))
                .stream().min(Comparator.comparingDouble(g->g.distanceToSqr(mob))).orElse(null);
            Vec3 cover=defender==null ? null : behind(target.position(),defender.position());
            boolean exposed=cover!=null && !mob.isUsingItem() && mob.distanceToSqr(cover)>25 && mob.distanceToSqr(target)<18*18;
            if (!threatened(mob.distanceToSqr(target)) && !exposed) return false;
            destination=cover;
            if (destination==null || threatened(destination.distanceToSqr(target.position())))
                destination=LandRandomPos.getPosAway(mob,10,2,target.position());
            return destination!=null && destination.distanceToSqr(target.position())>mob.distanceToSqr(target);
        }
        @Override public void start() {
            ticks=0; mob.getPersistentData().putBoolean("RiftfallRetreating",true);
            mob.getNavigation().moveTo(destination.x,destination.y,destination.z,1.35);
        }
        @Override public boolean canContinueToUse() {
            return ticks<50 && mob.getTarget()!=null && mob.getTarget().isAlive() && !mob.getNavigation().isDone()
                && mob.distanceToSqr(destination)>1;
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { ticks++; }
        @Override public void stop() { mob.getPersistentData().putBoolean("RiftfallRetreating",false); }
    }
    static final class Bow extends Goal {
        final GoblinEntity mob;
        int draw,cooldown;
        Bow(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            return mob.isArcher() && mob.getMainHandItem().is(net.minecraft.world.item.Items.BOW)
                && mob.getTarget()!=null && mob.getTarget().isAlive() && !mob.isAlliedTo(mob.getTarget());
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { draw=0; mob.setAggressive(true); }
        @Override public void tick() {
            var target=mob.getTarget(); if (target==null) return;
            mob.getLookControl().setLookAt(target,90,90);
            if (mob.distanceToSqr(target)>20*20 || !mob.getSensing().hasLineOfSight(target)) {
                draw=0; mob.stopUsingItem(); mob.getNavigation().moveTo(target,1.0); return;
            }
            mob.getNavigation().stop();
            if (cooldown>0) { cooldown--; return; }
            if (draw==0) mob.startUsingItem(InteractionHand.MAIN_HAND);
            if (++draw>=20) {
                mob.stopUsingItem(); mob.performRangedAttack(target,1); draw=0; cooldown=40;
                var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
                if (patch!=null) patch.playShootingAnimation();
            }
        }
        @Override public void stop() { mob.stopUsingItem(); mob.setAggressive(false); draw=0; }
    }
    static final class StaffClub extends Goal {
        final GoblinEntity mob;
        int ticks;
        long ready;
        StaffClub(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            var target=mob.getTarget();
            return mob.level().getGameTime()>=ready && mob.hurtTime==0 && target!=null && target.isAlive()
                && !mob.isAlliedTo(target) && mob.distanceToSqr(target)<2.5*2.5 && mob.getSensing().hasLineOfSight(target);
        }
        @Override public boolean canContinueToUse() { return ticks<18 && mob.getTarget()!=null && mob.getTarget().isAlive() && mob.hurtTime==0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            ticks=0; ready=mob.level().getGameTime()+45; mob.getNavigation().stop();
            RiftCastingAnimations.play(mob,RiftCastingAnimations.CLUB);
        }
        @Override public void tick() {
            LivingEntity target=mob.getTarget(); if (target==null) return;
            mob.getLookControl().setLookAt(target,90,90);
            if (++ticks==8 && mob.distanceToSqr(target)<3*3 && mob.getSensing().hasLineOfSight(target)) {
                mob.swing(InteractionHand.MAIN_HAND);
                if (mob.doHurtTarget(target)) {
                    target.knockback(.9,mob.getX()-target.getX(),mob.getZ()-target.getZ());
                    mob.getPersistentData().putInt("RiftfallClubHits",mob.getPersistentData().getInt("RiftfallClubHits")+1);
                }
            }
        }
    }
    static final class Heal extends Goal {
        final GoblinEntity mob;
        LivingEntity recipient;
        int ticks;
        final MagicData data=new MagicData(true);
        Heal(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            if (mob.level().getGameTime()<mob.getPersistentData().getLong("RiftfallNextHeal") || mob.hurtTime>0
                || mob.getTarget()!=null && threatened(mob.distanceToSqr(mob.getTarget())) || !SpellRegistry.HEALING_CIRCLE_SPELL.get().isEnabled()) return false;
            recipient=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(10),
                g->g!=mob && g.isAlive() && mob.isAlliedTo(g) && needsHealing(g.getHealth(),g.getMaxHealth()) && mob.getSensing().hasLineOfSight(g))
                .stream().min(Comparator.comparingDouble(g->g.getHealth()/g.getMaxHealth())).orElse(null);
            return recipient!=null;
        }
        @Override public boolean canContinueToUse() {
            return ticks<34 && mob.hurtTime==0 && recipient!=null && recipient.isAlive() && mob.distanceToSqr(recipient)<12*12
                && (mob.getTarget()==null || !threatened(mob.distanceToSqr(mob.getTarget())));
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            ticks=0; mob.getNavigation().stop(); mob.getPersistentData().putBoolean("RiftfallCasting",true);
            RiftCastingAnimations.play(mob,RiftCastingAnimations.CHARGE);
            var spell=SpellRegistry.HEALING_CIRCLE_SPELL.get(); spell.playSound(spell.getCastStartSound(),mob);
        }
        @Override public void tick() {
            mob.getLookControl().setLookAt(recipient,90,90);
            var spell=SpellRegistry.HEALING_CIRCLE_SPELL.get();
            if (++ticks==20) {
                data.setAdditionalCastData(new TargetEntityCastData(recipient));
                spell.onServerPreCast(mob.level(),1,mob,data);
                data.setAdditionalCastData(new TargetEntityCastData(recipient));
                spell.onCast(mob.level(),1,mob,CastSource.MOB,data);
                spell.onServerCastComplete(mob.level(),1,mob,data,false);
                mob.getPersistentData().putLong("RiftfallNextHeal",mob.level().getGameTime()+400);
                mob.getPersistentData().putInt("RiftfallHealsCast",mob.getPersistentData().getInt("RiftfallHealsCast")+1);
                RiftCastingAnimations.play(mob,RiftCastingAnimations.RELEASE);
            }
            if (ticks==26) RiftCastingAnimations.play(mob,RiftCastingAnimations.RECOVER);
        }
        @Override public void stop() {
            mob.getPersistentData().putBoolean("RiftfallCasting",false); data.resetCastingState();
            if (ticks<20) mob.getPersistentData().putLong("RiftfallNextHeal",mob.level().getGameTime()+80);
            if (ticks<26) RiftCastingAnimations.play(mob,RiftCastingAnimations.RECOVER);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void friendlyFire(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof GoblinEntity goblin && goblin.isAlliedTo(event.getEntity())) event.setCanceled(true);
    }
    private GoblinTactics() {}
}

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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

final class GoblinTactics {
    static boolean needsHealing(float health,float max) { return health>0 && max-health>=2; }
    private static boolean needsCover(GoblinEntity mob) {
        return mob.retreatThreat!=null && mob.retreatThreat.isAlive() && mob.level().getGameTime()<mob.retreatCoverUntil;
    }
    static boolean coverSafe(GoblinEntity mob) {
        return mob.coveredAlly!=null && mob.coveredAlly.isAlive() && needsCover(mob.coveredAlly)
            && mob.getTarget()!=null && mob.distanceToSqr(mob.getTarget())>=Math.pow(Math.max(3.5,mob.squad.panicRange()),2);
    }
    /** Target selection does not occupy MOVE/LOOK, so bow/spell goals can provide cover. */
    static final class CoverRetreat extends Goal {
        final GoblinEntity mob;
        GoblinEntity ally;
        CoverRetreat(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            ally=mob.level().getEntitiesOfClass(GoblinEntity.class,mob.getBoundingBox().inflate(16),
                g->!g.isRanged() && g.isAlive() && mob.isAlliedTo(g) && needsCover(g) && !mob.isAlliedTo(g.retreatThreat))
                .stream().min(Comparator.comparingDouble(g->g.distanceToSqr(mob))).orElse(null);
            return ally!=null;
        }
        @Override public boolean canContinueToUse() {
            return ally!=null && ally.isAlive() && needsCover(ally)
                && ally.distanceToSqr(mob)<20*20 && !mob.isAlliedTo(ally.retreatThreat);
        }
        @Override public void start() {
            mob.coveredAlly=ally; mob.setTarget(ally.retreatThreat);
            mob.getPersistentData().putBoolean("RiftfallCoveringRetreat",true);
            mob.getPersistentData().putInt("RiftfallCoverStarts",mob.getPersistentData().getInt("RiftfallCoverStarts")+1);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { mob.setTarget(ally.retreatThreat); }
        @Override public void stop() {
            mob.coveredAlly=null; ally=null; mob.getPersistentData().putBoolean("RiftfallCoveringRetreat",false);
        }
    }
    static final class WarriorRetreat extends Goal {
        final GoblinEntity mob;
        Vec3 destination;
        int ticks;
        long ready;
        WarriorRetreat(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            var target=mob.getTarget();
            if (target==null || !target.isAlive() || mob.getHealth()>mob.getMaxHealth()*GoblinSquadTactics.retreatHealth(mob.squad.courage())
                || mob.level().getGameTime()<ready || mob.distanceToSqr(target)>7*7) return false;
            destination=LandRandomPos.getPosAway(mob,10,2,target.position());
            return destination!=null && destination.distanceToSqr(target.position())>mob.distanceToSqr(target);
        }
        @Override public void start() {
            ticks=0; ready=mob.level().getGameTime()+100;
            mob.retreatThreat=mob.getTarget(); mob.retreatCoverUntil=mob.level().getGameTime()+100;
            mob.getPersistentData().putInt("RiftfallRetreats",mob.getPersistentData().getInt("RiftfallRetreats")+1);
            mob.getPersistentData().putBoolean("RiftfallRetreating",true);
            mob.getNavigation().moveTo(destination.x,destination.y,destination.z,1.1);
        }
        @Override public boolean canContinueToUse() {
            return ticks<80 && mob.getTarget()!=null && mob.getTarget().isAlive() && mob.getHealth()<mob.getMaxHealth()*.5
                && mob.distanceToSqr(mob.getTarget())<9*9 && !mob.getNavigation().isDone();
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            ticks++; mob.retreatCoverUntil=mob.level().getGameTime()+40; mob.getLookControl().setLookAt(mob.getTarget(),45,45);
        }
        @Override public void stop() {
            // Keep the pursuer marked briefly while the warrior finishes withdrawing or gets healed.
            mob.retreatCoverUntil=mob.level().getGameTime()+40;
            mob.getPersistentData().putInt("RiftfallLastRetreatTicks",ticks);
            mob.getPersistentData().putBoolean("RiftfallRetreating",false); mob.getNavigation().stop();
        }
    }
    static final class Retreat extends GoblinSquadTactics.PanicRetreat {
        Retreat(GoblinEntity mob) { super(mob); }
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
            if (draw==0 && !mob.squad.clearShot()) return;
            if (draw==0) mob.startUsingItem(InteractionHand.MAIN_HAND);
            if (++draw>=20) {
                mob.stopUsingItem(); draw=0;
                if (!mob.squad.clearShot()) { cooldown=10; return; }
                mob.performRangedAttack(target,1); cooldown=40;
                var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
                if (patch!=null) patch.playShootingAnimation();
            }
        }
        @Override public void stop() { mob.stopUsingItem(); mob.setAggressive(false); draw=0; }
    }
    static boolean interrupted(GoblinEntity mob) {
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,GoblinPatch.class);
        return mob.hurtTime>0 || (patch!=null && patch.isStunned());
    }
    static boolean canPlunge(double height,double horizontalDistanceSqr) {
        return height>=.5 && height<=1.75 && horizontalDistanceSqr>=1.5*1.5 && horizontalDistanceSqr<=4.5*4.5;
    }
    enum SwordAttack {
        SWIPE(18,8), BACKSWING(18,8), FLURRY(34,6,17,28);
        final int duration;
        private final int[] impacts;
        SwordAttack(int duration,int... impacts) { this.duration=duration; this.impacts=impacts; }
        boolean hitsAt(int tick) { for (int impact : impacts) if (tick==impact) return true; return false; }
    }
    /** Vanilla pursuit and cooldown, with synchronized swings and a three-cut flurry. */
    static final class Melee extends MeleeAttackGoal {
        final GoblinEntity goblin;
        LivingEntity victim;
        int remaining,nextAttack;
        SwordAttack attack;
        Melee(GoblinEntity mob) { super(mob,1.1,false); goblin=mob; }
        @Override public boolean canContinueToUse() {
            return (remaining>0 && victim!=null && victim.isAlive() && !interrupted(goblin)) || (GoblinSquadTactics.validEnemy(goblin,goblin.getTarget())
                    && goblin.squad.hasWarriorPlan() && goblin.distanceToSqr(goblin.getTarget())<28*28) || super.canContinueToUse();
        }
        @Override protected void checkAndPerformAttack(LivingEntity target) {
            if (remaining==0 && GoblinBuckler.canAttack(goblin) && canPerformAttack(target) && goblin.squad.mayStrike() && !interrupted(goblin)) {
                resetAttackCooldown(); victim=target;
                attack=SwordAttack.values()[nextAttack]; nextAttack=(nextAttack+1)%SwordAttack.values().length;
                remaining=attack.duration;
                GoblinBuckler.lowerForAttack(goblin, attack.duration);
                GoblinAnimations.play(goblin,switch (attack) {
                    case SWIPE -> GoblinAnimations.SLASH;
                    case BACKSWING -> GoblinAnimations.BACKSWING;
                    case FLURRY -> GoblinAnimations.FLURRY;
                });
                String counter=switch (attack) { case SWIPE -> "RiftfallSwipes"; case BACKSWING -> "RiftfallBackswings"; case FLURRY -> "RiftfallFlurries"; };
                goblin.getPersistentData().putInt(counter,goblin.getPersistentData().getInt(counter)+1);
                goblin.getPersistentData().putInt("RiftfallMeleeSwings",goblin.getPersistentData().getInt("RiftfallMeleeSwings")+1);
            }
        }
        @Override public void tick() {
            super.tick();
            if (remaining<=0) {
                if (!interrupted(goblin)) goblin.squad.moveWarrior();
                return;
            }
            goblin.getNavigation().stop();
            if (interrupted(goblin) || victim==null || !victim.isAlive() || goblin.isAlliedTo(victim)) { remaining=0; victim=null; return; }
            goblin.getLookControl().setLookAt(victim,90,90);
            if (attack.hitsAt(attack.duration- --remaining)) goblin.swing(InteractionHand.MAIN_HAND);
        }
        @Override public void stop() { super.stop(); remaining=0; victim=null; }
    }
    /** A committed spider-like leap from nearby high ground, never a homing attack. */
    static final class Plunge extends Goal {
        final GoblinEntity mob;
        LivingEntity victim;
        int ticks;
        long ready;
        boolean struck;
        Vec3 launch;
        Plunge(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP)); }
        @Override public boolean canUse() {
            var target=mob.getTarget();
            if (target==null || !target.isAlive() || mob.isAlliedTo(target) || !mob.onGround() || mob.isInWaterOrBubble()
                || mob.level().getGameTime()<ready || interrupted(mob)) return false;
            Vec3 offset=target.position().subtract(mob.position());
            return canPlunge(-offset.y,offset.horizontalDistanceSqr()) && mob.getSensing().hasLineOfSight(target)
                && mob.level().noCollision(mob,mob.getBoundingBox().move(0,.6,0));
        }
        @Override public boolean canContinueToUse() {
            return ticks<32 && victim!=null && victim.isAlive() && !interrupted(mob) && (ticks<10 || !mob.onGround());
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            victim=mob.getTarget(); ticks=0; struck=false; ready=mob.level().getGameTime()+80;
            GoblinBuckler.lowerForAttack(mob,32);
            mob.getNavigation().stop(); mob.setAggressive(true);
            Vec3 offset=victim.position().subtract(mob.position());
            float yaw=(float)(Math.toDegrees(Math.atan2(offset.z,offset.x))-90);
            mob.setYRot(yaw); mob.setYBodyRot(yaw); mob.setYHeadRot(yaw);
            GoblinAnimations.play(mob,GoblinAnimations.PLUNGE);
            launch=offset.multiply(.13,0,.13);
            mob.getPersistentData().putInt("RiftfallPlunges",mob.getPersistentData().getInt("RiftfallPlunges")+1);
        }
        @Override public void tick() {
            ticks++; mob.getNavigation().stop();
            if (ticks==6) {
                mob.setDeltaMovement(launch.x,.35,launch.z); mob.setOnGround(false); mob.hasImpulse=true;
            }
            if (!struck && ticks>=17 && mob.getDeltaMovement().y<0) {
                struck=true; mob.swing(InteractionHand.MAIN_HAND);
            }
        }
        @Override public void stop() { victim=null; mob.setAggressive(false); }
    }
    static float staffFacing(double x,double z) { return (float)(Math.toDegrees(Math.atan2(z,x))-90); }
    enum StaffAttack {
        SWEEP(40,22,.9), SMASH(48,28,.45);
        final int duration, impact;
        final double knockback;
        StaffAttack(int duration,int impact,double knockback) {
            this.duration=duration; this.impact=impact; this.knockback=knockback;
        }
    }
    static final class StaffClub extends Goal {
        final GoblinEntity mob;
        LivingEntity victim;
        StaffAttack attack;
        int ticks, nextAttack;
        long ready;
        float facing;
        StaffClub(GoblinEntity mob) {
            this.mob=mob; nextAttack=mob.getRandom().nextInt(2);
            setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
        }
        @Override public boolean canUse() {
            var target=mob.getTarget();
            return mob.level().getGameTime()>=ready && !interrupted(mob) && target!=null && target.isAlive()
                && !mob.isAlliedTo(target) && mob.distanceToSqr(target)<2.5*2.5 && mob.getSensing().hasLineOfSight(target);
        }
        @Override public boolean canContinueToUse() {
            return ticks<attack.duration && victim!=null && victim.isAlive() && !mob.isAlliedTo(victim) && !interrupted(mob);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            victim=mob.getTarget(); attack=StaffAttack.values()[nextAttack]; nextAttack=(nextAttack+1)%2;
            ticks=0; ready=mob.level().getGameTime()+attack.duration+25; mob.getNavigation().stop();
            facing=staffFacing(victim.getX()-mob.getX(),victim.getZ()-mob.getZ());
            face();
            mob.getPersistentData().putString("RiftfallStaffAttack",attack.name());
            GoblinAnimations.play(mob,attack==StaffAttack.SWEEP ? GoblinAnimations.STAFF_SWEEP : GoblinAnimations.STAFF_SMASH);
        }
        @Override public void tick() {
            if (victim==null) return;
            mob.getNavigation().stop();
            // Track through the wind-up, then commit instead of steering the hit around a dodge.
            if (ticks<attack.impact-4) facing=staffFacing(victim.getX()-mob.getX(),victim.getZ()-mob.getZ());
            face();
            if (++ticks==attack.impact) mob.swing(InteractionHand.MAIN_HAND);
        }
        private void face() {
            mob.setYRot(facing); mob.setYBodyRot(facing); mob.setYHeadRot(facing);
            double radians=Math.toRadians(facing);
            mob.getLookControl().setLookAt(mob.getX()-Math.sin(radians)*4,mob.getEyeY(),mob.getZ()+Math.cos(radians)*4,90,90);
        }
        @Override public void stop() { victim=null; mob.getPersistentData().remove("RiftfallStaffAttack"); }
    }
    static final class Heal extends Goal {
        final GoblinEntity mob;
        LivingEntity recipient;
        int ticks;
        final MagicData data=new MagicData(true);
        Heal(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            if (mob.level().getGameTime()<mob.getPersistentData().getLong("RiftfallNextHeal") || mob.hurtTime>0
                || mob.getTarget()!=null && mob.distanceToSqr(mob.getTarget())<Math.pow(mob.squad.panicRange(),2) && !coverSafe(mob) || !SpellRegistry.HEALING_CIRCLE_SPELL.get().isEnabled()) return false;
            recipient=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(10),
                g->g!=mob && g.isAlive() && mob.isAlliedTo(g) && needsHealing(g.getHealth(),g.getMaxHealth()) && mob.getSensing().hasLineOfSight(g))
                .stream().min(Comparator.<LivingEntity>comparingInt(g->g instanceof GoblinEntity goblin
                    && !goblin.isRanged() && goblin.getPersistentData().getBoolean("RiftfallRetreating") ? 0 : 1)
                    .thenComparingDouble(g->g.getHealth()/g.getMaxHealth())).orElse(null);
            return recipient!=null;
        }
        @Override public boolean canContinueToUse() {
            return ticks<34 && mob.hurtTime==0 && recipient!=null && recipient.isAlive() && mob.distanceToSqr(recipient)<12*12
                && (mob.getTarget()==null || mob.distanceToSqr(mob.getTarget())>=Math.pow(mob.squad.panicRange(),2) || coverSafe(mob));
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            ticks=0; mob.getNavigation().stop(); mob.getPersistentData().putBoolean("RiftfallCasting",true);
            GoblinAnimations.play(mob,GoblinAnimations.CHARGE);
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
                GoblinAnimations.play(mob,GoblinAnimations.RELEASE);
            }
            if (ticks==26) GoblinAnimations.play(mob,GoblinAnimations.RECOVER);
        }
        @Override public void stop() {
            mob.getPersistentData().putBoolean("RiftfallCasting",false); data.resetCastingState();
            if (ticks<20) mob.getPersistentData().putLong("RiftfallNextHeal",mob.level().getGameTime()+80);
            if (ticks<26) GoblinAnimations.play(mob,GoblinAnimations.RECOVER);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void friendlyFire(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof GoblinEntity goblin && goblin.isAlliedTo(event.getEntity())) event.setCanceled(true);
    }
    private GoblinTactics() {}
}

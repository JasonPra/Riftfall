package org.Azkerash.riftfall;

import static org.Azkerash.riftfall.GoblinTactics.*;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import org.Azkerash.riftfall.mana.MobMana;

import java.util.Comparator;
import java.util.EnumSet;

/** Shaman staff/healing execution; the shared utility engine still owns action selection. */
final class GoblinShamanActions {
    static final class StaffClub extends Goal {
        final GoblinEntity mob;
        LivingEntity victim;
        StaffAttack attack;
        int ticks, nextAttack, approachTicks;
        boolean windingUp, opportunistic;
        long ready, nextApproachPath;
        float facing;

        StaffClub(GoblinEntity mob) {
            this.mob = mob;
            nextAttack = mob.getRandom().nextInt(2);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private LivingEntity staffTarget() {
            // Defend against immediate mob threats even when the target selector prefers a player.
            var closeThreat =
                    mob
                            .level()
                            .getEntitiesOfClass(
                                    Mob.class,
                                    mob.getBoundingBox().inflate(2.5),
                                    other ->
                                            other != mob
                                                    && other.isAlive()
                                                    && !mob.isAlliedTo(other)
                                                    && mob.canAttack(other)
                                                    && mob.distanceToSqr(other) < 2.5 * 2.5
                                                    && mob.getSensing().hasLineOfSight(other)
                                                    && (GoblinHostility.undeadHunter(other)
                                                            || other == mob.getTarget()
                                                            || other.getTarget() != null
                                                                    && (other.getTarget() == mob
                                                                            || mob.isAlliedTo(
                                                                                    other
                                                                                            .getTarget()))))
                            .stream()
                            .min(Comparator.comparingDouble(mob::distanceToSqr))
                            .orElse(null);
            var target = mob.getTarget();
            if (closeThreat != null
                    && (target == null
                            || mob.distanceToSqr(closeThreat) < mob.distanceToSqr(target))) {
                return closeThreat;
            }
            return target;
        }

        @Override
        public boolean canUse() {
            var target = staffTarget();
            return mob.level().getGameTime() >= ready
                    && !interrupted(mob)
                    && target != null
                    && target.isAlive()
                    && !mob.isAlliedTo(target)
                    && mob.getSensing().hasLineOfSight(target)
                    && (WeaponReach.canContact(
                                    mob,
                                    target,
                                    (StaffAttack.values()[nextAttack] == StaffAttack.SWEEP
                                                    ? GoblinAnimations.STAFF_SWEEP
                                                    : GoblinAnimations.STAFF_SMASH)
                                            .get())
                            || mob.distanceToSqr(target) < 2.5 * 2.5
                            || mob.distanceToSqr(target) < 6 * 6 && exposedToStaff(target));
        }

        @Override
        public boolean canContinueToUse() {
            return victim != null
                    && victim.isAlive()
                    && !mob.isAlliedTo(victim)
                    && !interrupted(mob)
                    && (windingUp
                            ? ticks < attack.duration
                            : approachTicks < 40
                                    && mob.distanceToSqr(victim)
                                            < (opportunistic ? 6 * 6 : 3.5 * 3.5));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            victim = staffTarget();
            opportunistic = exposedToStaff(victim);
            attack = opportunistic ? StaffAttack.SMASH : StaffAttack.values()[nextAttack];
            nextAttack = (nextAttack + 1) % 2;
            ticks = 0;
            approachTicks = 0;
            nextApproachPath = 0;
            windingUp = false;
            ready = mob.level().getGameTime() + 10;
            mob.getPersistentData().putBoolean("RiftfallStaffApproaching", true);
            facing = staffFacing(victim.getX() - mob.getX(), victim.getZ() - mob.getZ());
            face();
            if (inReach()) {
                beginSwing();
            }
        }

        private boolean inReach() {
            return mob.getSensing().hasLineOfSight(victim)
                    && WeaponReach.canContact(
                            mob,
                            victim,
                            (attack == StaffAttack.SWEEP
                                            ? GoblinAnimations.STAFF_SWEEP
                                            : GoblinAnimations.STAFF_SMASH)
                                    .get());
        }

        private void beginSwing() {
            if (!org.Azkerash.riftfall.stamina.MobStamina.spend(
                    mob, attack == StaffAttack.SWEEP ? "staff_sweep" : "staff_smash")) {
                victim = null;
                approachTicks = 40;
                return;
            }
            windingUp = true;
            ticks = 0;
            ready = mob.level().getGameTime() + attack.duration + 25;
            mob.getNavigation().stop();
            facing = staffFacing(victim.getX() - mob.getX(), victim.getZ() - mob.getZ());
            face();
            mob.getPersistentData().putBoolean("RiftfallStaffApproaching", false);
            mob.getPersistentData().putString("RiftfallStaffAttack", attack.name());
            GoblinAnimations.play(
                    mob,
                    attack == StaffAttack.SWEEP
                            ? GoblinAnimations.STAFF_SWEEP
                            : GoblinAnimations.STAFF_SMASH);
        }

        @Override
        public void tick() {
            if (victim == null) {
                return;
            }
            if (!windingUp) {
                approachTicks++;
                facing = staffFacing(victim.getX() - mob.getX(), victim.getZ() - mob.getZ());
                face();
                if (inReach()) {
                    beginSwing();
                } else if (mob.level().getGameTime() >= nextApproachPath) {
                    nextApproachPath = mob.level().getGameTime() + 5;
                    // Vanilla's one-node arrival tolerance can stop outside a short weapon's
                    // collider.
                    var path = mob.getNavigation().createPath(victim.blockPosition(), 0);
                    if (path != null) {
                        mob.getNavigation().moveTo(path, 1.0);
                    }
                }
                return;
            }
            mob.getNavigation().stop();
            // Track through the wind-up, then commit instead of steering the hit around a dodge.
            if (ticks < attack.impact - 3) {
                facing = staffFacing(victim.getX() - mob.getX(), victim.getZ() - mob.getZ());
            }
            face();
            if (++ticks == attack.impact) {
                mob.swing(InteractionHand.MAIN_HAND);
            }
            if (attack == StaffAttack.SMASH && ticks == attack.impact + 1) {
                GoblinWeaponAttacks.groundImpact(mob);
            }
        }

        private void face() {
            mob.setYRot(facing);
            mob.setYBodyRot(facing);
            mob.setYHeadRot(facing);
            double radians = Math.toRadians(facing);
            mob.getLookControl()
                    .setLookAt(
                            mob.getX() - Math.sin(radians) * 4,
                            mob.getEyeY(),
                            mob.getZ() + Math.cos(radians) * 4,
                            90,
                            90);
        }

        @Override
        public void stop() {
            victim = null;
            mob.getNavigation().stop();
            mob.getPersistentData().remove("RiftfallStaffAttack");
            mob.getPersistentData().remove("RiftfallStaffApproaching");
        }
    }

    static final class Heal extends Goal {
        // Stronger circle while preserving the shaman's existing resource budget.
        private static final int HEALING_LEVEL = 2;
        private static final int MANA_LEVEL = 1;
        final GoblinEntity mob;
        LivingEntity recipient;
        int ticks, windup = 20;
        long nextHealSearch;
        MagicData data;

        Heal(GoblinEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            var spell = SpellRegistry.HEALING_CIRCLE_SPELL.get();
            MobMana.describe(mob, spell, MANA_LEVEL);
            if (mob.level().getGameTime() < mob.getPersistentData().getLong("RiftfallNextHeal")
                    || mob.hurtTime > 0
                    || mob.getTarget() != null
                            && mob.distanceToSqr(mob.getTarget())
                                    < Math.pow(mob.squad.panicRange(), 2)
                            && !coverSafe(mob)
                    || !SpellRegistry.HEALING_CIRCLE_SPELL.get().isEnabled()) {
                return false;
            }
            long now = mob.level().getGameTime();
            if (now < nextHealSearch) {
                return false;
            }
            nextHealSearch =
                    org.Azkerash.riftfall.behavior.SensingSchedule.next(now, 5, mob.getId());
            recipient =
                    mob
                            .level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    mob.getBoundingBox().inflate(10),
                                    g ->
                                            g != mob
                                                    && g.isAlive()
                                                    && mob.isAlliedTo(g)
                                                    && needsHealing(g.getHealth(), g.getMaxHealth())
                                                    && mob.getSensing().hasLineOfSight(g))
                            .stream()
                            .min(
                                    Comparator.<LivingEntity>comparingInt(
                                                    g ->
                                                            g instanceof GoblinEntity goblin
                                                                            && !goblin.isRanged()
                                                                            && goblin.getPersistentData()
                                                                                    .getBoolean(
                                                                                            "RiftfallRetreating")
                                                                    ? 0
                                                                    : 1)
                                            .thenComparingDouble(
                                                    g -> g.getHealth() / g.getMaxHealth()))
                            .orElse(null);
            return recipient != null && MobMana.canCast(mob, spell, MANA_LEVEL);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < windup + 14
                    && !MobMana.hurtInterrupts(mob, SpellRegistry.HEALING_CIRCLE_SPELL.get())
                    && recipient != null
                    && recipient.isAlive()
                    && mob.distanceToSqr(recipient) < 12 * 12
                    && (mob.getTarget() == null
                            || mob.distanceToSqr(mob.getTarget())
                                    >= Math.pow(mob.squad.panicRange(), 2)
                            || coverSafe(mob));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            data = MobMana.data(mob);
            if (!MobMana.canCast(mob, SpellRegistry.HEALING_CIRCLE_SPELL.get(), MANA_LEVEL)) {
                ticks = windup + 14;
                return;
            }
            ticks = 0;
            windup = MobMana.windup(mob, SpellRegistry.HEALING_CIRCLE_SPELL.get(), HEALING_LEVEL);
            mob.getNavigation().stop();
            mob.getPersistentData().putBoolean("RiftfallCasting", true);
            GoblinAnimations.play(mob, GoblinAnimations.CHARGE);
            var spell = SpellRegistry.HEALING_CIRCLE_SPELL.get();
            spell.playSound(spell.getCastStartSound(), mob);
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(recipient, 90, 90);
            var spell = SpellRegistry.HEALING_CIRCLE_SPELL.get();
            if (++ticks == windup) {
                data.setAdditionalCastData(new TargetEntityCastData(recipient));
                if (!spell.checkPreCastConditions(mob.level(), HEALING_LEVEL, mob, data)
                        || !MobMana.spend(mob, spell, MANA_LEVEL)) {
                    ticks = windup + 14;
                    GoblinAnimations.play(mob, GoblinAnimations.RECOVER);
                    return;
                }
                spell.onServerPreCast(mob.level(), HEALING_LEVEL, mob, data);
                data.setAdditionalCastData(new TargetEntityCastData(recipient));
                spell.onCast(mob.level(), HEALING_LEVEL, mob, CastSource.MOB, data);
                spell.onServerCastComplete(mob.level(), HEALING_LEVEL, mob, data, false);
                mob.getPersistentData()
                        .putLong(
                                "RiftfallNextHeal",
                                mob.level().getGameTime() + MobMana.cooldown(mob, 400));
                mob.getPersistentData()
                        .putInt(
                                "RiftfallHealsCast",
                                mob.getPersistentData().getInt("RiftfallHealsCast") + 1);
                GoblinAnimations.play(mob, GoblinAnimations.RELEASE);
            }
            if (ticks == windup + 6) {
                GoblinAnimations.play(mob, GoblinAnimations.RECOVER);
            }
        }

        @Override
        public void stop() {
            mob.getPersistentData().putBoolean("RiftfallCasting", false);
            if (data != null) {
                data.resetCastingState();
            }
            if (ticks < windup) {
                mob.getPersistentData().putLong("RiftfallNextHeal", mob.level().getGameTime() + 80);
            }
            if (ticks < windup + 6) {
                GoblinAnimations.play(mob, GoblinAnimations.RECOVER);
            }
        }
    }

    private GoblinShamanActions() {}
}

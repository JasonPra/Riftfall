package org.Azkerash.riftfall;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;

import net.minecraft.world.entity.ai.goal.Goal;

import org.Azkerash.riftfall.behavior.SensingSchedule;
import org.Azkerash.riftfall.mana.MobMana;
import org.Azkerash.riftfall.spells.RiftfallSpells;

import java.util.EnumSet;

/** Interruptible support cast; sensing is bounded and mana is charged only at release. */
final class GoblinRallyGoal extends Goal {
    private final GoblinEntity mob;
    private GoblinEntity recipient;
    private MagicData data;
    private int level, ticks, windup;
    private long nextSearch;
    private boolean released;

    GoblinRallyGoal(GoblinEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean safe() {
        return mob.getTarget() == null
                || mob.distanceToSqr(mob.getTarget()) >= 9
                || GoblinTactics.coverSafe(mob);
    }

    @Override
    public boolean canUse() {
        long now = mob.level().getGameTime();
        var spell = RiftfallSpells.RALLY.get();
        if (now < nextSearch
                || now < mob.getPersistentData().getLong("RiftfallNextRally")
                || !spell.isEnabled()
                || GoblinTactics.interrupted(mob)
                || !safe()) {
            return false;
        }
        nextSearch = SensingSchedule.next(now, 10, mob.getId());
        var needy = GoblinRally.needingHelp(mob);
        if (needy.isEmpty()) {
            return false;
        }
        data = MobMana.data(mob);
        level =
                RallyPolicy.level(
                        needy.size(),
                        data.getMana(),
                        spell.getManaCost(1),
                        spell.getManaCost(2),
                        spell.getMaxLevel());
        if (level == 0) {
            // An actually needed but unaffordable cast participates in normal mana recovery.
            MobMana.canCast(mob, spell, 1);
            return false;
        }
        recipient = needy.getFirst();
        MobMana.describe(mob, spell, level);
        return MobMana.canCast(mob, spell, level);
    }

    @Override
    public void start() {
        var spell = RiftfallSpells.RALLY.get();
        ticks = 0;
        released = false;
        windup = MobMana.windup(mob, spell, level);
        data = MobMana.data(mob);
        data.setAdditionalCastData(level == 1 ? new TargetEntityCastData(recipient) : null);
        if (!spell.checkPreCastConditions(mob.level(), level, mob, data)
                || !MobMana.canCast(mob, spell, level)) {
            ticks = windup + 12;
            return;
        }
        mob.getNavigation().stop();
        mob.getPersistentData().putBoolean("RiftfallCasting", true);
        GoblinAnimations.play(mob, GoblinAnimations.CHARGE);
        spell.playSound(spell.getCastStartSound(), mob);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < windup + 12
                && safe()
                && !MobMana.hurtInterrupts(mob, RiftfallSpells.RALLY.get());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
        if (recipient != null && recipient.isAlive()) {
            mob.getLookControl().setLookAt(recipient, 90, 90);
        }
        if (++ticks == windup) {
            var spell = RiftfallSpells.RALLY.get();
            if (!spell.checkPreCastConditions(mob.level(), level, mob, data)
                    || !MobMana.spend(mob, spell, level)) {
                ticks = windup + 12;
                return;
            }
            spell.onServerPreCast(mob.level(), level, mob, data);
            spell.onCast(mob.level(), level, mob, CastSource.MOB, data);
            spell.onServerCastComplete(mob.level(), level, mob, data, false);
            released = true;
            mob.getPersistentData()
                    .putLong(
                            "RiftfallNextRally",
                            mob.level().getGameTime()
                                    + MobMana.cooldown(mob, spell.getSpellCooldown()));
            mob.getPersistentData()
                    .putInt(
                            "RiftfallRalliesCast",
                            mob.getPersistentData().getInt("RiftfallRalliesCast") + 1);
            mob.getPersistentData().putInt("RiftfallLastRallyLevel", level);
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
        if (!released) {
            mob.getPersistentData().putLong("RiftfallNextRally", mob.level().getGameTime() + 80);
        }
        if (ticks < windup + 6) {
            GoblinAnimations.play(mob, GoblinAnimations.RECOVER);
        }
        recipient = null;
    }
}

package org.Azkerash.riftfall;

import java.util.EnumSet;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

final class MobSpellcasting {
    static boolean canLearn(String id,boolean undead) {
        return !undead && (id.equals("minecraft:pillager") || id.equals("riftfall:goblin_shaman"));
    }
    static boolean intelligentLiving(Mob mob) {
        return canLearn(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
            mob.getType().is(EntityTypeTags.UNDEAD));
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Mob mob && intelligentLiving(mob)) {
            mob.getPersistentData().putBoolean("RiftfallCasting",false);
            restore(mob);
        }
    }
    static Goal castingAction(Mob mob) { return new CastingGoal(mob); }
    static void restore(Mob mob) {
        if (mob instanceof GoblinEntity || !intelligentLiving(mob)) return;
        if (!(mob instanceof GoblinEntity) && !mob.getPersistentData().getBoolean("RiftfallSpellcaster")) return;
        if (mob.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof CastingGoal)) return;
        mob.goalSelector.addGoal(1,new CastingGoal(mob));
    }
    private static final class CastingGoal extends Goal {
        final Mob mob;
        final MagicData data = new MagicData(true);
        AbstractSpell spell;
        int time, cooldown=40;
        boolean released;
        CastingGoal(Mob mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            if (cooldown>0) { cooldown--; return false; }
            var target=mob.getTarget();
            return intelligentLiving(mob) && target!=null && target.isAlive() && !mob.isAlliedTo(target)
                && (!(mob instanceof GoblinEntity goblin) || mob.distanceToSqr(target)>=Math.pow(goblin.squad.panicRange(),2) || GoblinTactics.coverSafe(goblin))
                && mob.distanceToSqr(target)<24*24 && mob.getSensing().hasLineOfSight(target) && mob.hurtTime==0;
        }
        @Override public boolean canContinueToUse() {
            return time<34 && mob.getTarget()!=null && mob.getTarget().isAlive() && !mob.isAlliedTo(mob.getTarget())
                && (!(mob instanceof GoblinEntity) || mob.distanceToSqr(mob.getTarget())>=3*3)
                && mob.distanceToSqr(mob.getTarget())<28*28 && mob.getSensing().hasLineOfSight(mob.getTarget()) && mob.hurtTime==0;
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            time=0; released=false;
            spell = mob instanceof GoblinEntity ? SpellRegistry.MAGIC_ARROW_SPELL.get() : SpellRegistry.FIREBOLT_SPELL.get();
            mob.getNavigation().stop(); mob.getPersistentData().putBoolean("RiftfallCasting",true);
            spell.playSound(spell.getCastStartSound(),mob);
            GoblinAnimations.play(mob,GoblinAnimations.CHARGE);
        }
        @Override public void tick() {
            mob.getNavigation().stop();
            var target=mob.getTarget(); if (target==null) return;
            mob.getLookControl().setLookAt(target,90,90);
            mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
            var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
            if (patch!=null && patch.isStunned()) { time=34; return; }
            if (++time<20) ((ServerLevel)mob.level()).sendParticles(ParticleTypes.ENCHANT,mob.getX(),mob.getEyeY(),mob.getZ(),2,.25,.15,.25,0);
            if (time==20 && spell.isEnabled()) {
                int level=Math.min(spell.getMaxLevel(),Math.max(1,mob.getPersistentData().getInt("RiftfallSpellLevel")));
                if (spell.checkPreCastConditions(mob.level(),level,mob,data)) {
                    spell.onServerPreCast(mob.level(),level,mob,data);
                    spell.onCast(mob.level(),level,mob,CastSource.MOB,data);
                    spell.onServerCastComplete(mob.level(),level,mob,data,false);
                    released=true;
                    mob.getPersistentData().putInt("RiftfallSpellsCast",mob.getPersistentData().getInt("RiftfallSpellsCast")+1);
                }
                GoblinAnimations.play(mob,GoblinAnimations.RELEASE);
            }
            if (time==26) GoblinAnimations.play(mob,GoblinAnimations.RECOVER);
        }
        @Override public void stop() {
            mob.getPersistentData().putBoolean("RiftfallCasting",false);
            cooldown=released?100:60;
            if (time<26) GoblinAnimations.play(mob,GoblinAnimations.RECOVER);
            data.resetCastingState();
        }
    }
    private MobSpellcasting() {}
}

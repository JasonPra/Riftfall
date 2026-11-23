package org.Azkerash.riftfall;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import org.Azkerash.riftfall.behavior.skills.*;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** NPC adapter for Epic Fight's Step skill, aimed at an interception waypoint. */
final class GoblinInterceptDash implements MobActiveSkill {
    static void register() {
        MobSkillLoadout.registerAdapter(
                EpicFightSkills.STEP.get().getRegistryName(), new GoblinInterceptDash());
    }

    static void grant(GoblinEntity mob) {
        if (!mob.isRanged()
                && !mob.getPersistentData().getBoolean("RiftfallInterceptDashGranted")) {
            if (MobSkillLoadout.equipped(mob, SkillSlots.DODGE) == null) {
                MobSkillLoadout.equip(mob, SkillSlots.DODGE, EpicFightSkills.STEP.get());
            }
            mob.getPersistentData().putBoolean("RiftfallInterceptDashGranted", true);
        }
    }

    private Vec3 destination(GoblinEntity mob) {
        var plan = mob.squad.interception();
        return plan == null ? null : plan.position();
    }

    @Override
    public boolean canUse(Mob owner, LivingEntity target) {
        if (!(owner instanceof GoblinEntity mob)
                || mob.isRanged()
                || !mob.onGround()
                || mob.isInWaterOrBubble()
                || GoblinTactics.interrupted(mob)
                || mob.isUsingItem()
                        && !org.Azkerash.riftfall.shield.ShieldCollisions.isShield(
                                mob.getUseItem())) {
            return false;
        }
        var point = destination(mob);
        if (point == null || !dashGap(mob.position(), point)) {
            return false;
        }
        Vec3 direction = point.subtract(mob.position()).multiply(1, 0, 1).normalize();
        // Check the swept body and supporting floor before committing to root motion.
        for (int i = 1; i <= 4; i++) {
            var shift = direction.scale(i * .5);
            if (!mob.level().noCollision(mob, mob.getBoundingBox().move(shift))
                    || !mob.level()
                            .getBlockState(
                                    net.minecraft.core.BlockPos.containing(
                                            mob.position().add(shift).add(0, -.1, 0)))
                            .isFaceSturdy(
                                    mob.level(),
                                    net.minecraft.core.BlockPos.containing(
                                            mob.position().add(shift).add(0, -.1, 0)),
                                    net.minecraft.core.Direction.UP)) {
                return false;
            }
        }
        return EpicFightCapabilities.getEntityPatch(mob, GoblinPatch.class) != null;
    }

    static boolean dashGap(Vec3 origin, Vec3 destination) {
        double distance = origin.multiply(1, 0, 1).distanceToSqr(destination.multiply(1, 0, 1));
        return distance >= 2.5 * 2.5
                && distance <= 8 * 8
                && Math.abs(origin.y - destination.y) <= .5;
    }

    @Override
    public Execution start(Mob owner, LivingEntity target) {
        var mob = (GoblinEntity) owner;
        var point = destination(mob);
        var patch = EpicFightCapabilities.getEntityPatch(mob, GoblinPatch.class);
        if (point == null || patch == null) {
            return null;
        }
        float yaw = GoblinTactics.staffFacing(point.x - mob.getX(), point.z - mob.getZ());
        mob.getNavigation().stop();
        mob.setSprinting(false);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.stopUsingItem();
        mob.getPersistentData().putBoolean("RiftfallInterceptDashing", true);
        patch.playAnimationSynchronized(Animations.BIPED_STEP_FORWARD, 0);
        mob.getPersistentData()
                .putInt(
                        "RiftfallInterceptDashes",
                        mob.getPersistentData().getInt("RiftfallInterceptDashes") + 1);
        return new Execution() {
            int ticks;

            @Override
            public boolean running() {
                return ticks < 10 && mob.isAlive() && !GoblinTactics.interrupted(mob);
            }

            @Override
            public void tick() {
                ticks++;
            }

            @Override
            public void stop() {
                mob.getPersistentData().remove("RiftfallInterceptDashing");
            }
        };
    }

    @Override
    public int cooldownTicks() {
        return 80;
    }
}

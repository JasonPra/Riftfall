package org.Azkerash.riftfall.compat;

import dev.kubas.playersystem.dungeon.mob.behavior.Telegraph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Player System owns spell damage; its telegraph drives only these Epic Fight poses. */
public final class NativeOrcCast {
    public static final String UNTIL = "RiftfallOrcNativeCastUntil";
    public static final String WINDUP = "RiftfallOrcWindupTicks";

    public static boolean isOrc(Mob mob) {
        return mob instanceof org.Azkerash.riftfall.OrcEntity
                || BuiltInRegistries.ENTITY_TYPE
                        .getKey(mob.getType())
                        .toString()
                        .equals("player_system:orc_warrior");
    }

    public static boolean active(Mob mob) {
        return mob.isAlive()
                && isOrc(mob)
                && (mob.getPersistentData().getLong(UNTIL) > mob.level().getGameTime()
                        || !mob.level().isClientSide && Telegraph.isRunning(mob));
    }

    public static void begin(Mob mob, int ticks) {
        if (!mob.isAlive() || !isOrc(mob)) {
            return;
        }
        ticks = Math.clamp(ticks, 1, 12000);
        set(mob, ticks, ticks);
        var patch = EpicFightCapabilities.getEntityPatch(mob, LivingEntityPatch.class);
        if (patch != null) {
            patch.playAnimationSynchronized(OrcWarriorAnimations.SPELL_WINDUP, 0);
        }
    }

    public static void release(Mob mob) {
        if (mob == null || !mob.isAlive() || !isOrc(mob)) {
            return;
        }
        set(mob, 16, 0);
        var patch = EpicFightCapabilities.getEntityPatch(mob, LivingEntityPatch.class);
        if (patch != null) {
            patch.playAnimationSynchronized(OrcWarriorAnimations.SPELL_SLAM, 0);
        }
    }

    public static void cancel(Mob mob) {
        if (mob == null || !isOrc(mob) || mob.getPersistentData().getInt(WINDUP) <= 0) {
            return;
        }
        clear(mob);
        set(mob, 0, 0);
        // Never let a late cancellation replace the one-shot death clip with a looping idle.
        if (mob.isAlive()) {
            var patch = EpicFightCapabilities.getEntityPatch(mob, LivingEntityPatch.class);
            if (patch != null) {
                patch.playAnimationSynchronized(OrcWarriorAnimations.IDLE, 0);
            }
        }
    }

    public static void clear(Mob mob) {
        mob.getPersistentData().remove(UNTIL);
        mob.getPersistentData().remove(WINDUP);
    }

    private static void set(Mob mob, int duration, int windup) {
        mob.getPersistentData().putLong(UNTIL, mob.level().getGameTime() + duration);
        mob.getPersistentData().putInt(WINDUP, windup);
        PacketDistributor.sendToPlayersTrackingEntity(
                mob, new NativeOrcCastPayload(mob.getId(), mob.getUUID(), duration, windup));
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof Mob mob
                && active(mob)) {
            int remaining =
                    (int)
                            Math.clamp(
                                    mob.getPersistentData().getLong(UNTIL)
                                            - mob.level().getGameTime(),
                                    1,
                                    12000);
            PacketDistributor.sendToPlayer(
                    player,
                    new NativeOrcCastPayload(
                            mob.getId(),
                            mob.getUUID(),
                            remaining,
                            mob.getPersistentData().getInt(WINDUP)));
        }
    }

    private NativeOrcCast() {}
}

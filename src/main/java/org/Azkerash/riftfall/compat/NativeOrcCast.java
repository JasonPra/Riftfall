package org.Azkerash.riftfall.compat;

import dev.kubas.playersystem.dungeon.mob.behavior.Telegraph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Presentation handoff only. Player System still owns the telegraph callback and its damage. */
public final class NativeOrcCast {
    public static final String UNTIL = "RiftfallOrcNativeCastUntil";

    public static boolean isOrc(Mob mob) {
        return BuiltInRegistries.ENTITY_TYPE
                .getKey(mob.getType())
                .toString()
                .equals("player_system:orc_warrior");
    }

    public static boolean active(Mob mob) {
        return isOrc(mob)
                && (mob.getPersistentData().getLong(UNTIL) > mob.level().getGameTime()
                        || !mob.level().isClientSide && Telegraph.isRunning(mob));
    }

    public static void begin(Mob mob, int ticks) {
        if (!isOrc(mob)) {
            return;
        }
        var patch = EpicFightCapabilities.getEntityPatch(mob, LivingEntityPatch.class);
        if (patch != null) {
            patch.playAnimationSynchronized(Animations.BIPED_IDLE, 0);
        }
        set(mob, Math.min(12000, ticks + 2));
    }

    public static void release(Mob mob) {
        if (mob != null && isOrc(mob)) {
            set(mob, 16);
        }
    }

    public static void cancel(Mob mob) {
        if (mob != null && isOrc(mob)) {
            set(mob, 0);
        }
    }

    private static void set(Mob mob, int duration) {
        mob.getPersistentData().putLong(UNTIL, mob.level().getGameTime() + duration);
        PacketDistributor.sendToPlayersTrackingEntity(
                mob, new NativeOrcCastPayload(mob.getId(), mob.getUUID(), duration));
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
                    player, new NativeOrcCastPayload(mob.getId(), mob.getUUID(), remaining));
        }
    }

    private NativeOrcCast() {}
}

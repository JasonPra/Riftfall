package org.Azkerash.riftfall.compat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public record NativeOrcCastPayload(int entityId, UUID uuid, int duration, int windupTicks)
        implements CustomPacketPayload {
    public static final Type<NativeOrcCastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("riftfall", "native_orc_cast"));

    public NativeOrcCastPayload {
        if (duration < 0 || duration > 12000 || windupTicks < 0 || windupTicks > 12000) {
            throw new IllegalArgumentException("Invalid cast duration");
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, NativeOrcCastPayload> CODEC =
            new StreamCodec<>() {
                public NativeOrcCastPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new NativeOrcCastPayload(
                            buffer.readVarInt(),
                            buffer.readUUID(),
                            buffer.readVarInt(),
                            buffer.readVarInt());
                }

                public void encode(RegistryFriendlyByteBuf buffer, NativeOrcCastPayload payload) {
                    buffer.writeVarInt(payload.entityId);
                    buffer.writeUUID(payload.uuid);
                    buffer.writeVarInt(payload.duration);
                    buffer.writeVarInt(payload.windupTicks);
                }
            };

    @Override
    public Type<NativeOrcCastPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToClient(
                        TYPE,
                        CODEC,
                        (payload, context) -> {
                            var entity = context.player().level().getEntity(payload.entityId);
                            if (entity instanceof Mob mob
                                    && mob.getUUID().equals(payload.uuid)
                                    && NativeOrcCast.isOrc(mob)
                                    && mob.isAlive()) {
                                mob.getPersistentData()
                                        .putLong(
                                                NativeOrcCast.UNTIL,
                                                mob.level().getGameTime() + payload.duration);
                                mob.getPersistentData()
                                        .putInt(NativeOrcCast.WINDUP, payload.windupTicks);
                                var patch =
                                        yesman.epicfight.world.capabilities.EpicFightCapabilities
                                                .getEntityPatch(
                                                        mob,
                                                        yesman.epicfight.world.capabilities
                                                                .entitypatch.LivingEntityPatch
                                                                .class);
                                if (patch != null && payload.duration > 0) {
                                    var clip =
                                            payload.windupTicks > 0
                                                    ? OrcWarriorAnimations.SPELL_WINDUP
                                                    : OrcWarriorAnimations.SPELL_SLAM;
                                    patch.getAnimator().playAnimationInstantly(clip);
                                    float elapsed =
                                            payload.windupTicks > 0
                                                    ? 2F
                                                            * (1F
                                                                    - (float) payload.duration
                                                                            / payload.windupTicks)
                                                    : .8F - payload.duration * .05F;
                                    patch.getAnimator()
                                            .getPlayerFor(null)
                                            .setElapsedTime(Math.max(0, elapsed));
                                }
                            }
                        });
    }
}

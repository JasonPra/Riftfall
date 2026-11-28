package org.Azkerash.riftfall.compat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public record NativeOrcCastPayload(int entityId, UUID uuid, int duration)
        implements CustomPacketPayload {
    public static final Type<NativeOrcCastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("riftfall", "native_orc_cast"));

    public NativeOrcCastPayload {
        if (duration < 0 || duration > 12000) {
            throw new IllegalArgumentException("Invalid cast duration");
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, NativeOrcCastPayload> CODEC =
            new StreamCodec<>() {
                public NativeOrcCastPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new NativeOrcCastPayload(
                            buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
                }

                public void encode(RegistryFriendlyByteBuf buffer, NativeOrcCastPayload payload) {
                    buffer.writeVarInt(payload.entityId);
                    buffer.writeUUID(payload.uuid);
                    buffer.writeVarInt(payload.duration);
                }
            };

    @Override
    public Type<NativeOrcCastPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        TYPE,
                        CODEC,
                        (payload, context) -> {
                            var entity = context.player().level().getEntity(payload.entityId);
                            if (entity instanceof Mob mob
                                    && mob.getUUID().equals(payload.uuid)
                                    && NativeOrcCast.isOrc(mob)) {
                                mob.getPersistentData()
                                        .putLong(
                                                NativeOrcCast.UNTIL,
                                                mob.level().getGameTime() + payload.duration);
                            }
                        });
    }
}

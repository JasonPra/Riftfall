package org.Azkerash.riftfall;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Reports the lifetime of a local fear clip; the server retains authority over escape and timeout. */
public record GoblinVoicePayload(int entity,long sequence,int duration) implements CustomPacketPayload {
    public static final Type<GoblinVoicePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"goblin_voice"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GoblinVoicePayload> CODEC=new StreamCodec<>() {
        public GoblinVoicePayload decode(RegistryFriendlyByteBuf buffer){return new GoblinVoicePayload(buffer.readVarInt(),buffer.readVarLong(),buffer.readVarInt());}
        public void encode(RegistryFriendlyByteBuf buffer,GoblinVoicePayload packet){buffer.writeVarInt(packet.entity());buffer.writeVarLong(packet.sequence());buffer.writeVarInt(packet.duration());}
    };
    @Override public Type<GoblinVoicePayload> type(){return TYPE;}
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE,CODEC,(packet,context)-> {
            if(context.player() instanceof ServerPlayer player && player.serverLevel().getEntity(packet.entity()) instanceof GoblinEntity mob
                    && mob.isAlive() && mob.distanceToSqr(player)<=128*128) {
                if(packet.duration()==0)mob.voiceFinished(player.getUUID(),packet.sequence());
                else if(mob.isPanicking())mob.voiceStarted(player.getUUID(),packet.sequence(),packet.duration());
            }
        });
    }
}

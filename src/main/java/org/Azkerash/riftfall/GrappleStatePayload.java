package org.Azkerash.riftfall;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Victim input state works independently of the attacker's species or renderer. */
public record GrappleStatePayload(boolean active,int progress) implements CustomPacketPayload {
    public static final Type<GrappleStatePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"grapple_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GrappleStatePayload> CODEC=new StreamCodec<>(){
        public GrappleStatePayload decode(RegistryFriendlyByteBuf b){return new GrappleStatePayload(b.readBoolean(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,GrappleStatePayload p){b.writeBoolean(p.active);b.writeVarInt(p.progress);}
    };
    public GrappleStatePayload {if(progress<0||progress>GrappleStruggle.REQUIRED)throw new IllegalArgumentException("Invalid escape progress");}
    @Override public Type<GrappleStatePayload> type(){return TYPE;}
    static void send(ServerPlayer player,boolean active,int progress){PacketDistributor.sendToPlayer(player,new GrappleStatePayload(active,progress));}
    static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,(packet,context)->{
        var data=context.player().getPersistentData();data.putBoolean("RiftfallGrappled",packet.active);data.putInt("RiftfallGrappleProgress",packet.progress);
    });}
}

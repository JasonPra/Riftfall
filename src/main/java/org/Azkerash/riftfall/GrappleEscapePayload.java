package org.Azkerash.riftfall;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record GrappleEscapePayload() implements CustomPacketPayload {
    public static final GrappleEscapePayload INSTANCE=new GrappleEscapePayload();
    public static final Type<GrappleEscapePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"grapple_escape"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GrappleEscapePayload> CODEC=StreamCodec.unit(INSTANCE);
    @Override public Type<GrappleEscapePayload> type() { return TYPE; }
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE,CODEC,(packet,context)-> {
            if (context.player() instanceof ServerPlayer player) GoblinGrapple.struggle(player);
        });
    }
}

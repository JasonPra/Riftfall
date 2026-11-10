package org.Azkerash.riftfall;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** Victim model orientation is synchronized to observers; perspective changes affect only its owner. */
public record BackstabViewPayload(int entityId,UUID uuid,boolean active,float yaw) implements CustomPacketPayload {
    public static final Type<BackstabViewPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BackstabViewPayload> CODEC=new StreamCodec<>(){
        public BackstabViewPayload decode(RegistryFriendlyByteBuf b){return new BackstabViewPayload(b.readVarInt(),b.readUUID(),b.readBoolean(),b.readFloat());}
        public void encode(RegistryFriendlyByteBuf b,BackstabViewPayload p){b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeBoolean(p.active);b.writeFloat(p.yaw);}
    };
    public BackstabViewPayload {if(!Float.isFinite(yaw))throw new IllegalArgumentException("Invalid victim facing");}
    @Override public Type<BackstabViewPayload> type(){return TYPE;}
    static void send(ServerPlayer p,boolean active){send(p,active,p.getYRot());}
    static void send(ServerPlayer p,boolean active,float yaw){PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new BackstabViewPayload(p.getId(),p.getUUID(),active,yaw));}
    static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(packet,c)->{
        var entity=c.player().level().getEntity(packet.entityId);
        if(entity instanceof Player p && p.getUUID().equals(packet.uuid)){
            BackstabSequence.clientVictimView(p,packet.active,packet.yaw);
            if(p==c.player())org.Azkerash.riftfall.client.BackstabCameraState.victim(packet.active);
        }
    });}
}

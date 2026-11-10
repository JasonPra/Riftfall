package org.Azkerash.riftfall;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** Presentation notification only: existing positional sound playback remains unchanged. */
public record GoblinVocalCuePayload(int entityId,UUID uuid,int kind,float pitch) implements CustomPacketPayload {
    public static final int LAUGH=0,HURT=1,DEATH=2,GRUNT=3;
    public static final Type<GoblinVocalCuePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"goblin_vocal_cue"));
    public GoblinVocalCuePayload {if(kind<0||kind>3||!Float.isFinite(pitch)||pitch<=0||pitch>4)throw new IllegalArgumentException("Invalid vocal cue");}
    public static final StreamCodec<RegistryFriendlyByteBuf,GoblinVocalCuePayload> CODEC=new StreamCodec<>(){
        public GoblinVocalCuePayload decode(RegistryFriendlyByteBuf b){return new GoblinVocalCuePayload(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readFloat());}
        public void encode(RegistryFriendlyByteBuf b,GoblinVocalCuePayload p){b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.kind);b.writeFloat(p.pitch);}
    };
    @Override public Type<GoblinVocalCuePayload> type(){return TYPE;}
    static void send(GoblinEntity mob,int kind,float pitch){PacketDistributor.sendToPlayersTrackingEntityAndSelf(mob,new GoblinVocalCuePayload(mob.getId(),mob.getUUID(),kind,pitch));}
    static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(p,c)->{
        var entity=c.player().level().getEntity(p.entityId);
        if(entity instanceof GoblinEntity mob && mob.getUUID().equals(p.uuid))org.Azkerash.riftfall.client.GoblinVocalAnimation.cue(mob,p.kind,p.pitch);
    });}
}

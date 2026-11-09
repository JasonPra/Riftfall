package org.Azkerash.riftfall.behavior;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.Azkerash.riftfall.Riftfall;

/** Small, bounded debug frames sent only to the operator who opted in. */
public record BehaviorOverlayPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(int entityId,UUID uuid,List<String> lines) {
        public Entry {
            lines=List.copyOf(lines);Objects.requireNonNull(uuid);
            if (lines.size()>48 || lines.stream().anyMatch(line->line.length()>256)) throw new IllegalArgumentException("Oversized overlay entry");
        }
    }
    public BehaviorOverlayPayload { entries=List.copyOf(entries);if (entries.size()>32) throw new IllegalArgumentException("Oversized overlay frame"); }
    public static final Type<BehaviorOverlayPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"behavior_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BehaviorOverlayPayload> CODEC=new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buf,BehaviorOverlayPayload frame) {
            buf.writeVarInt(frame.entries.size());
            for (var entry:frame.entries) {
                buf.writeVarInt(entry.entityId);buf.writeUUID(entry.uuid);buf.writeVarInt(entry.lines.size());
                for (String line:entry.lines) buf.writeUtf(line,256);
            }
        }
        @Override public BehaviorOverlayPayload decode(RegistryFriendlyByteBuf buf) {
            int count=bounded(buf.readVarInt(),32);var entries=new ArrayList<Entry>();
            for (int i=0;i<count;i++) {
                int id=buf.readVarInt();UUID uuid=buf.readUUID();int lines=bounded(buf.readVarInt(),48);var text=new ArrayList<String>();
                for (int j=0;j<lines;j++) text.add(buf.readUtf(256));entries.add(new Entry(id,uuid,text));
            }
            return new BehaviorOverlayPayload(entries);
        }
    };
    private static int bounded(int count,int max) {
        if (count<0 || count>max) throw new IllegalArgumentException("Invalid debug frame size");return count;
    }
    @Override public Type<BehaviorOverlayPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE,CODEC,(frame,context)->
            context.enqueueWork(()->org.Azkerash.riftfall.client.BehaviorOverlayRenderer.accept(frame)));
    }
}

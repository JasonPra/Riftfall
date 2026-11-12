package org.Azkerash.riftfall.worldmana.geysers;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.Riftfall;

/** Temporary persisted SURFACE→CARVERS plan, removed once the water cap is sealed. */
public final class GeyserSurfacePlan {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<int[]>> PLAN=TYPES.register("geyser_surface_plan",()->AttachmentType.builder(()->new int[0])
        .serialize(Codec.INT_STREAM.xmap(java.util.stream.IntStream::toArray,java.util.Arrays::stream)).build());
    public static void register(IEventBus bus){TYPES.register(bus);}
    private GeyserSurfacePlan(){}
}

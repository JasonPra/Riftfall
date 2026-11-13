package org.Azkerash.riftfall.worldmana.geysers;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.Riftfall;

/** Persisted SURFACE→CARVERS plan: 256 packed floor/flags, followed by 256 local water levels.
 * Legacy 256-entry plans retain their sea-level water cap. Removed after sealing. */
public final class GeyserSurfacePlan {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<int[]>> PLAN=TYPES.register("geyser_surface_plan",()->AttachmentType.builder(()->new int[0])
        .serialize(Codec.INT_STREAM.xmap(java.util.stream.IntStream::toArray,java.util.Arrays::stream)).build());
    public static void register(IEventBus bus){TYPES.register(bus);}
    private GeyserSurfacePlan(){}
}

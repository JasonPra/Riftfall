package org.Azkerash.riftfall.mixin;

import net.minecraft.client.KeyMapping;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface WheelKeyMappingAccess {
    @Accessor("clickCount")
    int riftfall$clickCount();

    @Accessor("clickCount")
    void riftfall$clickCount(int value);
}

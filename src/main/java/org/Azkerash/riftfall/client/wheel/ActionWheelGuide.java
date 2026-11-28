package org.Azkerash.riftfall.client.wheel;

import dev.kubas.quintessence.client.QKeys;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Resolve key labels when the page is requested, never from the bindings' default keys. */
public final class ActionWheelGuide {
    public static MutableComponent controls() {
        var text =
                Component.translatable(
                        "guide.riftfall.controls.casting", QKeys.CAST.getTranslatedKeyMessage());
        text.append("\n\n")
                .append(
                        ActionWheels.OPEN.isUnbound()
                                ? Component.translatable("guide.riftfall.controls.wheel_unbound")
                                : Component.translatable(
                                        "guide.riftfall.controls.wheel",
                                        ActionWheels.OPEN.getTranslatedKeyMessage()));
        return text.append("\n\n").append(Component.translatable("guide.riftfall.controls.edit"));
    }

    private ActionWheelGuide() {}
}

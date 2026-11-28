package org.Azkerash.riftfall.client.wheel;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.Azkerash.riftfall.mixin.WheelKeyMappingAccess;
import org.lwjgl.glfw.GLFW;

/** Dispatch through each mod's existing client input path, after closing our screen. */
@EventBusSubscriber(modid = "riftfall", value = Dist.CLIENT)
public final class WheelActions {
    private static String pending;
    private static KeyMapping pressed;
    private static int remaining;
    private static Screen quintWheel;

    public static void queue(String id) {
        pending = id;
        if (Minecraft.getInstance().screen != null) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static boolean holdsQuintWheel(KeyMapping key) {
        return key == dev.kubas.quintessence.client.QKeys.WHEEL
                && quintWheel != null
                && Minecraft.getInstance().screen == quintWheel;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (quintWheel != null && mc.screen != quintWheel) {
            quintWheel = null;
        }
        if (pressed != null && (--remaining <= 0 || mc.player == null || mc.level == null)) {
            release();
        }
        if (pending == null) {
            return;
        }
        String id = pending;
        pending = null;
        if (mc.player == null || mc.screen != null || !ActionWheels.available(id)) {
            return;
        }
        if (id.equals("key.quintessence.wheel")) {
            quintWheel = new dev.kubas.quintessence.client.gui.SpellWheelScreen();
            mc.setScreen(quintWheel);
            return;
        }
        if (id.equals("key.irons_spellbooks.spell_wheel")
                || id.equals("key.irons_spellbooks.spell_wheel_toggle")) {
            press(io.redspace.ironsspellbooks.player.KeyMappings.SPELL_WHEEL_TOGGLE_KEYMAP);
            return;
        }
        if (id.startsWith("key.ars_nouveau.")) {
            ars(ActionWheels.key(id));
            return;
        }
        press(ActionWheels.key(id));
    }

    private static void press(KeyMapping key) {
        release();
        pressed = key;
        remaining = 2;
        key.setDown(true);
        var access = (WheelKeyMappingAccess) (Object) key;
        access.riftfall$clickCount(access.riftfall$clickCount() + 1);
    }

    private static void release() {
        if (pressed != null) {
            pressed.setDown(false);
            ((WheelKeyMappingAccess) (Object) pressed).riftfall$clickCount(0);
            pressed = null;
        }
    }

    private static void ars(KeyMapping key) {
        // Ars compares raw key codes. Borrow an unused function key for this one direct handler
        // call;
        // no global keyboard event is posted and no other mod's binding is pressed.
        var mc = Minecraft.getInstance();
        int unused = GLFW.GLFW_KEY_F25;
        while (unused >= GLFW.GLFW_KEY_F13) {
            boolean used = false;
            for (var candidate : mc.options.keyMappings) {
                if (candidate != key
                        && candidate.getKey().getType() == InputConstants.Type.KEYSYM
                        && candidate.getKey().getValue() == unused) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                break;
            }
            unused--;
        }
        if (unused < GLFW.GLFW_KEY_F13) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "wheel.riftfall.no_dispatch_key"),
                    true);
            return;
        }
        var original = key.getKey();
        try {
            key.setKey(InputConstants.Type.KEYSYM.getOrCreate(unused));
            com.hollingsworth.arsnouveau.client.keybindings.KeyHandler.checkKeysPressed(unused);
            if (mc.screen
                    instanceof
                    com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu<?> radial) {
                radial.setHoldToOpenGUI(false);
            }
        } finally {
            key.setKey(original);
        }
    }

    private WheelActions() {}
}

package org.Azkerash.riftfall.client.wheel;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;

import org.lwjgl.glfw.GLFW;

import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid = "riftfall", value = Dist.CLIENT)
public final class ActionWheels {
    public static final KeyMapping OPEN =
            new KeyMapping(
                    "key.riftfall.action_wheel",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_GRAVE_ACCENT,
                    "key.categories.riftfall");
    private static final Path FILE =
            FMLPaths.CONFIGDIR.get().resolve("riftfall-action-wheels.json");
    private static WheelSettings settings;
    private static final Map<String, KeyMapping> KEYS = new LinkedHashMap<>();
    private static boolean failed;

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (settings == null && !failed && mc.options != null && mc.player != null) {
            load();
        }
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null && settings != null) {
                mc.setScreen(new ActionWheelScreen());
            }
        }
    }

    public static WheelSettings settings() {
        return settings;
    }

    public static Collection<KeyMapping> keys() {
        return KEYS.values();
    }

    public static KeyMapping key(String id) {
        return KEYS.get(id);
    }

    public static Component label(String id) {
        return key(id) == null ? Component.literal(id) : Component.translatable(key(id).getName());
    }

    public static Component shortLabel(String id) {
        String alias = "wheel.riftfall.action." + id;
        return net.minecraft.client.resources.language.I18n.exists(alias)
                ? Component.translatable(alias)
                : label(id);
    }

    private static void load() {
        var mc = Minecraft.getInstance();
        for (var key : mc.options.keyMappings) {
            if (WheelLayout.eligible(key.getName(), key.getCategory())) {
                KEYS.putIfAbsent(key.getName(), key);
            }
        }
        try {
            settings =
                    Files.exists(FILE)
                            ? new Gson().fromJson(Files.readString(FILE), WheelSettings.class)
                            : defaults();
            if (settings == null || settings.version != 1) {
                throw new IllegalArgumentException("Unsupported wheel configuration");
            }
            settings.normalize();
            // Save the recovery copy before removing any direct key assignments.
            remember();
            if (!save()) {
                throw new java.io.IOException("Cannot save original key assignments");
            }
            applyBindings();
        } catch (Exception exception) {
            failed = true;
            settings = null;
            LogUtils.getLogger()
                    .error("Cannot load action wheels; direct bindings left intact", exception);
            mc.player.displayClientMessage(
                    Component.translatable("wheel.riftfall.load_error"), false);
        }
    }

    private static WheelSettings defaults() {
        var result = new WheelSettings();
        var menus = new WheelSettings.Group("Menus & books");
        var magic = new WheelSettings.Group("Magic");
        var combat = new WheelSettings.Group("Combat");
        var utility = new WheelSettings.Group("Utility");
        add(
                result,
                menus,
                true,
                "key.spell_atlas.open",
                "key.spell_atlas.search",
                "key.tempest_almanac.open",
                "key.arcane_framework.open_hub",
                "key.quintessence.codex",
                "key.epicfight.skill_gui",
                "key.epicfight.config",
                "key.curios.open.desc");
        add(
                result,
                magic,
                true,
                "key.irons_spellbooks.spell_wheel",
                "key.quintessence.wheel",
                "key.ars_nouveau.open_book",
                "key.ars_nouveau.selection_hud");
        add(
                result,
                combat,
                false,
                "key.epicfight.switch_mode",
                "key.epicfight.lock_on",
                "key.epicfight.dodge",
                "key.quintessence.blink",
                "key.quintessence.flight_dash");
        add(
                result,
                utility,
                true,
                "key.quintessence.thralls",
                "key.quintessence.next_set",
                "key.quintessence.prev_set",
                "key.epicfight.emote",
                "key.riftfall.held_item_bounds");
        result.wheels.addAll(List.of(menus, magic, combat, utility));
        return result;
    }

    private static void add(
            WheelSettings result, WheelSettings.Group group, boolean only, String... ids) {
        for (String id : ids) {
            if (KEYS.containsKey(id)) {
                group.bindings.add(id);
                if (only) {
                    result.wheelOnly.add(id);
                }
            }
        }
    }

    private static void remember() {
        for (String id : settings.wheelOnly) {
            var key = key(id);
            if (key != null && (!settings.originalKeys.containsKey(id) || !key.isUnbound())) {
                settings.originalKeys.put(
                        id,
                        new WheelSettings.SavedKey(
                                key.getKey().getName(), key.getKeyModifier().name()));
            }
        }
    }

    public static boolean changed() {
        settings.normalize();
        remember();
        if (!save()) {
            return false;
        }
        applyBindings();
        return true;
    }

    private static boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, settings.json());
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception exception) {
            LogUtils.getLogger().error("Cannot save action wheels", exception);
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.translatable("wheel.riftfall.save_error"), false);
            }
            return false;
        }
    }

    private static void applyBindings() {
        for (var entry : new ArrayList<>(settings.originalKeys.entrySet())) {
            var key = key(entry.getKey());
            if (key == null) {
                continue;
            }
            if (settings.wheelOnly.contains(entry.getKey())) {
                key.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
            } else {
                if (key.isUnbound()) {
                    try {
                        key.setKeyModifierAndCode(
                                KeyModifier.valueOf(entry.getValue().modifier()),
                                InputConstants.getKey(entry.getValue().key()));
                    } catch (RuntimeException exception) {
                        LogUtils.getLogger().warn("Invalid saved wheel binding {}", entry.getKey());
                    }
                }
                settings.originalKeys.remove(entry.getKey());
            }
        }
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        save();
    }

    public static void restoreAll() {
        settings.wheelOnly.clear();
        changed();
    }

    private ActionWheels() {}
}

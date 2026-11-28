package org.Azkerash.riftfall.compat;

import dev.kubas.playersystem.api.PlayerSystemApi;
import dev.kubas.playersystem.data.PlayerDataSync;
import dev.kubas.playersystem.data.TitleView;
import dev.kubas.playersystem.panel.PanelActionHandlers;
import dev.kubas.playersystem.title.TitleService;
import dev.kubas.spellatlas.AtlasConfig;
import dev.kubas.spellatlas.ProgressEvents;
import dev.kubas.spellatlas.SpellAtlas;
import dev.kubas.spellatlas.Titles;

import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Optional;

/** Presentation and selection only: never grants a title, AP, or a second set of bonuses. */
public final class AtlasTitleBridge {
    private static final String PREFIX = "riftfall:atlas/";
    private static final ThreadLocal<Boolean> SELECTING = ThreadLocal.withInitial(() -> false);

    public static String encode(String title) {
        return PREFIX + HexFormat.of().formatHex(title.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<String> decode(String id) {
        if (id == null || !id.startsWith(PREFIX) || id.length() > TitleView.MAX_TEXT) {
            return Optional.empty();
        }
        try {
            String title =
                    new String(
                            HexFormat.of().parseHex(id.substring(PREFIX.length())),
                            StandardCharsets.UTF_8);
            return !title.isEmpty() && encode(title).equals(id)
                    ? Optional.of(title)
                    : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static boolean isBridge(String id) {
        return id != null && id.startsWith(PREFIX);
    }

    public static TitleView view(ServerPlayer player, TitleView original) {
        if (!PlayerSystemApi.isAwakened(player) || !AtlasConfig.TITLES.get()) {
            return original;
        }
        var earned = Titles.earned(player);
        String worn = Titles.shown(player.getData(SpellAtlas.DISCOVERED));
        var owned = new ArrayList<>(original.owned());
        // Prefer the worn Atlas title when the remaining wire-view slots are limited.
        if (earned.contains(worn) && !worn.isEmpty()) {
            add(owned, worn);
        }
        for (String title : earned) {
            add(owned, title);
        }
        String selected = owned.contains(encode(worn)) ? encode(worn) : original.selected();
        return new TitleView(selected, owned);
    }

    private static void add(ArrayList<String> owned, String title) {
        String id = encode(title);
        if (owned.size() < TitleView.MAX_OWNED
                && id.length() <= TitleView.MAX_TEXT
                && !owned.contains(id)) {
            owned.add(id);
        }
    }

    public static boolean select(ServerPlayer player, String id) {
        if (!PanelActionHandlers.canAct(player) || !AtlasConfig.TITLES.get()) {
            return false;
        }
        var title = decode(id);
        if (title.isEmpty() || !Titles.earned(player).contains(title.get())) {
            return false;
        }
        SELECTING.set(true);
        try {
            TitleService.select(player, Optional.empty());
            if (!title.get().equals(player.getData(SpellAtlas.DISCOVERED).title())) {
                ProgressEvents.setTitle(player, title.get());
            } else {
                // A repeated click must not replay Atlas's title-change events or rewards.
                atlasSynced(player);
            }
        } finally {
            SELECTING.remove();
        }
        return true;
    }

    public static void nativeSelected(ServerPlayer player) {
        if (!SELECTING.get() && !player.getData(SpellAtlas.DISCOVERED).title().isEmpty()) {
            ProgressEvents.setTitle(player, "");
        }
    }

    public static void atlasSynced(ServerPlayer player) {
        if (!PlayerSystemApi.isAwakened(player)) {
            return;
        }
        if (!SELECTING.get() && !Titles.shown(player.getData(SpellAtlas.DISCOVERED)).isEmpty()) {
            SELECTING.set(true);
            try {
                TitleService.select(player, Optional.empty());
            } finally {
                SELECTING.remove();
            }
        }
        PlayerDataSync.send(player);
    }

    private AtlasTitleBridge() {}
}

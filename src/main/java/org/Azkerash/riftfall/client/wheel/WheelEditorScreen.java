package org.Azkerash.riftfall.client.wheel;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/** All eligible installed mappings are discoverable; Player System and vanilla are excluded. */
public final class WheelEditorScreen extends Screen {
    private int group, page;
    private String query = "";
    private EditBox search;
    private EditBox newName;
    private final List<Button> rows = new ArrayList<>();

    public WheelEditorScreen(Screen parent) {
        super(Component.translatable("wheel.riftfall.edit"));
    }

    private WheelSettings.Group selected() {
        return ActionWheels.settings().wheels.get(group);
    }

    @Override
    protected void init() {
        var groups = ActionWheels.settings().wheels;
        group = Math.floorMod(group, groups.size());
        int left = Math.max(10, width / 2 - 220);
        int span = Math.min(440, width - 20);
        addRenderableWidget(
                Button.builder(
                                Component.literal(selected().name + "  ›"),
                                b -> {
                                    group = (group + 1) % groups.size();
                                    page = 0;
                                    rebuildWidgets();
                                })
                        .bounds(left, 29, span - 68, 20)
                        .build());
        var delete =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable("wheel.riftfall.delete"),
                                        b -> {
                                            groups.remove(group);
                                            group = 0;
                                            page = 0;
                                            ActionWheels.changed();
                                            rebuildWidgets();
                                        })
                                .bounds(left + span - 64, 29, 64, 20)
                                .build());
        delete.active = groups.size() > 1;
        newName =
                new EditBox(
                        font,
                        left,
                        53,
                        span - 68,
                        20,
                        Component.translatable("wheel.riftfall.new_name"));
        newName.setMaxLength(32);
        newName.setHint(Component.translatable("wheel.riftfall.new_name"));
        addRenderableWidget(newName);
        addRenderableWidget(
                Button.builder(
                                Component.translatable("wheel.riftfall.create"),
                                b -> {
                                    String name = newName.getValue().trim();
                                    if (!name.isEmpty() && groups.size() < 16) {
                                        groups.add(new WheelSettings.Group(name));
                                        group = groups.size() - 1;
                                        page = 0;
                                        ActionWheels.changed();
                                        rebuildWidgets();
                                    }
                                })
                        .bounds(left + span - 64, 53, 64, 20)
                        .build());
        search =
                new EditBox(
                        font, left, 77, span, 20, Component.translatable("wheel.riftfall.search"));
        search.setHint(Component.translatable("wheel.riftfall.search"));
        search.setValue(query);
        search.setResponder(
                value -> {
                    query = value;
                    page = 0;
                    refreshRows();
                });
        addRenderableWidget(search);
        addRenderableWidget(
                Button.builder(
                                Component.literal("‹"),
                                b -> {
                                    page--;
                                    refreshRows();
                                })
                        .bounds(left, height - 52, 28, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.literal("›"),
                                b -> {
                                    page++;
                                    refreshRows();
                                })
                        .bounds(left + 32, height - 52, 28, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("wheel.riftfall.restore"),
                                b -> {
                                    ActionWheels.restoreAll();
                                    refreshRows();
                                })
                        .bounds(left + span - 178, height - 52, 178, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> onClose())
                        .bounds(width / 2 - 60, height - 27, 120, 20)
                        .build());
        refreshRows();
    }

    private List<KeyMapping> filtered() {
        String term = query.toLowerCase(Locale.ROOT);
        return ActionWheels.keys().stream()
                .filter(
                        k ->
                                (Component.translatable(k.getName()).getString()
                                                + " "
                                                + Component.translatable(k.getCategory())
                                                        .getString()
                                                + " "
                                                + k.getName())
                                        .toLowerCase(Locale.ROOT)
                                        .contains(term))
                .sorted(Comparator.comparing(k -> Component.translatable(k.getName()).getString()))
                .toList();
    }

    private int rowCount() {
        return Math.max(1, (height - 175) / 32 + 1);
    }

    private void refreshRows() {
        rows.forEach(this::removeWidget);
        rows.clear();
        var filtered = filtered();
        int count = rowCount(), pages = Math.max(1, (filtered.size() + count - 1) / count);
        page = Math.floorMod(page, pages);
        int left = Math.max(10, width / 2 - 220), span = Math.min(440, width - 20);
        for (int i = 0; i < count && page * count + i < filtered.size(); i++) {
            var key = filtered.get(page * count + i);
            String id = key.getName();
            boolean assigned = selected().bindings.contains(id);
            int y = 103 + i * 32;
            var add =
                    Button.builder(
                                    Component.literal(assigned ? "−" : "+"),
                                    b -> {
                                        if (selected().bindings.contains(id)) {
                                            selected().bindings.remove(id);
                                        } else {
                                            selected().bindings.add(id);
                                            ActionWheels.settings().wheelOnly.add(id);
                                        }
                                        ActionWheels.changed();
                                        refreshRows();
                                    })
                            .bounds(left + span - 140, y, 24, 20)
                            .build();
            rows.add(add);
            addRenderableWidget(add);
            var mode =
                    Button.builder(
                                    Component.translatable(
                                            ActionWheels.settings().wheelOnly.contains(id)
                                                    ? "wheel.riftfall.only"
                                                    : "wheel.riftfall.direct"),
                                    b -> {
                                        if (!ActionWheels.settings().wheelOnly.remove(id)) {
                                            ActionWheels.settings().wheelOnly.add(id);
                                        }
                                        ActionWheels.changed();
                                        refreshRows();
                                    })
                            .bounds(left + span - 112, y, 112, 20)
                            .build();
            mode.active = assigned;
            rows.add(mode);
            addRenderableWidget(mode);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partial) {
        graphics.fill(0, 0, width, height, 0xF0081524);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xE5F7FF);
        var list = filtered();
        int count = rowCount(),
                left = Math.max(10, width / 2 - 220),
                span = Math.min(440, width - 20);
        for (int i = 0; i < count && page * count + i < list.size(); i++) {
            var key = list.get(page * count + i);
            int y = 104 + i * 32;
            String label = Component.translatable(key.getName()).getString();
            graphics.drawString(
                    font, font.plainSubstrByWidth(label, span - 149), left, y, 0xFFFFFF);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(
                            Component.translatable(key.getCategory()).getString(), span - 149),
                    left,
                    y + 11,
                    0x829CB2);
            if (mx >= left && mx < left + span - 145 && my >= y && my < y + 23) {
                graphics.renderTooltip(
                        font,
                        List.of(
                                Component.literal(label),
                                Component.literal(key.getName()),
                                Component.translatable("wheel.riftfall.tap_hint")),
                        Optional.empty(),
                        mx,
                        my);
            }
        }
        graphics.drawString(
                font,
                Component.translatable(
                        "wheel.riftfall.page",
                        page + 1,
                        Math.max(1, (list.size() + count - 1) / count)),
                left + 66,
                height - 46,
                0xA5B9C9);
        super.render(graphics, mx, my, partial);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new ActionWheelScreen());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen paints its own background before its content.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

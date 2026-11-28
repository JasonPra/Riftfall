package org.Azkerash.riftfall.client.wheel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Nested, paged action wheels. Opening never pauses the world or activates a hovered entry. */
public final class ActionWheelScreen extends Screen {
    private static final net.minecraft.resources.ResourceLocation[] TEXTURES =
            java.util.stream.Stream.of(
                            "pages",
                            "runes",
                            "slashes",
                            "circuits",
                            "waves",
                            "scales",
                            "stars",
                            "crystals")
                    .map(
                            name ->
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                            "riftfall", "textures/gui/wheels/" + name + ".png"))
                    .toArray(net.minecraft.resources.ResourceLocation[]::new);
    private int group = -1;
    private int page;
    private int hovered = -1;
    private boolean openerReleased;
    private final java.util.Set<Integer> pressedNumbers = new java.util.HashSet<>();
    private double radius;
    private double cx, cy;

    public ActionWheelScreen() {
        super(Component.translatable("wheel.riftfall.title"));
    }

    @Override
    protected void init() {
        openerReleased = !openerDown();
        addRenderableWidget(
                Button.builder(
                                Component.translatable("wheel.riftfall.edit"),
                                b -> minecraft.setScreen(new WheelEditorScreen(this)))
                        .bounds(width / 2 - 60, height - 25, 120, 20)
                        .build());
    }

    private boolean openerDown() {
        var key = ActionWheels.OPEN.getKey();
        if (key.getValue() < 0) {
            return false;
        }
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.SCANCODE) {
            return ActionWheels.OPEN.isDown();
        }
        long window = minecraft.getWindow().getWindow();
        return key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
    }

    @Override
    public void tick() {
        if (!openerDown()) {
            openerReleased = true;
        }
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        pressedNumbers.remove(key);
        if (ActionWheels.OPEN.matches(key, scan)) {
            openerReleased = true;
            return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }

    private List<String> entries() {
        return group < 0
                ? ActionWheels.settings().wheels.stream().map(g -> g.name).toList()
                : ActionWheels.settings().wheels.get(group).bindings.stream()
                        .filter(id -> ActionWheels.key(id) != null)
                        .toList();
    }

    private int pages() {
        return Math.max(1, (entries().size() + WheelLayout.PAGE_SIZE - 1) / WheelLayout.PAGE_SIZE);
    }

    private List<String> shown() {
        var entries = entries();
        page = Math.floorMod(page, pages());
        int start = page * WheelLayout.PAGE_SIZE;
        return entries.subList(start, Math.min(start + WheelLayout.PAGE_SIZE, entries.size()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xAA030C18);
        graphics.fill(0, height - 65, width, height, 0xF0081524);
        cx = width / 2D;
        cy = height / 2D - 12;
        radius = Math.min(132, Math.min(width * .32, (height - 100) / 2D));
        var entries = shown();
        hovered =
                WheelLayout.hovered(mouseX - cx, mouseY - cy, radius * .36, radius, entries.size());
        graphics.drawCenteredString(
                font,
                group < 0
                        ? title
                        : Component.literal(ActionWheels.settings().wheels.get(group).name),
                width / 2,
                13,
                0xE5F7FF);
        for (int i = 0; i < entries.size(); i++) {
            wedge(graphics, i, entries.size(), i == hovered ? 0xFFFFFFFF : 0xFFD0D5DD);
            double angle = -Math.PI / 2 + i * Math.PI * 2 / entries.size();
            int x = (int) (cx + Math.cos(angle) * radius * .69),
                    y = (int) (cy + Math.sin(angle) * radius * .69);
            Component label =
                    group < 0
                            ? Component.literal(entries.get(i))
                            : ActionWheels.shortLabel(entries.get(i));
            int limit = (int) Math.max(42, radius * .75);
            float scale = 1F;
            var lines = font.split(label, limit);
            while (lines.size() * font.lineHeight * scale > 24) {
                scale *= .9F;
                lines = font.split(label, Math.max(1, (int) (limit / scale)));
            }
            graphics.drawCenteredString(font, Integer.toString(i + 1), x, y - 21, 0xD8ECF4);
            float top = y + 4 - lines.size() * font.lineHeight * scale / 2;
            graphics.pose().pushPose();
            graphics.pose().translate(x, top, 0);
            graphics.pose().scale(scale, scale, 1);
            for (int line = 0; line < lines.size(); line++) {
                graphics.drawCenteredString(
                        font, lines.get(line), 0, line * font.lineHeight, 0xFFFFFF);
            }
            graphics.pose().popPose();
        }
        graphics.drawCenteredString(
                font,
                Component.translatable(group < 0 ? "wheel.riftfall.center" : "wheel.riftfall.back"),
                (int) cx,
                (int) cy - 4,
                0xA7D8E8);
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("wheel.riftfall.empty"),
                    width / 2,
                    (int) cy + 18,
                    0xAEC2D0);
        }
        if (hovered >= 0) {
            var label =
                    group < 0
                            ? Component.literal(entries.get(hovered))
                            : ActionWheels.label(entries.get(hovered));
            float scale = 1F;
            var lines = font.split(label, width - 24);
            while (lines.size() * font.lineHeight * scale > 20) {
                scale *= .9F;
                lines = font.split(label, Math.max(1, (int) ((width - 24) / scale)));
            }
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2F, height - 62, 0);
            graphics.pose().scale(scale, scale, 1);
            for (int line = 0; line < lines.size(); line++) {
                graphics.drawCenteredString(
                        font, lines.get(line), 0, line * font.lineHeight, 0xFFFFFF);
            }
            graphics.pose().popPose();
        }
        graphics.drawCenteredString(
                font,
                Component.translatable("wheel.riftfall.hint", page + 1, pages()),
                width / 2,
                height - 43,
                0xA5B9C9);
        super.render(graphics, mouseX, mouseY, partial);
    }

    private void wedge(GuiGraphics graphics, int index, int count, int color) {
        graphics.flush();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        int style =
                Math.floorMod(
                        (group < 0 ? 0 : group * 2) + page * WheelLayout.PAGE_SIZE + index,
                        TEXTURES.length);
        RenderSystem.setShaderTexture(0, TEXTURES[style]);
        var builder =
                Tesselator.getInstance()
                        .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        var matrix = graphics.pose().last().pose();
        double step = Math.PI * 2 / count;
        double start = -Math.PI / 2 + (index - .5) * step + .018;
        double end = start + step - .036;
        for (int j = 0; j < 16; j++) {
            double a = start + (end - start) * j / 16, b = start + (end - start) * (j + 1) / 16;
            double inner = radius * .36;
            float x1 = (float) (cx + Math.cos(a) * inner), y1 = (float) (cy + Math.sin(a) * inner);
            float x2 = (float) (cx + Math.cos(a) * radius),
                    y2 = (float) (cy + Math.sin(a) * radius);
            float x3 = (float) (cx + Math.cos(b) * radius),
                    y3 = (float) (cy + Math.sin(b) * radius);
            float x4 = (float) (cx + Math.cos(b) * inner), y4 = (float) (cy + Math.sin(b) * inner);
            builder.addVertex(matrix, x1, y1, 0)
                    .setUv(
                            (float) ((x1 - cx) / (radius * 2) + .5),
                            (float) ((y1 - cy) / (radius * 2) + .5))
                    .setColor(color);
            builder.addVertex(matrix, x2, y2, 0)
                    .setUv(
                            (float) ((x2 - cx) / (radius * 2) + .5),
                            (float) ((y2 - cy) / (radius * 2) + .5))
                    .setColor(color);
            builder.addVertex(matrix, x3, y3, 0)
                    .setUv(
                            (float) ((x3 - cx) / (radius * 2) + .5),
                            (float) ((y3 - cy) / (radius * 2) + .5))
                    .setColor(color);
            builder.addVertex(matrix, x1, y1, 0)
                    .setUv(
                            (float) ((x1 - cx) / (radius * 2) + .5),
                            (float) ((y1 - cy) / (radius * 2) + .5))
                    .setColor(color);
            builder.addVertex(matrix, x3, y3, 0)
                    .setUv(
                            (float) ((x3 - cx) / (radius * 2) + .5),
                            (float) ((y3 - cy) / (radius * 2) + .5))
                    .setColor(color);
            builder.addVertex(matrix, x4, y4, 0)
                    .setUv(
                            (float) ((x4 - cx) / (radius * 2) + .5),
                            (float) ((y4 - cy) / (radius * 2) + .5))
                    .setColor(color);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void select(int index) {
        var entries = shown();
        if (index < 0 || index >= entries.size()) {
            return;
        }
        if (group < 0) {
            group = page * WheelLayout.PAGE_SIZE + index;
            page = 0;
        } else {
            WheelActions.queue(entries.get(index));
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (ActionWheels.OPEN.matchesMouse(button)) {
            if (openerReleased) {
                onClose();
            }
            return true;
        }
        if (super.mouseClicked(x, y, button)) {
            return true;
        }
        if (button == 1 || (Math.hypot(x - cx, y - cy) < radius * .36 && group >= 0)) {
            group = -1;
            page = 0;
            return true;
        }
        if (button == 0) {
            select(WheelLayout.hovered(x - cx, y - cy, radius * .36, radius, shown().size()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (ActionWheels.OPEN.matchesMouse(button)) {
            openerReleased = true;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy != 0) {
            page = Math.floorMod(page + (dy < 0 ? 1 : -1), pages());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (ActionWheels.OPEN.matches(key, scan)) {
            if (openerReleased) {
                onClose();
            }
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + WheelLayout.PAGE_SIZE) {
            if (pressedNumbers.add(key)) {
                select(key - GLFW.GLFW_KEY_1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE || (key == GLFW.GLFW_KEY_ESCAPE && group >= 0)) {
            group = -1;
            page = 0;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
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

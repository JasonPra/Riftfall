package org.Azkerash.riftfall.client.titles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.Azkerash.riftfall.Riftfall;

/** Biome-only, Souls-inspired area names, independent of Minecraft's story titles. */
@EventBusSubscriber(modid = Riftfall.MODID, value = Dist.CLIENT)
public final class ArrivalTitles {
    private static final ArrivalTracker TRACKER = new ArrivalTracker();
    private static final SoundEvent DISCOVERY = SoundEvent.createVariableRangeEvent(
        ResourceLocation.fromNamespaceAndPath(Riftfall.MODID, "biome_discovery"));
    private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(Riftfall.MODID, "area_title");
    private static final int FADE_IN = 20, HOLD = 60, FADE_OUT = 30, DURATION = FADE_IN + HOLD + FADE_OUT;
    private static Component title;
    private static int sparkleSeed;
    private static ResourceLocation dimension;
    private static int age;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ArrivalTitleConfig.ENABLED.get()) {
            TRACKER.reset(); title = null; dimension = null; return;
        }
        if (mc.isPaused()) return;
        if (!mc.level.dimension().location().equals(dimension)) {
            dimension = mc.level.dimension().location(); TRACKER.reset(); title = null;
        }
        if (title != null && ++age >= DURATION) title = null;
        if (!mc.level.hasChunkAt(mc.player.blockPosition())) return;
        var biome = mc.level.getBiome(mc.player.blockPosition()).unwrapKey();
        if (biome.isEmpty()) return;
        String arrival = TRACKER.tick(biome.get().location().toString(), 30, 200);
        if (arrival == null) return;
        var id = ResourceLocation.parse(arrival);
        String key = "biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        title = (I18n.exists(key) ? Component.translatable(key) : Component.literal(readable(id.getPath())))
            .withStyle(Style.EMPTY.withFont(FONT));
        age = 0;
        sparkleSeed = arrival.hashCode();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(DISCOVERY, 1F, .45F));
    }

    private static String readable(String path) {
        var words = path.replace('_', ' ').replace('/', ' ').split(" ");
        for (int i = 0; i < words.length; i++) if (!words[i].isEmpty())
            words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
        return String.join(" ", words);
    }

    public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (title == null || mc.level == null || mc.options.hideGui || !ArrivalTitleConfig.ENABLED.get()) return;
        float time = age + delta.getGameTimeDeltaPartialTick(false);
        int alpha = (int)(255 * ArrivalTracker.opacity(time, FADE_IN, HOLD, FADE_OUT));
        if (alpha < 4) return;
        int width = Math.max(1, mc.font.width(title));
        float scale = Math.min(2.4F, (graphics.guiWidth() - 32F) / width);
        float x = graphics.guiWidth() / 2F, y = graphics.guiHeight() * .43F;
        int halfRule = Math.min(graphics.guiWidth() / 2 - 16, Math.max(85, (int)(width * scale / 2) + 20));
        // Taper the rules at their ends instead of drawing an opaque banner over the view.
        for (int dx = -halfRule; dx <= halfRule; dx++) {
            float edge = Math.min(1F, (halfRule - Math.abs(dx)) / 24F);
            int ruleColor = ((int)(alpha * .65F * edge) << 24) | 0xDEDACB;
            graphics.fill((int)x + dx, (int)y - 6, (int)x + dx + 1, (int)y - 5, ruleColor);
            graphics.fill((int)x + dx, (int)(y + 25), (int)x + dx + 1, (int)(y + 26), ruleColor);
        }
        float left = x - width * scale / 2F;
        float right = left + width * scale;
        float front = left + (width * scale + 12F) * TitleReveal.progress(time);
        // An uninterrupted, feathered sweep reveals the actual glyphs, rather than adding letters.
        if (time >= TitleReveal.REVEAL_TICKS) {
            lettering(graphics, x, y, scale, width, (alpha << 24) | 0xEEECE3);
        } else {
            float feather = 12F;
            letteringSlice(graphics, x, y, scale, width, (alpha << 24) | 0xEEECE3,
                left - 5, front - feather);
            for (int i = 0; i < 8; i++) {
                float start = front - feather + i * feather / 8;
                float brightness = 1 - i / 8F;
                int sliceAlpha = (int)(alpha * brightness);
                if (sliceAlpha >= 4) letteringSlice(graphics, x, y, scale, width,
                    (sliceAlpha << 24) | 0xFFF2CE, start, start + feather / 8);
            }
        }
        // Deterministic screen-space stars: no world particles or frame-to-frame random jitter.
        for (int i = 0; i < TitleReveal.SPARKS; i++) {
            var spark = TitleReveal.spark(i, time, sparkleSeed);
            if (spark == null) continue;
            float sx = left + (right - left) * spark.progress() + spark.driftX();
            float sy = y + 10 + spark.driftY();
            star(graphics, sx, sy, spark.size(), (int)(alpha * spark.opacity()), i % 3 == 0 ? 0xCDEEFF : 0xFFF0CB);
        }
    }
    private static void lettering(GuiGraphics graphics, float x, float y, float scale, int width, int color) {
        var pose = graphics.pose();
        pose.pushPose(); pose.translate(x, y, 0); pose.scale(scale, scale, 1);
        graphics.drawString(Minecraft.getInstance().font, title, -width / 2, 0, color, true);
        pose.popPose();
    }
    private static void letteringSlice(GuiGraphics graphics, float x, float y, float scale, int width,
                                       int color, float start, float end) {
        if (end <= start) return;
        graphics.enableScissor((int)Math.floor(start), (int)y - 10, (int)Math.floor(end), (int)(y + 40));
        lettering(graphics, x, y, scale, width, color);
        graphics.disableScissor();
    }
    private static void star(GuiGraphics graphics, float x, float y, float radius, int alpha, int rgb) {
        if (alpha < 4) return;
        int cx = Math.round(x), cy = Math.round(y), r = Math.max(1, Math.round(radius));
        int glow = ((alpha / 7) << 24) | rgb;
        graphics.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, glow);
        int arms = ((alpha / 2) << 24) | rgb;
        graphics.fill(cx - r, cy, cx + r + 1, cy + 1, arms);
        graphics.fill(cx, cy - r, cx + 1, cy + r + 1, arms);
        graphics.fill(cx, cy, cx + 1, cy + 1, (alpha << 24) | 0xFFFFFF);
    }
    private ArrivalTitles() {}
}

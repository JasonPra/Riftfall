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
        int alpha = (int)(255 * ArrivalTracker.opacity(age + delta.getGameTimeDeltaPartialTick(false), FADE_IN, HOLD, FADE_OUT));
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
        var pose = graphics.pose();
        pose.pushPose(); pose.translate(x, y, 0); pose.scale(scale, scale, 1);
        graphics.drawString(mc.font, title, -width / 2, 0, (alpha << 24) | 0xEEECE3, true);
        pose.popPose();
    }
    private ArrivalTitles() {}
}

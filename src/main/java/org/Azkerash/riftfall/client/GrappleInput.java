package org.Azkerash.riftfall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GrappleEscapePayload;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class GrappleInput {
    private static boolean prompted;
    private static GoblinEntity attacker() {
        var mc=Minecraft.getInstance();
        if (mc.player==null || mc.level==null) return null;
        return mc.level.getEntitiesOfClass(GoblinEntity.class,mc.player.getBoundingBox().inflate(3),
            g->g.isAlive() && g.grappledPlayerId()==mc.player.getId()).stream().findFirst().orElse(null);
    }
    @SubscribeEvent(receiveCanceled=true,priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre event) {
        var mc=Minecraft.getInstance();
        if (mc.screen==null && event.getAction()==1 && mc.options.keyAttack.matchesMouse(event.getButton()) && attacker()!=null) {
            PacketDistributor.sendToServer(GrappleEscapePayload.INSTANCE); event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void keyboard(InputEvent.Key event) {
        var mc=Minecraft.getInstance();
        if (mc.screen==null && event.getAction()==1 && mc.options.keyAttack.matches(event.getKey(),event.getScanCode()) && attacker()!=null)
            PacketDistributor.sendToServer(GrappleEscapePayload.INSTANCE);
    }
    @SubscribeEvent(receiveCanceled=true) public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || attacker()==null) return;
        event.setCanceled(true); event.setSwingHand(false);
    }
    @SubscribeEvent public static void movement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (attacker()==null) return;
        event.getInput().forwardImpulse=0; event.getInput().leftImpulse=0; event.getInput().jumping=false;
    }
    @SubscribeEvent public static void prompt(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance(); var goblin=attacker();
        if (mc.player==null) { prompted=false; return; }
        if (goblin!=null) {
            mc.player.displayClientMessage(Component.translatable("message.riftfall.grapple_escape",
                mc.options.keyAttack.getTranslatedKeyMessage(),goblin.escapeProgress(),6),true);
            prompted=true;
        } else if (prompted) { mc.player.displayClientMessage(Component.empty(),true); prompted=false; }
    }
    private GrappleInput() {}
}

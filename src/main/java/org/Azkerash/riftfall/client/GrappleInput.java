package org.Azkerash.riftfall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.Azkerash.riftfall.GrappleEscapePayload;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class GrappleInput {
    private static boolean prompted;
    private static boolean restrained() {
        var player=Minecraft.getInstance().player;
        return player!=null && player.getPersistentData().getBoolean("RiftfallGrappled");
    }
    @SubscribeEvent(receiveCanceled=true,priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre event) {
        var mc=Minecraft.getInstance();
        if (mc.screen==null && event.getAction()==1 && mc.options.keyAttack.matchesMouse(event.getButton()) && restrained()) {
            PacketDistributor.sendToServer(GrappleEscapePayload.INSTANCE); event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void keyboard(InputEvent.Key event) {
        var mc=Minecraft.getInstance();
        if (mc.screen==null && event.getAction()==1 && mc.options.keyAttack.matches(event.getKey(),event.getScanCode()) && restrained())
            PacketDistributor.sendToServer(GrappleEscapePayload.INSTANCE);
    }
    @SubscribeEvent(receiveCanceled=true) public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || !restrained()) return;
        event.setCanceled(true); event.setSwingHand(false);
    }
    @SubscribeEvent public static void movement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!restrained()) return;
        event.getInput().forwardImpulse=0; event.getInput().leftImpulse=0; event.getInput().jumping=false;
    }
    @SubscribeEvent public static void prompt(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if (mc.player==null) { prompted=false; return; }
        if (restrained()) {
            mc.player.displayClientMessage(Component.translatable("message.riftfall.grapple_escape",
                mc.options.keyAttack.getTranslatedKeyMessage(),mc.player.getPersistentData().getInt("RiftfallGrappleProgress"),6),true);
            prompted=true;
        } else if (prompted) { mc.player.displayClientMessage(Component.empty(),true); prompted=false; }
    }
    private GrappleInput() {}
}

package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.Azkerash.riftfall.Riftfall;
import org.lwjgl.glfw.GLFW;

/** Model alignment bounds complement Epic Fight's own attack-collider debugging. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class HeldItemDebug {
    public static final KeyMapping KEY=new KeyMapping("key.riftfall.held_item_bounds",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.riftfall_debug");
    private static final Map<BakedModel,AABB> CACHE=new WeakHashMap<>();
    private static boolean enabled;
    private static int toggle() {
        enabled=!enabled;
        message("Held item MODEL bounds "+(enabled ? "ON" : "OFF")+". Cyan: shields; gold: other items. F3+B shows hurtboxes and Epic Fight attack colliders.");
        return 1;
    }
    private static void message(String text) {
        if (Minecraft.getInstance().player!=null) Minecraft.getInstance().player.displayClientMessage(Component.literal(text),false);
    }
    @SubscribeEvent public static void keys(ClientTickEvent.Post event) {
        while (KEY.consumeClick()) if (Minecraft.getInstance().player!=null) toggle();
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("debug")
            .then(Commands.literal("helditems").executes(c->toggle()))
            .then(Commands.literal("colliders").executes(c->{
                var dispatcher=Minecraft.getInstance().getEntityRenderDispatcher();
                boolean show=!dispatcher.shouldRenderHitBoxes(); dispatcher.setRenderHitBoxes(show);
                message("Hurtboxes and Epic Fight attack colliders "+(show ? "ON" : "OFF")); return 1;
            }))
            .then(Commands.literal("off").executes(c->{ enabled=false;
                Minecraft.getInstance().getEntityRenderDispatcher().setRenderHitBoxes(false);
                message("Weapon debugging OFF");return 1;
            }))));
    }
    public static void draw(ItemStack stack,ItemDisplayContext context,PoseStack poses,MultiBufferSource buffers,BakedModel model) {
        if (!enabled || stack.isEmpty() || !(context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) return;
        AABB bounds;
        if (model.isCustomRenderer()) {
            if (!stack.is(Items.SHIELD)) return;
            // Vanilla ShieldModel plate + handle, including the renderer's Y/Z flip.
            bounds=new AABB(-6/16D,-11/16D,-5/16D,6/16D,11/16D,2/16D);
        } else bounds=CACHE.computeIfAbsent(model,m->bounds(m,stack));
        if (bounds==null) return;
        boolean shield=stack.getItem() instanceof ShieldItem;
        LevelRenderer.renderLineBox(poses,buffers.getBuffer(RenderType.lines()),bounds.inflate(.002),
            shield ? .1F : 1F,shield ? 1F : .75F,shield ? 1F : .1F,1F);
    }
    private static AABB bounds(BakedModel model,ItemStack stack) {
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        var random=RandomSource.create(42);
        for (var pass:model.getRenderPasses(stack,true)) for (int face=-1;face<6;face++) {
            random.setSeed(42);
            for (var quad:pass.getQuads(null,face<0 ? null : Direction.values()[face],random)) {
                int[] vertices=quad.getVertices();int stride=vertices.length/4;
                for (int i=0;i<4;i++) {
                    double x=Float.intBitsToFloat(vertices[i*stride]),y=Float.intBitsToFloat(vertices[i*stride+1]),z=Float.intBitsToFloat(vertices[i*stride+2]);
                    minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);
                    maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);
                }
            }
        }
        return Double.isFinite(minX) ? new AABB(minX,minY,minZ,maxX,maxY,maxZ) : null;
    }
    private HeldItemDebug() {}
}

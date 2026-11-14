package org.Azkerash.riftfall.client.fauna;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.fauna.ManaheartFauna;
@EventBusSubscriber(modid="riftfall",value=Dist.CLIENT)
public final class RyRenderer extends WolfRenderer {
 private static final ResourceLocation FUR=ResourceLocation.fromNamespaceAndPath("riftfall","textures/entity/ry.png");
 public RyRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public ResourceLocation getTextureLocation(Wolf wolf){return FUR;}
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ManaheartFauna.RY.get(),RyRenderer::new);}
}

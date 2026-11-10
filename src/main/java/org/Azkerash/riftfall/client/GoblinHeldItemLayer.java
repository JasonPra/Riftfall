package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.Azkerash.riftfall.GoblinEntity;

/** The digging knife is cosmetic; saved equipment, loot and durability are never replaced. */
public final class GoblinHeldItemLayer extends ItemInHandLayer<GoblinEntity,HumanoidModel<GoblinEntity>> {
    public static ItemStack knife(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riftfall","goblin_shortsword")));}
    private final ItemStack knife=knife();
    public GoblinHeldItemLayer(RenderLayerParent<GoblinEntity,HumanoidModel<GoblinEntity>> parent,ItemInHandRenderer renderer){super(parent,renderer);}
    @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,GoblinEntity mob,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(!mob.isBurrowing()){super.render(stack,buffers,light,mob,limb,amount,partial,age,yaw,pitch);return;}
        renderArmWithItem(mob,knife,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,HumanoidArm.RIGHT,stack,buffers,light);
    }
}

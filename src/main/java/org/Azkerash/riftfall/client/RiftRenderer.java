package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.Azkerash.riftfall.RiftEntity;

/** A two-sided vertical aperture. Only its texture and energy rim animate. */
public final class RiftRenderer extends EntityRenderer<RiftEntity> {
    private static final ResourceLocation PORTAL = ResourceLocation.withDefaultNamespace("block/nether_portal");
    private static final int SEGMENTS = 64;

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public void render(RiftEntity entity, float yaw, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int packedLight) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(PORTAL);
        // Minecraft advances the vanilla portal sprite's frames in the block atlas.
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS, false));
        float time = entity.tickCount + partialTick;
        poses.pushPose();
        // Never use the camera rotation: the aperture keeps its world orientation.
        poses.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        for (int i = 0; i < SEGMENTS; i++) {
            float a = Mth.TWO_PI * i / SEGMENTS;
            float b = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float ax = Mth.cos(a), ay = Mth.sin(a);
            float bx = Mth.cos(b), by = Mth.sin(b);
            float pulseA = 1.04F + .025F * Mth.sin(time * .12F + a * 7);
            float pulseB = 1.04F + .025F * Mth.sin(time * .12F + b * 7);
            // Degenerate quads form a triangle fan for the animated interior.
            vertex(vertices, poses, sprite, 0, 0, 0, 1, 1, 1, .94F);
            vertex(vertices, poses, sprite, ax, ay, 0, 1, 1, 1, .94F);
            vertex(vertices, poses, sprite, bx, by, 0, 1, 1, 1, .94F);
            vertex(vertices, poses, sprite, 0, 0, 0, 1, 1, 1, .94F);
            // A narrow, rippling energy rim changes to a hot red tint on a break.
            float green = entity.isBroken() ? .18F : .65F;
            float blue = entity.isBroken() ? .25F : 1F;
            vertex(vertices, poses, sprite, ax, ay, -.002F, 1, green, blue, 1);
            vertex(vertices, poses, sprite, ax * pulseA, ay * pulseA, -.002F, 1, green, blue, .45F);
            vertex(vertices, poses, sprite, bx * pulseB, by * pulseB, -.002F, 1, green, blue, .45F);
            vertex(vertices, poses, sprite, bx, by, -.002F, 1, green, blue, 1);
        }
        poses.popPose();
        super.render(entity, yaw, partialTick, poses, buffers, packedLight);
    }

    private static void vertex(VertexConsumer out, PoseStack poses, TextureAtlasSprite sprite,
                               float x, float y, float z, float red, float green, float blue, float alpha) {
        float u = Mth.clamp((x + 1) * .5F, 0, 1);
        float v = Mth.clamp((1 - y) * .5F, 0, 1);
        out.addVertex(poses.last(), x * .9F, 1.5F + y * 1.4F, z)
            .setColor(red, green, blue, alpha).setUv(sprite.getU(u), sprite.getV(v))
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poses.last(), 0, 0, 1);
    }

    @Override public ResourceLocation getTextureLocation(RiftEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}

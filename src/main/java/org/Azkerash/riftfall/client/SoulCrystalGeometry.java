package org.Azkerash.riftfall.client;

import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.Azkerash.riftfall.CrystalFormation;
import org.joml.Vector3f;

/** Faceted six-sided prisms with solid luminous hearts and a connected mineral bed. */
public record SoulCrystalGeometry(int crystals,int variant,int connections,String part) implements IUnbakedGeometry<SoulCrystalGeometry> {
    @Override public BakedModel bake(IGeometryBakingContext context,ModelBaker baker,
            Function<Material,TextureAtlasSprite> sprites,ModelState state,ItemOverrides overrides) {
        TextureAtlasSprite body=sprites.apply(context.getMaterial("crystal"));
        TextureAtlasSprite core=sprites.apply(context.getMaterial("core"));
        TextureAtlasSprite bed=sprites.apply(context.getMaterial("bed"));
        var builder=new SimpleBakedModel.Builder(false,true,true,context.getTransforms(),overrides).particle(body);
        if (!part.equals("bed")) for (var prism : CrystalFormation.crystals(crystals,variant)) {
            // Bake the heart first, then the denser outer mineral. No glass outlines.
            var heart=new CrystalFormation.Prism(prism.root().lerp(prism.tip(),.12),prism.root().lerp(prism.tip(),.92),prism.radius()*.38);
            prism(builder,state,core,heart,true);
            prism(builder,state,body,prism,false);
        }
        if (!part.equals("crystals")) {
            var outline=CrystalFormation.bed(connections);
            Vec3 center=new Vec3(8,.8,8);
            for (int i=0;i<outline.size();i++) {
                Vec3 a=outline.get(i),b=outline.get((i+1)%outline.size());
                quad(builder,state,bed,center,b,a,a,185,192,196,0,true);
                // Omit the internal face where two mineral patches merge.
                boolean shared=(a.x==b.x && (a.x==0 || a.x==16)) || (a.z==b.z && (a.z==0 || a.z==16));
                if (!shared) quad(builder,state,bed,a,b,new Vec3(b.x,0,b.z),new Vec3(a.x,0,a.z),165,177,181,0,true);
            }
        }
        return builder.build(context.getRenderType(ResourceLocation.withDefaultNamespace("translucent")));
    }
    private static void prism(SimpleBakedModel.Builder builder,ModelState state,TextureAtlasSprite sprite,CrystalFormation.Prism p,boolean core) {
        for (int i=0;i<6;i++) {
            Vec3 a=p.vertex(i,false),b=p.vertex((i+1)%6,false),c=p.vertex((i+1)%6,true),d=p.vertex(i,true);
            int light=core ? 15 : 8;
            int shade=core ? 255 : new int[]{206,235,180,214,248,192}[i];
            quad(builder,state,sprite,a,b,c,d,shade,shade,shade,light,false);
            quad(builder,state,sprite,d,c,p.tip(),p.tip(),Math.min(255,shade+12),Math.min(255,shade+12),Math.min(255,shade+12),light,false);
        }
    }
    private static void quad(SimpleBakedModel.Builder builder,ModelState state,TextureAtlasSprite sprite,
            Vec3 a,Vec3 b,Vec3 c,Vec3 d,int red,int green,int blue,int light,boolean surfaceUV) {
        Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();
        if (normal.lengthSqr()<.01) return;
        Vector3f n=new Vector3f((float)normal.x,(float)normal.y,(float)normal.z);
        state.getRotation().getMatrix().transformDirection(n); n.normalize();
        var consumer=new QuadBakingVertexConsumer();
        consumer.setSprite(sprite); consumer.setShade(false); consumer.setHasAmbientOcclusion(false);
        consumer.setDirection(Direction.getNearest(n.x,n.y,n.z));
        Vec3[] vertices={a,b,c,d};
        float[][] uv={{0,16},{16,16},{16,0},{0,0}};
        for (int i=0;i<4;i++) {
            Vec3 point=vertices[i];
            Vector3f v=new Vector3f((float)point.x/16-.5F,(float)point.y/16-.5F,(float)point.z/16-.5F);
            state.getRotation().getMatrix().transformPosition(v); v.add(.5F,.5F,.5F);
            float u=surfaceUV ? (float)point.x : uv[i][0], t=surfaceUV ? (float)point.z : uv[i][1];
            consumer.addVertex(v.x,v.y,v.z).setColor(red,green,blue,255).setUv(sprite.getU(u/16),sprite.getV(t/16))
                .setUv2(light*16,light*16).setNormal(n.x,n.y,n.z);
        }
        builder.addUnculledFace(consumer.bakeQuad());
    }
}

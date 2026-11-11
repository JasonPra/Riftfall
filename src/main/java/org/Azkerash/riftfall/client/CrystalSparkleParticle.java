package org.Azkerash.riftfall.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.crystals.CrystalParticles;

/** Brief, stationary mineral glints; RGB parameters are color, never velocity. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class CrystalSparkleParticle extends TextureSheetParticle {
    private CrystalSparkleParticle(ClientLevel level,double x,double y,double z,double r,double g,double b,SpriteSet sprites){
        super(level,x,y,z);xd=yd=zd=0;hasPhysics=false;
        lifetime=8+random.nextInt(7);quadSize=.085f+random.nextFloat()*.045f;
        roll=oRoll=(random.nextFloat()-.5f)*.6f;
        setColor(.65f+.35f*(float)r,.65f+.35f*(float)g,.65f+.35f*(float)b);pickSprite(sprites);
    }
    @Override public float getQuadSize(float partialTick){float t=Math.min(1,(age+partialTick)/lifetime);return quadSize*(t<.2f?t/.2f:(1-t)/.8f);}
    @Override public void tick(){super.tick();alpha=Math.max(0,1-(float)age/lifetime);}
    @Override public int getLightColor(float partialTick){return 0xF000F0;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(CrystalParticles.SPARKLE.get(),sprites->(SimpleParticleType type,ClientLevel level,double x,double y,double z,double r,double g,double b)->new CrystalSparkleParticle(level,x,y,z,r,g,b,sprites));
    }
}

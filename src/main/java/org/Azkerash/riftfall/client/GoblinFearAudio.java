package org.Azkerash.riftfall.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GoblinSounds;
import org.Azkerash.riftfall.GoblinVoicePayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.Azkerash.riftfall.Riftfall;

/** Entity-bound voices can be cut off when fear ends, unlike ordinary server sound packets. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class GoblinFearAudio {
    private static final Map<GoblinEntity,Voice> VOICES=new IdentityHashMap<>();
    private static final float PITCH_MULTIPLIER=1.5F;
    private static final float CLIP_TICKS=3.782302F*20;
    private static long nextSequence;
    private static final class Voice {FearSound sound;int nextClipTick,startedAt;long sequence;}
    private static void finished(GoblinEntity mob,Voice voice) {
        if(voice.sound!=null && Minecraft.getInstance().getConnection()!=null)
            PacketDistributor.sendToServer(new GoblinVoicePayload(mob.getId(),voice.sequence,0));
        voice.sound=null;
    }
    private static boolean afraid(GoblinEntity mob) {
        return mob.isAlive() && !mob.isRemoved() && mob.isPanicking() && !mob.isSilent();
    }
    @SubscribeEvent public static void entityTick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof GoblinEntity mob && mob.level().isClientSide && afraid(mob))
            VOICES.computeIfAbsent(mob,key->new Voice());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.isPaused() && mc.level!=null)return;
        var sounds=mc.getSoundManager();
        var iterator=VOICES.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var mob=entry.getKey();var voice=entry.getValue();
            if(mob.level()!=mc.level || !afraid(mob)) {
                if(voice.sound!=null)sounds.stop(voice.sound);
                finished(mob,voice);
                iterator.remove();continue;
            }
            if(voice.sound!=null) {
                // Give asynchronous decoding time to install its channel before testing completion.
                if(mob.tickCount<voice.startedAt+10 || sounds.isActive(voice.sound))continue;
                finished(mob,voice);
            }
            // Finish the current clip underground, but never start another one there.
            if(mob.isWaitingUnderground() || mob.tickCount<voice.nextClipTick)continue;
            float minimum=1F/PITCH_MULTIPLIER;
            float pitch=minimum+mob.getRandom().nextFloat()*(PITCH_MULTIPLIER-minimum);
            voice.sound=new FearSound(mob,pitch);
            voice.startedAt=mob.tickCount;voice.sequence=++nextSequence;
            sounds.play(voice.sound);
            PacketDistributor.sendToServer(new GoblinVoicePayload(mob.getId(),voice.sequence,(int)Math.ceil(CLIP_TICKS/pitch)+20));
            // Lower pitch lengthens the recording; allow it to finish before the next random voice.
            voice.nextClipTick=mob.tickCount+(int)Math.ceil(CLIP_TICKS/pitch)+8+mob.getRandom().nextInt(9);
        }
    }
    public static void stop(GoblinEntity mob) {
        var voice=VOICES.remove(mob);
        if(voice!=null) {
            if(voice.sound!=null)Minecraft.getInstance().getSoundManager().stop(voice.sound);
            finished(mob,voice);
        }
    }
    private static final class FearSound extends EntityBoundSoundInstance {
        private final GoblinEntity mob;
        FearSound(GoblinEntity mob,float pitch) {
            super(GoblinSounds.SCREAM.get(),mob.getSoundSource(),.9F,pitch,mob,mob.getRandom().nextLong());
            this.mob=mob;
        }
        @Override public boolean canPlaySound(){return super.canPlaySound() && afraid(mob);}
        @Override public void tick(){super.tick();if(!afraid(mob))stop();}
    }
    private GoblinFearAudio() {}
}

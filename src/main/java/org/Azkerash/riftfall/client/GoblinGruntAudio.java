package org.Azkerash.riftfall.client;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.Azkerash.riftfall.GoblinSounds;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class GoblinGruntAudio {
    @SubscribeEvent public static void play(PlaySoundEvent event) {
        var sound=event.getSound();
        if(sound!=null && sound.getLocation().equals(GoblinSounds.GRUNT.get().getLocation()))
            event.setSound(new Slice(sound));
    }
    /** Delegate resolution and positional properties; only replace how the PCM is streamed. */
    private static final class Slice implements SoundInstance {
        private final SoundInstance original;
        private final double start=SoundInstance.createUnseededRandom().nextDouble()*(3.782302-.5);
        Slice(SoundInstance original){this.original=original;}
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers,Sound sound,boolean looping) {
            return original.getStream(buffers,sound,false).thenApply(stream->new AudioSliceStream(stream,start,.5));
        }
        @Override public ResourceLocation getLocation(){return original.getLocation();}
        @Override public WeighedSoundEvents resolve(SoundManager manager){return original.resolve(manager);}
        @Override public Sound getSound(){return original.getSound();}
        @Override public SoundSource getSource(){return original.getSource();}
        @Override public boolean isLooping(){return false;}
        @Override public boolean isRelative(){return original.isRelative();}
        @Override public int getDelay(){return original.getDelay();}
        @Override public float getVolume(){return original.getVolume();}
        @Override public float getPitch(){return original.getPitch();}
        @Override public double getX(){return original.getX();}
        @Override public double getY(){return original.getY();}
        @Override public double getZ(){return original.getZ();}
        @Override public Attenuation getAttenuation(){return original.getAttenuation();}
        @Override public boolean canStartSilent(){return original.canStartSilent();}
        @Override public boolean canPlaySound(){return original.canPlaySound();}
    }
    private GoblinGruntAudio() {}
}

package org.Azkerash.riftfall.shield;

import net.minecraft.sounds.SoundEvent;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Reserve the original guard clang pitch for successful timed parries. */
public final class GuardClang {
    public static final float BLOCK_PITCH=(float)Math.pow(2,-5D/12);
    public static void play(ServerPlayerPatch patch,SoundEvent sound,float minimum,float maximum,boolean parry) {
        if(sound!=EpicFightSounds.CLASH.get() || parry)patch.playSound(sound,minimum,maximum);
        else patch.getOriginal().playSound(sound,1,pitch(patch.getOriginal().getRandom().nextFloat(),minimum,maximum));
    }
    public static float pitch(float random,float minimum,float maximum) {
        return BLOCK_PITCH*(1+(random*2-1)*(maximum-minimum));
    }
    private GuardClang(){}
}

package org.Azkerash.riftfall;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.Azkerash.riftfall.behavior.PanicEscapeGoal;

/** Species animation binding; the shared panic executor has no goblin or Epic Fight dependency. */
final class GoblinPanicVisuals implements PanicEscapeGoal.Visuals {
    static final TagKey<Block> BURROWABLE=TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("riftfall","burrowable"));
    private final GoblinEntity mob;
    private boolean ownsRetreat;
    private int digStrokes;
    GoblinPanicVisuals(GoblinEntity mob){this.mob=mob;}
    @Override public void update(boolean panicked,boolean escaping,double progress){
        mob.panicVisuals(panicked,escaping,progress);
        if(panicked&&escaping){
            ownsRetreat=true;mob.getPersistentData().putBoolean("RiftfallRetreating",true);
            mob.retreatThreat=mob.getTarget();mob.retreatCoverUntil=mob.level().getGameTime()+40;
        }else if(ownsRetreat){mob.getPersistentData().putBoolean("RiftfallRetreating",false);ownsRetreat=false;}
    }
    @Override public void beginBurrow(int duration){
        digStrokes=0;mob.burrowDuration(duration);
        mob.getPersistentData().putInt("RiftfallBurrowDuration",duration);
        GoblinAnimations.play(mob,GoblinAnimations.BURROW);
    }
    @Override public void abortBurrow(){GoblinAnimations.play(mob,GoblinAnimations.RECOVER);}
    @Override public void dig(BlockState ground){
        if(mob.level() instanceof net.minecraft.server.level.ServerLevel server){
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground),mob.getX(),mob.getY()+.08,mob.getZ(),8,.4,.08,.4,.05);
            if(++digStrokes%2==0)mob.playSound(ground.getSoundType().getHitSound(),.65F,1.2F);
        }
    }
}

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
    static boolean canDig(GoblinEntity mob,BlockState ground,net.minecraft.core.BlockPos position){
        return org.Azkerash.riftfall.behavior.BurrowTerrain.canDig(ground.is(BURROWABLE),ground.getDestroySpeed(mob.level(),position));
    }
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
        mob.resetEscapeSoundHold();
        digStrokes=0;mob.burrowDuration(duration);
        mob.getPersistentData().putInt("RiftfallBurrowDuration",duration);
        GoblinAnimations.play(mob,GoblinAnimations.BURROW);
    }
    @Override public boolean prepareEscape(){
        return !mob.loot().hasLoot() || org.Azkerash.riftfall.loot.EscapedGoblinStash.get(mob.getServer()).store(mob);
    }
    @Override public boolean readyToEscape(){return mob.escapeSoundsFinished();}
    @Override public void abortBurrow(){mob.resetEscapeSoundHold();GoblinAnimations.play(mob,GoblinAnimations.RECOVER);}
    @Override public void dig(BlockState ground){
        if(mob.level() instanceof net.minecraft.server.level.ServerLevel server){
            double yaw=Math.toRadians(mob.yBodyRot);double backX=Math.sin(yaw),backZ=-Math.cos(yaw);
            double sideX=Math.cos(yaw),sideZ=Math.sin(yaw);
            var debris=new BlockParticleOption(ParticleTypes.BLOCK,ground);
            // A dense local cloud plus directed clods: use the actual substrate, never generic smoke.
            double x=mob.getX()+backX*.5,z=mob.getZ()+backZ*.5;
            server.sendParticles(debris,x,mob.getY()+.16,z,18,.35,.16,.35,.12);
            for(int i=0;i<6;i++){
                double lateral=(mob.getRandom().nextDouble()-.5)*.65;
                server.sendParticles(debris,x+sideX*lateral,mob.getY()+.12,z+sideZ*lateral,0,
                    backX*.9+sideX*lateral,.7+mob.getRandom().nextDouble()*.5,backZ*.9+sideZ*lateral,.22);
            }
            if(++digStrokes%3==0)mob.playSound(ground.getSoundType().getHitSound(),.8F,1.25F);
        }
    }
}

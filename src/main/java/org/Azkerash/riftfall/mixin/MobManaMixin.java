package org.Azkerash.riftfall.mixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import org.Azkerash.riftfall.mana.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class MobManaMixin implements ManaUser {
    @Unique private ManaState riftfall$mana;
    @Override public ManaState manaState(){if(riftfall$mana==null)riftfall$mana=new ManaState();return riftfall$mana;}
    @Override public boolean hasManaState(){return riftfall$mana!=null;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void riftfall$saveMana(CompoundTag tag,CallbackInfo ci){MobMana.save((Mob)(Object)this,tag);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void riftfall$loadMana(CompoundTag tag,CallbackInfo ci){MobMana.load((Mob)(Object)this,tag);}
}

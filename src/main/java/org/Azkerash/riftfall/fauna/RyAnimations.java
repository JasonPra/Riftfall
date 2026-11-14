package org.Azkerash.riftfall.fauna;

import yesman.epicfight.api.animation.AnimationManager.*;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Armatures;
import net.minecraft.resources.ResourceLocation;

/** Ry has its own quadruped skeleton; no humanoid casting poses or ravager substitutions. */
public final class RyAnimations {
    public static final yesman.epicfight.api.asset.AssetAccessor<Armature> RIG = Armatures.getOrCreate(
        ResourceLocation.fromNamespaceAndPath("riftfall", "entity/ry"), Armature::new);
    public static AnimationAccessor<StaticAnimation> IDLE, WALK, RUN, SIT, DEATH;
    public static AnimationAccessor<ActionAnimation> BITE, CHARGE, RELEASE;
    public static AnimationAccessor<HitAnimation> HURT;
    public static void register(AnimationBuilder b) {
            IDLE=b.nextAccessor("ry/idle",a->new StaticAnimation(.15F,true,a,RIG));
            WALK=b.nextAccessor("ry/walk",a->new StaticAnimation(.15F,true,a,RIG));
            RUN=b.nextAccessor("ry/run",a->new StaticAnimation(.1F,true,a,RIG));
            SIT=b.nextAccessor("ry/sit",a->new StaticAnimation(.15F,true,a,RIG));
            DEATH=b.nextAccessor("ry/death",a->new StaticAnimation(.1F,false,a,RIG));
            BITE=b.nextAccessor("ry/bite",a->new ActionAnimation(.08F,a,RIG));
            CHARGE=b.nextAccessor("ry/cast_charge",a->new ActionAnimation(.12F,a,RIG));
            RELEASE=b.nextAccessor("ry/cast_release",a->new ActionAnimation(.05F,a,RIG));
            HURT=b.nextAccessor("ry/hurt",a->new HitAnimation(.05F,a,RIG));
    }
    private RyAnimations() {}
}

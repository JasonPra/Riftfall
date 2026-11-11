package org.Azkerash.riftfall;

/** Goblin policy and synchronized visuals; the grapple executor is reusable. */
final class GoblinGrapple extends GrappleGoal {
    GoblinGrapple(GoblinEntity mob) {
        super(mob,GoblinAnimations.GRAPPLE,GoblinAnimations.GRAPPLE_RELEASE,Settings.defaults(),
            actor->!(actor.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem)
                && !(actor.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem)
                && !GoblinTactics.interrupted(mob),
            new Visuals(){
                @Override public void began(net.minecraft.world.entity.Mob actor,net.minecraft.world.entity.LivingEntity target){GoblinBuckler.lowerForAttack(mob,20);}
                @Override public void state(net.minecraft.world.entity.Mob actor,net.minecraft.world.entity.LivingEntity target,int progress){mob.grappleState(target.getId(),progress);}
                @Override public void released(net.minecraft.world.entity.Mob actor,net.minecraft.world.entity.LivingEntity target,boolean escaped){mob.grappleState(-1,0);}
            });
    }
}

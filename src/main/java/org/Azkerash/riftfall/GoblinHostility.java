package org.Azkerash.riftfall;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Overworld and dungeon undead recognize goblins as prey; goblins can retaliate and hunt them too. */
final class GoblinHostility {
    static boolean undeadHunter(LivingEntity entity) { return entity instanceof Zombie || entity instanceof AbstractSkeleton; }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !undeadHunter(mob)) return;
        if (mob.targetSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof HuntGoblins))
            mob.targetSelector.addGoal(2,new HuntGoblins(mob));
    }
    private static final class HuntGoblins extends NearestAttackableTargetGoal<GoblinEntity> {
        HuntGoblins(Mob mob) { super(mob,GoblinEntity.class,10,true,false,g->!mob.isAlliedTo(g)); }
    }
    private GoblinHostility() {}
}

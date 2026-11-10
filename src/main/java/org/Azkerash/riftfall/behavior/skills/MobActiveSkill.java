package org.Azkerash.riftfall.behavior.skills;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** NPC execution contract for an Epic Fight skill; no player-only SkillContainer is fabricated. */
public interface MobActiveSkill {
    boolean canUse(Mob owner,LivingEntity target);
    Execution start(Mob owner,LivingEntity target);
    default int cooldownTicks(){return 200;}
    interface Execution {
        boolean running();
        default boolean committed(){return true;}
        void tick();
        void stop();
    }
}

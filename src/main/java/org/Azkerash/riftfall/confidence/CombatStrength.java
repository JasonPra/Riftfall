package org.Azkerash.riftfall.confidence;

import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Relative observable attributes, not a prediction of exact damage, enchantments, DPS or armor penetration. */
public final class CombatStrength {
    public record Stats(double damage,double armor,double toughness,double health) {
        public Stats {
            if (!Double.isFinite(damage) || !Double.isFinite(armor) || !Double.isFinite(toughness) || !Double.isFinite(health))
                throw new IllegalArgumentException("Non-finite combat stats");
        }
    }
    public static Stats stats(LivingEntity entity) {
        var damage=entity.getAttribute(Attributes.ATTACK_DAMAGE);var toughness=entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        return new Stats(damage==null?1:damage.getValue(),entity.getArmorValue(),toughness==null?0:toughness.getValue(),entity.getHealth());
    }
    public static Map<String,Double> factors(Stats self,Stats target) {
        return Map.of("combat.damage_advantage",Math.clamp((self.damage-target.damage)/4,-3,3),
            "combat.armor_advantage",Math.clamp((self.armor-target.armor)/5,-4,4),
            "combat.toughness_advantage",Math.clamp((self.toughness-target.toughness)/4,-3,3),
            "combat.health_advantage",Math.clamp((self.health-target.health)/10,-3,3));
    }
    private CombatStrength() {}
}

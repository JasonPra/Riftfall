package org.Azkerash.riftfall.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import org.Azkerash.riftfall.GoblinEntities;
import org.Azkerash.riftfall.GoblinEntity;

/** Substitutes bodies before encounter scaling, identity and reward bookkeeping are applied. */
public final class EncounterGoblins {
    public static EntityType<?> replacement(EntityType<?> type) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        return switch (id) {
            case "player_system:goblin_scavenger", "player_system:hobgoblin_guard" ->
                    GoblinEntities.GOBLIN.get();
            default -> type;
        };
    }

    public static Entity create(EntityType<?> type, Level level) {
        return replacement(type).create(level);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof GoblinEntity goblin) {
            var data = goblin.getPersistentData();
            if (event.getLevel()
                            .dimension()
                            .location()
                            .toString()
                            .equals("player_system:dungeon_tower")
                    || goblin.getTags().contains("player_system.dungeon_mob")
                    || data.contains("player_system_instance")
                    || data.contains("quintessence_rift")) {
                goblin.setInRift(true);
            }
        }
    }

    private EncounterGoblins() {}
}

package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;

/** Deferred content and attribute registrations, in dependency order. */
final class RiftfallContent {
    static void register(IEventBus bus) {
        bus.addListener(org.Azkerash.riftfall.equipment.MobEquipment::attributes);
        GoblinEntities.TYPES.register(bus);
        org.Azkerash.riftfall.spells.RiftfallSpells.register(bus);
        GoblinItems.register(bus);
        RiftfallSkills.SKILLS.register(bus);
        GoblinSounds.register(bus);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET
                .registerEvent(GoblinSwordCombat::presets);
        GoblinCombatRegistries.MOVESETS.register(bus);
        GoblinCombatRegistries.CONDITIONALS.register(bus);
        bus.addListener(GoblinEntities::attributes);
    }

    private RiftfallContent() {}
}

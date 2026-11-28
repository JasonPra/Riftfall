package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Epic Fight bindings and shared/goblin combat event subscriptions. */
final class RiftfallCombat {
    static void register(IEventBus bus) {
        bus.addListener(GoblinAnimations::register);
        bus.addListener(
                (net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                        event.enqueueWork(GoblinInterceptDash::register));
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(
                GoblinEntities::patches);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(
                org.Azkerash.riftfall.compat.PlayerSystemCombat::patches);
        NeoForge.EVENT_BUS.register(MobSpellcasting.class);
        NeoForge.EVENT_BUS.register(GoblinTactics.class);
        NeoForge.EVENT_BUS.register(GoblinWeaponAttacks.class);
        NeoForge.EVENT_BUS.register(BackstabSequence.class);
        NeoForge.EVENT_BUS.register(BackstabActorLock.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.behavior.skills.MobSkillCommands.class);
        GoblinFlurryStun.register();
        NeoForge.EVENT_BUS.register(GoblinHostility.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.compat.NativeOrcCast.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.compat.EncounterGoblins.class);
        NeoForge.EVENT_BUS.register(GrappleGoal.class);
    }

    private RiftfallCombat() {}
}

package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Riftfall.MODID)
public class Riftfall {
    public static final String MODID = "riftfall";

    public Riftfall(IEventBus bus,net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
            org.Azkerash.riftfall.confidence.ConfidenceConfig.SPEC);
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event)->org.Azkerash.riftfall.confidence.ConfidenceConfig.loaded(event));
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading event)->org.Azkerash.riftfall.confidence.ConfidenceConfig.loaded(event));
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
            org.Azkerash.riftfall.stamina.StaminaConfig.SPEC,"riftfall-stamina-server.toml");
        if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)
            container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,org.Azkerash.riftfall.client.CameraConfig.SPEC,"riftfall-camera-client.toml");
        if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)
            container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                org.Azkerash.riftfall.client.titles.ArrivalTitleConfig.SPEC,"riftfall-titles-client.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,org.Azkerash.riftfall.mana.ManaConfig.SPEC,"riftfall-mana-server.toml");
        bus.addListener(org.Azkerash.riftfall.equipment.MobEquipment::attributes);
        BehaviorTestEntities.register(bus);
        GoblinEntities.TYPES.register(bus);
        GoblinItems.register(bus);
        org.Azkerash.riftfall.worldmana.WorldMana.register(bus);
        org.Azkerash.riftfall.worldmana.geysers.GeyserCaves.register(bus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,org.Azkerash.riftfall.crystals.CrystalRules.SPEC,"riftfall-crystals-server.toml");
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading e)->org.Azkerash.riftfall.crystals.CrystalRules.loaded(e));
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading e)->org.Azkerash.riftfall.crystals.CrystalRules.loaded(e));
        org.Azkerash.riftfall.crystals.ManaCrystals.register(bus);
        org.Azkerash.riftfall.crystals.CrystalParticles.TYPES.register(bus);
        RiftfallSkills.SKILLS.register(bus);
        GoblinSounds.register(bus);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(GoblinSwordCombat::presets);
        GoblinCombatRegistries.MOVESETS.register(bus);
        GoblinCombatRegistries.CONDITIONALS.register(bus);
        bus.addListener(GoblinEntities::attributes);
        bus.addListener(GrappleEscapePayload::register);
        bus.addListener(GrappleStatePayload::register);
        bus.addListener(BackstabCorpsePayload::register);
        bus.addListener(BackstabActorPayload::register);
        bus.addListener(BackstabViewPayload::register);
        bus.addListener(GoblinVoicePayload::register);
        bus.addListener(GoblinVocalCuePayload::register);
        bus.addListener(org.Azkerash.riftfall.behavior.BehaviorOverlayPayload::register);
        bus.addListener(GoblinAnimations::register);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(GoblinEntities::patches);
        NeoForge.EVENT_BUS.register(MobSpellcasting.class);
        NeoForge.EVENT_BUS.register(GoblinTactics.class);
        NeoForge.EVENT_BUS.register(GoblinWeaponAttacks.class);
        NeoForge.EVENT_BUS.register(BackstabSequence.class);
        NeoForge.EVENT_BUS.register(BackstabActorLock.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.class);
        NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.behavior.skills.MobSkillCommands.class);
        GoblinFlurryStun.register();
        NeoForge.EVENT_BUS.register(GoblinHostility.class);
        NeoForge.EVENT_BUS.register(GrappleGoal.class);
    }
}

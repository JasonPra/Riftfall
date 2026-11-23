package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;

/** Configuration ownership and explicit client/server separation. */
final class RiftfallConfiguration {
    static void register(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                org.Azkerash.riftfall.confidence.ConfidenceConfig.SPEC);
        bus.addListener(
                (net.neoforged.fml.event.config.ModConfigEvent.Loading event) ->
                        org.Azkerash.riftfall.confidence.ConfidenceConfig.loaded(event));
        bus.addListener(
                (net.neoforged.fml.event.config.ModConfigEvent.Reloading event) ->
                        org.Azkerash.riftfall.confidence.ConfidenceConfig.loaded(event));
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                org.Azkerash.riftfall.stamina.StaminaConfig.SPEC,
                "riftfall-stamina-server.toml");
        if (net.neoforged.fml.loading.FMLEnvironment.dist
                == net.neoforged.api.distmarker.Dist.CLIENT) {
            container.registerConfig(
                    net.neoforged.fml.config.ModConfig.Type.CLIENT,
                    org.Azkerash.riftfall.client.CameraConfig.SPEC,
                    "riftfall-camera-client.toml");
        }
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                org.Azkerash.riftfall.mana.ManaConfig.SPEC,
                "riftfall-mana-server.toml");
    }

    private RiftfallConfiguration() {}
}

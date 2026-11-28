package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;

/** Single registration point for the mod network payloads. */
final class RiftfallNetworking {
    static void register(IEventBus bus) {
        bus.addListener(GrappleEscapePayload::register);
        bus.addListener(GrappleStatePayload::register);
        bus.addListener(BackstabCorpsePayload::register);
        bus.addListener(BackstabActorPayload::register);
        bus.addListener(BackstabViewPayload::register);
        bus.addListener(GoblinVoicePayload::register);
        bus.addListener(GoblinVocalCuePayload::register);
        bus.addListener(org.Azkerash.riftfall.compat.NativeOrcCastPayload::register);
        bus.addListener(org.Azkerash.riftfall.behavior.BehaviorOverlayPayload::register);
    }

    private RiftfallNetworking() {}
}

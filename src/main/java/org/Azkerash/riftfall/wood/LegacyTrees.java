package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.event.TypeRegistryEvent;
import com.dtteam.dynamictrees.registry.NeoForgeRegistryHandler;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

/** Tree pack registration; uses DT's normal branch, seed, sapling and root registries. */
public final class LegacyTrees {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("riftfall","vermillion_legacy");

    public static void register(IEventBus bus) {
        NeoForgeRegistryHandler.setup("riftfall",bus);
        LegacyBarkWound.register(bus);
        bus.addListener(LegacyTrees::familyTypes);
        bus.addListener(LegacyTrees::speciesTypes);
        bus.addListener(LegacyTrees::growthKits);
    }

    private static void familyTypes(TypeRegistryEvent<Family> event) {
        if(event.isEntryOfType(Family.class))event.registerType(ID,TypedRegistry.newType(LegacyTreeFamily::new));
    }

    private static void speciesTypes(TypeRegistryEvent<Species> event) {
        if(event.isEntryOfType(Species.class))event.registerType(ID,Species.createDefaultType(LegacyTreeSpecies::new));
    }
    private static void growthKits(com.dtteam.dynamictrees.event.RegistryEvent<com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit> event) {
        if(event.isEntryOfType(com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit.class))
            event.getRegistry().register(new LegacyGrowthLogic());
    }
    private LegacyTrees() {}
}

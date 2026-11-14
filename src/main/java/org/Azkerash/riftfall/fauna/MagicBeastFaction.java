package org.Azkerash.riftfall.fauna;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.Factions;

/** An independent NPC faction, not Epic Fight's neutral or monster factions. */
public enum MagicBeastFaction implements Faction {
    MAGIC_BEASTS;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MEMBERS=net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("riftfall","magic_beasts"));
    private final int id = Faction.ENUM_MANAGER.assign(this);
    @Override public int universalOrdinal() { return id; }
    @Override public ResourceLocation healthBarTexture() { return Factions.VILLAGER.healthBarTexture(); }
    @Override public int healthBarIndex() { return Factions.VILLAGER.healthBarIndex(); }
    @Override public int damageColor() { return 0x75DCCB; }
}

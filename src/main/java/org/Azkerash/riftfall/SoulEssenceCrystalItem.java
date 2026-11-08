package org.Azkerash.riftfall;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Harvested crystals can be planted as a single-crystal seed on any sturdy face. */
public final class SoulEssenceCrystalItem extends BlockItem {
    public SoulEssenceCrystalItem() { super(RiftBlocks.CRYSTAL_CLUSTER.get(), new Item.Properties()); }
    @Override public String getDescriptionId() { return "item.riftfall.soul_essence_crystal"; }
}

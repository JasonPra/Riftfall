package org.Azkerash.riftfall;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

/** Item-bound stances, shared by goblin pursuit and Epic Fight's player combos. */
final class GoblinSwordCombat {
    static GoblinTactics.SwordAttack select(ItemStack weapon,ItemStack offhand,int combo) {
        return select(weapon.is(GoblinItems.SHORTSWORD.get()),offhand.isEmpty(),
            offhand.getItem() instanceof net.minecraft.world.item.ShieldItem,combo);
    }
    static GoblinTactics.SwordAttack select(boolean goblinSword,boolean emptyOffhand,boolean shield,int combo) {
        if (goblinSword) {
            if (emptyOffhand) return GoblinTactics.SwordAttack.TWOHAND_FLURRY;
            if (shield)
                return switch (Math.floorMod(combo,3)) {
                    case 0 -> GoblinTactics.SwordAttack.SWIPE;
                    case 1 -> GoblinTactics.SwordAttack.BACKSWING;
                    default -> GoblinTactics.SwordAttack.SHIELD_OVERHEAD;
                };
        }
        // Other equipped weapons retain the basic sword combo and their own colliders.
        return switch (Math.floorMod(combo,3)) {
            case 0 -> GoblinTactics.SwordAttack.SWIPE;
            case 1 -> GoblinTactics.SwordAttack.BACKSWING;
            default -> GoblinTactics.SwordAttack.FLURRY;
        };
    }
    static void presets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"goblin_shortsword"),item ->
            WeaponCapability.builder().category(WeaponCategories.SWORD)
                .hitSound(yesman.epicfight.registry.entries.EpicFightSounds.BLADE_HIT)
                .swingSound(yesman.epicfight.registry.entries.EpicFightSounds.WHOOSH_SHARP)
                .collider(new yesman.epicfight.api.collider.MultiOBBCollider(5,.09,.08,.36,0,-.04,-.32))
                .styleProvider(patch -> patch.getOriginal().getOffhandItem().isEmpty() ? Styles.TWO_HAND : Styles.ONE_HAND)
                .newStyleCombo(Styles.ONE_HAND,GoblinAnimations.SLASH,GoblinAnimations.BACKSWING,
                    GoblinAnimations.SHIELD_OVERHEAD,GoblinAnimations.SLASH,GoblinAnimations.PLUNGE)
                .newStyleCombo(Styles.TWO_HAND,GoblinAnimations.TWOHAND_FLURRY,
                    GoblinAnimations.TWOHAND_FLURRY,GoblinAnimations.PLUNGE)
                .weaponCombinationPredicator(patch -> patch.getOriginal().getOffhandItem().getItem()
                    instanceof net.minecraft.world.item.ShieldItem)
                .canBePlacedOffhand(false));
    }
    private GoblinSwordCombat() {}
}

package org.Azkerash.riftfall;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

/** Item-bound stances, shared by goblin pursuit and Epic Fight's player combos. */
final class GoblinSwordCombat {
    enum Stance implements yesman.epicfight.world.capabilities.item.Style {
        GOBLIN_FRENZY;
        private final int id=yesman.epicfight.world.capabilities.item.Style.ENUM_MANAGER.assign(this);
        @Override public int universalOrdinal() { return id; }
        @Override public boolean canUseOffhand() { return true; }
    }
    static GoblinTactics.SwordAttack select(ItemStack weapon,ItemStack offhand,int combo) {
        return select(weapon.is(GoblinItems.SHORTSWORD.get()),offhand.isEmpty(),
            offhand.getItem() instanceof net.minecraft.world.item.ShieldItem,combo);
    }
    static GoblinTactics.SwordAttack select(boolean goblinSword,boolean emptyOffhand,boolean shield,int combo) {
        if (goblinSword) {
            return switch (Math.floorMod(combo,comboLength(true,shield))) {
                case 0 -> shield ? GoblinTactics.SwordAttack.SWIPE : GoblinTactics.SwordAttack.SWORD_SWIPE;
                case 1 -> shield ? GoblinTactics.SwordAttack.BACKSWING : GoblinTactics.SwordAttack.SWORD_REVERSE;
                case 2 -> GoblinTactics.SwordAttack.FRENZY;
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
    static int comboLength(boolean goblinSword,boolean shield) { return goblinSword && shield ? 4 : 3; }
    static void presets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"goblin_shortsword"),item ->
            WeaponCapability.builder().category(WeaponCategories.SWORD)
                .hitSound(yesman.epicfight.registry.entries.EpicFightSounds.BLADE_HIT)
                .swingSound(yesman.epicfight.registry.entries.EpicFightSounds.WHOOSH_SHARP)
                .collider(new yesman.epicfight.api.collider.MultiOBBCollider(5,.09,.08,.36,0,-.04,-.32))
                .addConditionals(GoblinCombatRegistries.EMPTY_HAND,GoblinCombatRegistries.SHIELD_HAND,GoblinCombatRegistries.OTHER_HAND)
                .addMoveset(Styles.ONE_HAND,GoblinCombatRegistries.ONE_HAND_SET)
                .addMoveset(Stance.GOBLIN_FRENZY,GoblinCombatRegistries.FRENZY_SET)
                .canBePlacedOffhand(false));
    }
    private GoblinSwordCombat() {}
}

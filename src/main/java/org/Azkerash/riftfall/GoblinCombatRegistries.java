package org.Azkerash.riftfall;

import net.minecraft.world.item.ShieldItem;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.api.ex_cap.provider.ProviderConditional;
import yesman.epicfight.registry.deferred.MovesetRegister;
import yesman.epicfight.registry.deferred.ProviderConditionalRegister;
import yesman.epicfight.registry.deferred.holders.DeferredMoveset;
import yesman.epicfight.registry.deferred.holders.DeferredConditional;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;

/** Modern Epic Fight registrations, kept separate from pure goblin combo selection. */
final class GoblinCombatRegistries {
    static final MovesetRegister MOVESETS=MovesetRegister.create(Riftfall.MODID);
    static final ProviderConditionalRegister CONDITIONALS=ProviderConditionalRegister.create(Riftfall.MODID);
    // Keep the legacy ordering: ground combo first, then dash and aerial slots.
    static final DeferredMoveset ONE_HAND_SET=MOVESETS.registerMoveset("goblin_shortsword/one_hand",()->
        new Moveset.Builder().addComboAttacks(GoblinAnimations.SLASH,GoblinAnimations.BACKSWING,GoblinAnimations.FRENZY,
            GoblinAnimations.SHIELD_OVERHEAD,yesman.epicfight.gameasset.Animations.SWORD_DASH,GoblinAnimations.PLUNGE));
    static final DeferredMoveset FRENZY_SET=MOVESETS.registerMoveset("goblin_shortsword/frenzy",()->
        new Moveset.Builder().addComboAttacks(GoblinAnimations.SWORD_SWIPE,GoblinAnimations.SWORD_REVERSE,GoblinAnimations.FRENZY,
            yesman.epicfight.gameasset.Animations.SWORD_DASH,GoblinAnimations.PLUNGE));
    // Distinct IDs matter: two conditions select ONE_HAND, with different offhand visibility.
    static final DeferredConditional EMPTY_HAND=CONDITIONALS.registerConditional("goblin_shortsword/empty_offhand",()->
        ProviderConditional.createCustom(GoblinSwordCombat.Stance.GOBLIN_FRENZY,patch->patch.getOriginal().getOffhandItem().isEmpty(),false));
    static final DeferredConditional SHIELD_HAND=CONDITIONALS.registerConditional("goblin_shortsword/shield_offhand",()->
        ProviderConditional.createCustom(Styles.ONE_HAND,patch->patch.getOriginal().getOffhandItem().getItem() instanceof ShieldItem,true));
    static final DeferredConditional OTHER_HAND=CONDITIONALS.registerConditional("goblin_shortsword/other_offhand",()->
        ProviderConditional.createDefault(Styles.ONE_HAND,false));
    private GoblinCombatRegistries() {}
}

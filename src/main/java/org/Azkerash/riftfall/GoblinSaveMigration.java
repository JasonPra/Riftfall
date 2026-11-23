package org.Azkerash.riftfall;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compatibility conversions for old goblin saves; current runtime state belongs to controllers. */
final class GoblinSaveMigration {
    static void apply(GoblinEntity mob, CompoundTag tag) {
        // Correct the previous default in existing saves while preserving deliberate
        // custom scale values and attribute modifiers.
        if (mob.isShaman() && mob.getMainHandItem().is(Items.BLAZE_ROD)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GoblinItems.BONE_STAFF.get()));
        }
        if (!tag.contains("RiftfallInventory")
                && mob.getPersistentData().contains("RiftfallOwner")) {
            if (mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)) {
                mob.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            if (mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                    || mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)) {
                mob.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
            if (!mob.isRanged()
                    && (mob.getMainHandItem().is(Items.IRON_SWORD)
                            || mob.getMainHandItem().is(Items.DIAMOND_SWORD)
                            || mob.getMainHandItem().is(Items.NETHERITE_SWORD))) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            }
        }
        if (!tag.contains("RiftfallInventory")
                && !mob.isRanged()
                && mob.getMainHandItem().is(Items.STONE_SWORD)) {
            mob.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    mob.getMainHandItem().transmuteCopy(GoblinItems.SHORTSWORD.get()));
        }
        // Update the old default without overwriting deliberately customized speeds.
        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && Math.abs(speed.getBaseValue() - .28) < .00001) {
            speed.setBaseValue(.23);
        }
        for (String key :
                java.util.List.of(
                        "RiftfallRetreating",
                        "RiftfallCoveringRetreat",
                        "RiftfallPanicked",
                        "RiftfallPanicking",
                        "RiftfallBurrowing",
                        "RiftfallBurrowProgress",
                        "RiftfallPanicEscapeAllowed",
                        "RiftfallPanicSafety",
                        "RiftfallBurrowDuration",
                        "RiftfallBurrowWaitingForSound",
                        "RiftfallIntercepting",
                        "RiftfallInterceptDashing",
                        "RiftfallProtectedAlly",
                        "RiftfallInterceptX",
                        "RiftfallInterceptZ",
                        "RiftfallFormation",
                        "RiftfallHighGround",
                        "RiftfallFlanking",
                        "RiftfallScreening",
                        "RiftfallCasting",
                        "RiftfallStaffAttack",
                        "RiftfallStaffApproaching",
                        "RiftfallSeekingHealing",
                        "RiftfallBehaviorNotes",
                        "RiftfallBehavior")) {
            mob.getPersistentData().remove(key);
        }
        var scale = mob.getAttribute(Attributes.SCALE);
        if (scale != null
                && (Math.abs(scale.getBaseValue() - .56) < .00001
                        || Math.abs(scale.getBaseValue() - .72) < .00001)) {
            scale.setBaseValue(.75);
        }
    }

    private GoblinSaveMigration() {}
}

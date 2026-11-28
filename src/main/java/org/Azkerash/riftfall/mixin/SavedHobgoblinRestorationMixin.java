package org.Azkerash.riftfall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.kubas.playersystem.data.PsAttachments;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GoblinItems;
import org.Azkerash.riftfall.compat.EncounterGoblinLoadout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** Repairs only saved goblin replacements explicitly identified as Player System hobgoblins. */
@Mixin(EntityType.class)
public abstract class SavedHobgoblinRestorationMixin {
    @ModifyReturnValue(
            method =
                    "create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"))
    private static Optional<Entity> riftfall$restoreHobgoblin(
            Optional<Entity> original, CompoundTag saved, Level level) {
        var hobgoblin = ResourceLocation.parse("player_system:hobgoblin_guard");
        if (original.orElse(null) instanceof GoblinEntity goblin
                && goblin.getPersistentData().getBoolean(EncounterGoblinLoadout.REPLACEMENT)
                && goblin.hasData(PsAttachments.MOB_RANK)
                && goblin.getData(PsAttachments.MOB_RANK)
                        .species()
                        .filter(hobgoblin::equals)
                        .isPresent()) {
            var restored = BuiltInRegistries.ENTITY_TYPE.get(hobgoblin).create(level);
            if (restored != null) {
                restored.load(saved);
                restored.getPersistentData().remove(EncounterGoblinLoadout.REPLACEMENT);
                restored.getPersistentData().remove("RiftfallEncounterGroup");
                restored.getPersistentData().remove("RiftfallEncounterBuckler");
                if (restored instanceof net.minecraft.world.entity.Mob mob) {
                    for (var slot :
                            java.util.List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
                        var stack = mob.getItemBySlot(slot);
                        if (stack.is(GoblinItems.SHORTSWORD.get())
                                || stack.is(GoblinItems.BONE_STAFF.get())
                                || stack.is(GoblinItems.BUCKLER.get())
                                || stack.is(Items.BOW)) {
                            mob.setItemSlot(slot, ItemStack.EMPTY);
                        }
                    }
                }
                return Optional.of(restored);
            }
        }
        return original;
    }
}

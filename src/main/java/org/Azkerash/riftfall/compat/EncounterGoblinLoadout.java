package org.Azkerash.riftfall.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.Azkerash.riftfall.*;

/** Bounded local encounter composition; existing spawn slots are reused, never supplemented. */
public final class EncounterGoblinLoadout {
    public static final String REPLACEMENT = "RiftfallEncounterReplacement";
    private static final String GROUP = "RiftfallEncounterGroup";
    private static final String SHIELD = "RiftfallEncounterBuckler";

    private record Recent(java.lang.ref.WeakReference<GoblinEntity> entity, long tick) {}

    // Newly added mobs can precede the chunk's visible-entity index during initial floor creation.
    // Server-thread owned; weak references and a one-second horizon avoid retaining unloaded mobs.
    private static final java.util.Map<Level, java.util.List<Recent>> RECENT =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public enum Role {
        SHAMAN,
        WARRIOR,
        ARCHER
    }

    public static Role role(int shamans, int warriors, int archers, int roll) {
        if (shamans == 0) {
            return Role.SHAMAN;
        }
        if (shamans < 2 && warriors + archers >= 12) {
            return Role.SHAMAN;
        }
        if (warriors == 0) {
            return Role.WARRIOR;
        }
        if (archers == 0 || roll < 2) {
            return Role.ARCHER;
        }
        return Role.WARRIOR;
    }

    public static Entity create(EntityType<?> type, Level level, double x, double y, double z) {
        if (EncounterGoblins.replacement(type) != GoblinEntities.GOBLIN.get()) {
            return type.create(level);
        }
        String group =
                level instanceof net.minecraft.server.level.ServerLevel serverLevel
                        ? dev.kubas.playersystem.dungeon.DungeonService.instanceAt(
                                        serverLevel.getServer(), level, x, z)
                                .map(instance -> instance.id().toString())
                                .orElse("")
                        : "";
        var nearby =
                new java.util.HashSet<>(
                        level.getEntitiesOfClass(
                                GoblinEntity.class,
                                new AABB(x - 32, y - 4, z - 32, x + 32, y + 4, z + 32),
                                g ->
                                        near(g, x, y, z)
                                                && g.isAlive()
                                                && !g.isNoAi()
                                                && (g.isInRift()
                                                        || g.getPersistentData()
                                                                .getBoolean(REPLACEMENT))));
        if (!group.isEmpty()
                && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (var entity : serverLevel.getAllEntities()) {
                if (entity instanceof GoblinEntity goblin
                        && goblin.isAlive()
                        && group.equals(goblin.getPersistentData().getString(GROUP))) {
                    nearby.add(goblin);
                }
            }
            nearby.removeIf(goblin -> !group.equals(goblin.getPersistentData().getString(GROUP)));
        }
        var recent = RECENT.computeIfAbsent(level, ignored -> new java.util.ArrayList<>());
        long now = level.getGameTime();
        recent.removeIf(
                entry -> {
                    var goblin = entry.entity().get();
                    return entry.tick() + 20 < now || goblin == null || goblin.isRemoved();
                });
        for (var entry : recent) {
            var goblin = entry.entity().get();
            if (goblin != null
                    && goblin.isAlive()
                    && !goblin.isNoAi()
                    && (group.isEmpty()
                            ? near(goblin, x, y, z)
                            : group.equals(goblin.getPersistentData().getString(GROUP)))) {
                nearby.add(goblin);
            }
        }
        int shamans = 0, archers = 0, warriors = 0, shields = 0;
        for (var goblin : nearby) {
            if (goblin.isShaman()) {
                shamans++;
            } else if (goblin.isArcher()) {
                archers++;
            } else {
                warriors++;
                if (!goblin.getOffhandItem().isEmpty()) {
                    shields++;
                }
            }
        }
        var role = role(shamans, warriors, archers, level.random.nextInt(4));
        var selected =
                switch (role) {
                    case SHAMAN -> GoblinEntities.GOBLIN_SHAMAN.get();
                    case ARCHER -> GoblinEntities.GOBLIN_ARCHER.get();
                    default -> GoblinEntities.GOBLIN.get();
                };
        var goblin = selected.create(level);
        if (goblin != null) {
            goblin.moveTo(x, y, z);
            goblin.getPersistentData().putString(GROUP, group);
            goblin.getPersistentData().putBoolean(REPLACEMENT, true);
            goblin.getPersistentData()
                    .putBoolean(
                            SHIELD,
                            role == Role.WARRIOR && (shields == 0 || level.random.nextBoolean()));
            restore(goblin);
            recent.add(new Recent(new java.lang.ref.WeakReference<>(goblin), now));
        }
        return goblin;
    }

    private static boolean near(GoblinEntity goblin, double x, double y, double z) {
        double dx = goblin.getX() - x, dz = goblin.getZ() - z;
        return Math.abs(goblin.getY() - y) <= 4 && dx * dx + dz * dz <= 32 * 32;
    }

    /**
     * SpeciesApplier clears every slot, so restore empty role slots after its scaling/equipment.
     */
    public static void restore(GoblinEntity goblin) {
        if (!goblin.getPersistentData().getBoolean(REPLACEMENT)) {
            return;
        }
        if (goblin.getMainHandItem().isEmpty()) {
            goblin.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(
                            goblin.isShaman()
                                    ? GoblinItems.BONE_STAFF.get()
                                    : goblin.isArcher()
                                            ? Items.BOW
                                            : GoblinItems.SHORTSWORD.get()));
            goblin.setDropChance(EquipmentSlot.MAINHAND, 0);
        }
        if (!goblin.isRanged()
                && goblin.getOffhandItem().isEmpty()
                && goblin.getPersistentData().getBoolean(SHIELD)) {
            goblin.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(GoblinItems.BUCKLER.get()));
            goblin.setDropChance(EquipmentSlot.OFFHAND, .085F);
        }
    }

    private EncounterGoblinLoadout() {}
}

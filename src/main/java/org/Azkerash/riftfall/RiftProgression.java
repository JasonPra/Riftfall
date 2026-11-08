package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.Holder;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import org.Azkerash.riftfall.RiftData.Rift;

final class RiftProgression {
    static int level(double essence, int cost, int max) { return 1 + (int) Math.min(max - 1, Math.floor(Math.max(0, essence) / cost)); }
    static int enemies(int base, int level) { return base + 2 * (level - 1); }
    static int radius(int enemies) { return enemies <= 6 ? 10 : Math.min(26, 12 + (enemies - 7) / 2); }

    static int fixedCost(int base,int level) { return 20+level*20; }
    static int minimumCost(int base,int level) { return fixedCost(base,level)+enemies(base,level)*10; }

    static void equip(Mob mob, int level) {
        attribute(mob, Attributes.MAX_HEALTH, mob.getMaxHealth() * (1 + .25 * (level - 1)));
        var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(damage.getBaseValue() * (1 + .15 * (level - 1)));
        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() + .003 * (level - 1));
        attribute(mob, EpicFightAttributes.IMPACT, .5 + .15 * (level - 1));
        attribute(mob, EpicFightAttributes.STUN_ARMOR, 2 + level - 1);
        mob.setHealth(mob.getMaxHealth());
        boolean humanoid = mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof AbstractIllager || mob instanceof AbstractPiglin;
        if (humanoid) { mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET)); mob.setDropChance(EquipmentSlot.HEAD,0); }
        if (mob instanceof Zombie zombie && zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE) != null)
            zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        boolean ranged = mob.getMainHandItem().is(Items.BOW) || mob.getMainHandItem().is(Items.CROSSBOW);
        if (level >= 2 && humanoid && !ranged) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(level >= 8 ? Items.NETHERITE_SWORD : level >= 5 ? Items.DIAMOND_SWORD : level >= 3 ? Items.IRON_SWORD : Items.STONE_SWORD));
            mob.setDropChance(EquipmentSlot.MAINHAND, 0);
        }
        if (level >= 3 && humanoid) {
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(level >= 6 ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
            mob.setDropChance(EquipmentSlot.CHEST, 0);
        }
    }

    private static void attribute(Mob mob, Holder<Attribute> type, double value) {
        var attribute = mob.getAttribute(type);
        if (attribute != null) attribute.setBaseValue(value);
    }

    static void rewards(ServerLevel arena, Rift r) {
        BlockPos pos = r.arena().offset(0, 1, -r.radius + 2);
        arena.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,net.minecraft.core.Direction.SOUTH));
        if (!(arena.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        int level = r.encounterLevel;
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 3 + 2 * level));
        chest.setItem(2, new ItemStack(Items.GOLD_INGOT, level));
        chest.setItem(4, new ItemStack(Items.EMERALD, level));
        chest.setItem(6, new ItemStack(Items.COOKED_BEEF, 6 + level));
        if (level >= 2) chest.setItem(8, new ItemStack(Items.DIAMOND, level - 1));
        if (level >= 2) chest.setItem(13, new ItemStack(level >= 5 ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
        if (r.boss != null) {
            chest.setItem(15,new ItemStack(RiftBlocks.SOUL_ESSENCE_CRYSTAL.get(),8));
            chest.setItem(16,new ItemStack(Items.DIAMOND,2));
        }
        chest.setChanged();
    }
    private RiftProgression() {}
}

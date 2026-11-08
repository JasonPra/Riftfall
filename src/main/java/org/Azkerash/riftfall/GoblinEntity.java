package org.Azkerash.riftfall;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Living humanoids: the shaman is a separate entity type, never an undead caster. */
public final class GoblinEntity extends Monster {
    private boolean castingGoalAdded;
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type,level);
        xpReward=isShaman()?12:5;
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(isShaman()?Items.BLAZE_ROD:Items.STONE_SWORD));
        setDropChance(EquipmentSlot.MAINHAND,0);
    }
    public boolean isShaman() { return getType()==RiftEntities.GOBLIN_SHAMAN.get(); }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,28).add(Attributes.SCALE,.72);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        if (isShaman()) {
            goalSelector.addGoal(2,new AvoidEntityGoal<>(this,Player.class,5,1.1,1.2));
            goalSelector.addGoal(3,new MoveTowardsTargetGoal(this,1.0,24));
        } else goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && isShaman() && !castingGoalAdded) {
            castingGoalAdded=true; RiftSpellcasting.restore(this);
        }
    }
}

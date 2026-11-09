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
public final class GoblinEntity extends Monster implements net.minecraft.world.entity.monster.RangedAttackMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> GRAPPLED_PLAYER =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ESCAPE_PROGRESS =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    final GoblinSquadTactics squad=new GoblinSquadTactics(this);
    GoblinEntity coveredAlly;
    net.minecraft.world.entity.LivingEntity retreatThreat;
    long retreatCoverUntil;
    private boolean castingGoalAdded;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(GRAPPLED_PLAYER,-1); builder.define(ESCAPE_PROGRESS,0);
    }
    public int grappledPlayerId() { return entityData.get(GRAPPLED_PLAYER); }
    public int escapeProgress() { return entityData.get(ESCAPE_PROGRESS); }
    void grappleState(int playerId,int progress) { entityData.set(GRAPPLED_PLAYER,playerId); entityData.set(ESCAPE_PROGRESS,progress); }
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type,level);
        xpReward=isShaman()?12:5;
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(isShaman()?GoblinItems.BONE_STAFF.get():isArcher()?Items.BOW:Items.STONE_SWORD));
        setDropChance(EquipmentSlot.MAINHAND,0);
    }
    public boolean isShaman() { return getType()==GoblinEntities.GOBLIN_SHAMAN.get(); }
    public boolean isArcher() { return getType()==GoblinEntities.GOBLIN_ARCHER.get(); }
    public boolean isRanged() { return isShaman() || isArcher(); }
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor world, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData group) {
        var result = super.finalizeSpawn(world, difficulty, reason, group);
        if (!isRanged() && getOffhandItem().isEmpty() && getRandom().nextInt(3) == 0) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(GoblinItems.BUCKLER.get()));
            setDropChance(EquipmentSlot.OFFHAND, .085F);
        }
        return result;
    }
    @Override public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        if (other instanceof GoblinEntity && getTeam()==null && other.getTeam()==null) return true;
        return super.isAlliedTo(other);
    }
    @Override public boolean canFireProjectileWeapon(net.minecraft.world.item.ProjectileWeaponItem weapon) { return isArcher() && weapon instanceof net.minecraft.world.item.BowItem; }
    @Override public void performRangedAttack(net.minecraft.world.entity.LivingEntity target,float power) {
        if (!isArcher()) return;
        var arrow=net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(this,new ItemStack(Items.ARROW),power,getMainHandItem());
        double x=target.getX()-getX(),z=target.getZ()-getZ(),y=target.getY(.33)-arrow.getY();
        arrow.shoot(x,y+Math.sqrt(x*x+z*z)*.2,z,1.6F,6);
        playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT,1,1);
        level().addFreshEntity(arrow);
        getPersistentData().putInt("RiftfallArrowsShot",getPersistentData().getInt("RiftfallArrowsShot")+1);
    }
    @Override protected void hurtCurrentlyUsedShield(float damage) {
        if (damage >= 3 && getUseItem().is(GoblinItems.BUCKLER.get()))
            getUseItem().hurtAndBreak(1 + (int) damage, this, EquipmentSlot.OFFHAND);
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(yesman.epicfight.registry.entries.EpicFightAttributes.IMPACT,.5)
            .add(yesman.epicfight.registry.entries.EpicFightAttributes.STUN_ARMOR,0).add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3)
            .add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,28).add(Attributes.SCALE,.75);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Correct the previous default in existing saves while preserving deliberate
        // custom scale values and attribute modifiers.
        if (isShaman() && getMainHandItem().is(Items.BLAZE_ROD))
            setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(GoblinItems.BONE_STAFF.get()));
        if (getPersistentData().contains("RiftfallOwner")) {
            if (getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)) setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            if (getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE) || getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE))
                setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
            if (!isRanged() && (getMainHandItem().is(Items.IRON_SWORD) || getMainHandItem().is(Items.DIAMOND_SWORD) || getMainHandItem().is(Items.NETHERITE_SWORD)))
                setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));
        }
        // Update the old default without overwriting deliberately customized speeds.
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed!=null && Math.abs(speed.getBaseValue()-.28)<.00001) speed.setBaseValue(.23);
        getPersistentData().remove("RiftfallRetreating");
        getPersistentData().remove("RiftfallCoveringRetreat");
        var scale=getAttribute(Attributes.SCALE);
        if (scale!=null && (Math.abs(scale.getBaseValue()-.56)<.00001 || Math.abs(scale.getBaseValue()-.72)<.00001))
            scale.setBaseValue(.75);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        if (isShaman()) {
            goalSelector.addGoal(1,new GoblinTactics.StaffClub(this));
            goalSelector.addGoal(2,new GoblinTactics.Heal(this));
        }
        if (isRanged()) {
            targetSelector.addGoal(0,new GoblinTactics.CoverRetreat(this));
            goalSelector.addGoal(3,new GoblinTactics.Retreat(this));
            goalSelector.addGoal(4,new GoblinSquadTactics.Position(this));
        }
        if (isArcher()) goalSelector.addGoal(5,new GoblinTactics.Bow(this));
        if (!isRanged()) {
            goalSelector.addGoal(1,new GoblinTactics.WarriorRetreat(this));
            goalSelector.addGoal(1,new GoblinGrapple(this));
            goalSelector.addGoal(2,new GoblinTactics.Plunge(this));
            goalSelector.addGoal(2,new GoblinBuckler.Guard(this));
            goalSelector.addGoal(3,new GoblinTactics.Melee(this));
        }
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new GoblinSquadTactics.Assist(this));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.LivingEntity.class,10,true,false,
            target->GoblinHostility.undeadHunter(target) && !isAlliedTo(target)));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            var target=getTarget();
            // Keep the client combat pose active while withdrawing or covering allies as well.
            setAggressive(target!=null && target.isAlive() && !isAlliedTo(target));
        }
        if (!level().isClientSide && isShaman() && !castingGoalAdded) {
            castingGoalAdded=true; MobSpellcasting.restore(this);
        }
    }
}

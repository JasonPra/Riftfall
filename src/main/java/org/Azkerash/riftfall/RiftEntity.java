package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A stationary visual for a saved rift or its arena exit; deliberately has no AI. */
public final class RiftEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BROKEN = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private int riftId = -1;
    private boolean exit;

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    void configure(int id, boolean arenaExit, BlockPos pos, boolean broken, float yaw) {
        riftId = id;
        exit = arenaExit;
        moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, yaw, 0);
        setBroken(broken);
    }

    boolean belongsTo(int id, boolean arenaExit) { return riftId == id && exit == arenaExit; }
    public boolean isBroken() { return entityData.get(BROKEN); }
    void setBroken(boolean broken) { entityData.set(BROKEN, broken); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(BROKEN, false); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel level) {
            if (tickCount % 20 == 0) {
                boolean active = RiftData.get(level.getServer()).rifts.stream().anyMatch(r ->
                    r.id == riftId && (exit ? r.built && level.dimension() == RiftEvents.ARENA
                        : level.dimension() == Level.OVERWORLD));
                if (!active) { discard(); return; }
            }
            if (random.nextInt(100) == 0) {
                level.playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, .5F, .8F + random.nextFloat() * .4F);
            }
        }
    }

    // Moving pistons, liquids, explosions, and other entities cannot move a rift.
    @Override public void move(MoverType type, Vec3 movement) {}
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
}

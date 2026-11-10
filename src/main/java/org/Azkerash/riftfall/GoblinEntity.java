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
public final class GoblinEntity extends Monster implements org.Azkerash.riftfall.stamina.StaminaUser, org.Azkerash.riftfall.behavior.skills.BehaviorSkillUser, net.minecraft.world.entity.monster.RangedAttackMob, org.Azkerash.riftfall.confidence.ConfidenceParticipant, org.Azkerash.riftfall.facial.FacialParticipant {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> GRAPPLED_PLAYER =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ESCAPE_PROGRESS =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CONFIDENCE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> FACE=
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PANIC=
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> BURROW_PROGRESS=
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> BURROW_DURATION=
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PANTING=
        net.minecraft.network.syncher.SynchedEntityData.defineId(GoblinEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private final org.Azkerash.riftfall.stamina.StaminaState stamina=new org.Azkerash.riftfall.stamina.StaminaState();
    @Override public org.Azkerash.riftfall.stamina.StaminaState staminaState(){return stamina;}
    @Override public boolean staminaConfident(){return behavior!=null && behavior.bold();}
    @Override public boolean staminaFleeing(){return isPanicFleeing() || getPersistentData().getBoolean("RiftfallRetreating") || getPersistentData().getBoolean("RiftfallSeekingHealing");}
    @Override public void syncPanting(boolean panting){entityData.set(PANTING,panting);}
    public boolean isPanting(){return level().isClientSide?entityData.get(PANTING):org.Azkerash.riftfall.stamina.MobStamina.panting(this);}
    @Override public boolean managesStaminaActions(){return true;}
    private float previousBurrow,visualBurrow;
    private long lastKillLaugh=Long.MIN_VALUE;
    private final org.Azkerash.riftfall.behavior.EscapeSoundWait escapeSounds=new org.Azkerash.riftfall.behavior.EscapeSoundWait();
    private long hurtVoiceUntil;
    public boolean isWaitingUnderground(){return isBurrowing() && burrowProgress()>=1;}
    public boolean isBurrowLockHidden(){return isBurrowing() && org.Azkerash.riftfall.behavior.BurrowExposure.breaksLock(burrowProgress());}
    public boolean isFullyBuried(){return isBurrowing() && org.Azkerash.riftfall.behavior.BurrowExposure.fullyBuried(burrowProgress());}
    @Override public boolean isPickable(){return !isFullyBuried() && super.isPickable();}
    @Override public boolean isAttackable(){return !isFullyBuried() && super.isAttackable();}
    @Override public boolean isPushable(){return !isFullyBuried() && super.isPushable();}
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return isFullyBuried() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }
    public void voiceStarted(java.util.UUID observer,long sequence,int duration){escapeSounds.started(observer,sequence,level().getGameTime(),duration);}
    public void voiceFinished(java.util.UUID observer,long sequence){escapeSounds.finished(observer,sequence);}
    void resetEscapeSoundHold(){escapeSounds.resetHold();}
    boolean escapeSoundsFinished(){return escapeSounds.ready(level().getGameTime()) && level().getGameTime()>=hurtVoiceUntil;}
    public float burrowProgress(){return entityData.get(BURROW_PROGRESS)/1000F;}
    public int burrowDuration(){return entityData.get(BURROW_DURATION);}
    void burrowDuration(int ticks){entityData.set(BURROW_DURATION,ticks);}
    public boolean isPanicking(){return entityData.get(PANIC)>0;}
    public boolean isPanicFleeing(){return entityData.get(PANIC)==1;}
    public boolean isBurrowing(){return entityData.get(PANIC)==2;}
    public boolean isInRift(){return getPersistentData().getBoolean("RiftfallInRift");}
    public void setInRift(boolean inRift){getPersistentData().putBoolean("RiftfallInRift",inRift);}
    void panicVisuals(boolean panicked,boolean escaping,double progress){
        entityData.set(PANIC,!panicked?0:!escaping?3:getPersistentData().getBoolean("RiftfallBurrowing")?2:1);
        entityData.set(BURROW_PROGRESS,(int)Math.round(progress*1000));
    }
    public float burrowOffset(float partialTick){
        float progress=net.minecraft.util.Mth.lerp(Math.clamp(partialTick,0,1),previousBurrow,visualBurrow);
        if(!isBurrowing())return 0;
        return org.Azkerash.riftfall.behavior.BurrowExposure.offset(progress);
    }
    private org.Azkerash.riftfall.facial.FacialController face;
    @Override public org.Azkerash.riftfall.facial.FacialController facialController() { return face; }
    @Override public org.Azkerash.riftfall.facial.FaceFrame facialFrame() { return org.Azkerash.riftfall.facial.FaceFrame.unpack(entityData.get(FACE)); }
    private float previousVisualConfidence=.5F,visualConfidence=.5F;
    final GoblinSquadTactics squad=new GoblinSquadTactics(this);
    long bucklerAttackUntil, bucklerNextAttack;
    GoblinEntity coveredAlly;
    net.minecraft.world.entity.LivingEntity retreatThreat;
    long retreatCoverUntil;
    GoblinBehavior behavior;
    @Override public org.Azkerash.riftfall.confidence.CombatConfidenceController confidenceController() { return behavior.confidence; }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(PANTING,false); builder.define(GRAPPLED_PLAYER,-1); builder.define(ESCAPE_PROGRESS,0); builder.define(CONFIDENCE,50);builder.define(FACE,org.Azkerash.riftfall.facial.FaceFrame.NEUTRAL.pack());builder.define(PANIC,0);builder.define(BURROW_PROGRESS,0);builder.define(BURROW_DURATION,50);
    }
    public float tremorConfidence(float partialTick) {
        return level().isClientSide ? net.minecraft.util.Mth.lerp(partialTick,previousVisualConfidence,visualConfidence)
            : entityData.get(CONFIDENCE)/100F;
    }
    public int grappledPlayerId() { return entityData.get(GRAPPLED_PLAYER); }
    public int escapeProgress() { return entityData.get(ESCAPE_PROGRESS); }
    void grappleState(int playerId,int progress) { entityData.set(GRAPPLED_PLAYER,playerId); entityData.set(ESCAPE_PROGRESS,progress); }
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type,level);
        xpReward=isShaman()?12:5;
        face=new org.Azkerash.riftfall.facial.FacialController(this,org.Azkerash.riftfall.facial.FacialProfile.defaults().withBlinkTiming(2,2),packed->entityData.set(FACE,packed),()->isPanicking()?org.Azkerash.riftfall.facial.FaceExpression.SCARED:null);
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(isShaman()?GoblinItems.BONE_STAFF.get():isArcher()?Items.BOW:GoblinItems.SHORTSWORD.get()));
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
        if (!isArcher() || !org.Azkerash.riftfall.stamina.MobStamina.spend(this,"bow")) return;
        var arrow=net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(this,new ItemStack(Items.ARROW),power,getMainHandItem());
        double x=target.getX()-getX(),z=target.getZ()-getZ(),y=target.getY(.33)-arrow.getY();
        arrow.shoot(x,y+Math.sqrt(x*x+z*z)*.2,z,1.6F,6);
        playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT,1,1);
        level().addFreshEntity(arrow);
        getPersistentData().putInt("RiftfallArrowsShot",getPersistentData().getInt("RiftfallArrowsShot")+1);
    }
    @Override protected void hurtCurrentlyUsedShield(float damage) {
        if (damage >= 3 && org.Azkerash.riftfall.shield.ShieldCollisions.isShield(getUseItem()))
            getUseItem().hurtAndBreak(1 + (int) damage, this, getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND);
    }
    @Override public boolean isDamageSourceBlocked(net.minecraft.world.damagesource.DamageSource source) {
        // Shield contacts are resolved by GoblinPatch.tryHurt before vanilla shield handling.
        return org.Azkerash.riftfall.shield.ShieldCollisions.isShield(getUseItem())?false:super.isDamageSourceBlocked(source);
    }
    void playBlockedGrunt() {
        if(!level().isClientSide && isAlive() && !isSilent()) {
            playSound(GoblinSounds.GRUNT.get(),getSoundVolume(),.55F+getRandom().nextFloat()*.2F);
            hurtVoiceUntil=Math.max(hurtVoiceUntil,level().getGameTime()+24);
        }
    }
    @Override public void playSound(net.minecraft.sounds.SoundEvent sound,float volume,float pitch) {
        super.playSound(sound,volume,pitch);
        if(level().isClientSide || isSilent())return;
        int kind=sound==GoblinSounds.LAUGH.get()?GoblinVocalCuePayload.LAUGH:sound==GoblinSounds.HURT.get()?GoblinVocalCuePayload.HURT:
            sound==GoblinSounds.DEATH.get()?GoblinVocalCuePayload.DEATH:sound==GoblinSounds.GRUNT.get()?GoblinVocalCuePayload.GRUNT:-1;
        if(kind>=0)GoblinVocalCuePayload.send(this,kind,pitch);
    }
    @Override public boolean killedEntity(net.minecraft.server.level.ServerLevel world,net.minecraft.world.entity.LivingEntity victim) {
        boolean result=super.killedEntity(world,victim);
        // A simultaneous multi-kill produces one laugh, but panic and the former five-second cooldown cannot suppress victory voices.
        if(isAlive() && !isAlliedTo(victim) && lastKillLaugh!=world.getGameTime()) {
            lastKillLaugh=world.getGameTime();playSound(GoblinSounds.LAUGH.get(),.9F,.95F+getRandom().nextFloat()*.1F);
            getPersistentData().putInt("RiftfallKillLaughs",getPersistentData().getInt("RiftfallKillLaughs")+1);
        }
        return result;
    }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        // NeoForge can invoke this hook for a completely shield-blocked hit. The
        // client damage animation also invokes it independently of health loss.
        return null;
    }
    @Override protected void actuallyHurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
        float before=getHealth();
        super.actuallyHurt(source,amount);
        if(!level().isClientSide && isAlive() && !isSilent()) {
            if(getHealth()<before) {
                // Reductions, absorption and cancellation have already been applied.
                playSound(GoblinSounds.HURT.get(),getSoundVolume(),getVoicePitch());
                hurtVoiceUntil=level().getGameTime()+68;
            }else {
                // The client slices a random half-second from the existing scream in memory.
                playBlockedGrunt();
            }
        }
    }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {
        if(level().isClientSide) org.Azkerash.riftfall.client.GoblinFearAudio.stop(this);
        return GoblinSounds.DEATH.get();
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==3 && level().isClientSide) org.Azkerash.riftfall.client.GoblinFearAudio.stop(this);
        super.handleEntityEvent(event);
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
        if (!isRanged() && getMainHandItem().is(Items.STONE_SWORD))
            setItemSlot(EquipmentSlot.MAINHAND,getMainHandItem().transmuteCopy(GoblinItems.SHORTSWORD.get()));
        // Update the old default without overwriting deliberately customized speeds.
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed!=null && Math.abs(speed.getBaseValue()-.28)<.00001) speed.setBaseValue(.23);
        for (String key:java.util.List.of("RiftfallRetreating","RiftfallCoveringRetreat","RiftfallPanicked","RiftfallPanicking","RiftfallBurrowing","RiftfallBurrowProgress","RiftfallPanicEscapeAllowed","RiftfallPanicSafety","RiftfallBurrowDuration","RiftfallBurrowWaitingForSound",
                "RiftfallFormation","RiftfallHighGround","RiftfallFlanking","RiftfallScreening","RiftfallCasting",
                "RiftfallStaffAttack","RiftfallStaffApproaching","RiftfallSeekingHealing","RiftfallBehaviorNotes","RiftfallBehavior")) getPersistentData().remove(key);
        var scale=getAttribute(Attributes.SCALE);
        if (scale!=null && (Math.abs(scale.getBaseValue()-.56)<.00001 || Math.abs(scale.getBaseValue()-.72)<.00001))
            scale.setBaseValue(.75);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        behavior=new GoblinBehavior(this);
        goalSelector.addGoal(1,behavior.goal());
        if (isRanged()) targetSelector.addGoal(0,new GoblinTactics.CoverRetreat(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new GoblinSquadTactics.Assist(this));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.LivingEntity.class,10,true,false,
            target->GoblinHostility.undeadHunter(target) && !isAlliedTo(target)));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            previousBurrow=visualBurrow;visualBurrow=entityData.get(BURROW_PROGRESS)/1000F;
            previousVisualConfidence=visualConfidence;
            visualConfidence+=(entityData.get(CONFIDENCE)/100F-visualConfidence)*.15F;
        }
        if (!level().isClientSide) {
            // Reuse the shared engine's confidence; send only changed percentages.
            // The client smooths these values rather than scanning nearby mobs per rendered pose.
            if (tickCount%10==0) entityData.set(CONFIDENCE,(int)Math.round(behavior.confidence()*100));
            var target=getTarget();
            // Keep the client combat pose active while withdrawing or covering allies as well.
            setAggressive(target!=null && target.isAlive() && !isAlliedTo(target));
            GoblinBuckler.tick(this);
        }
    }
}

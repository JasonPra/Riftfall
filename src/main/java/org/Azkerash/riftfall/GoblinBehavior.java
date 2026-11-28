package org.Azkerash.riftfall;

import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;

import org.Azkerash.riftfall.behavior.*;
import org.Azkerash.riftfall.confidence.*;

import java.util.*;

/** Per-goblin engine integration. Only the shared weighted goal schedules combat actions. */
final class GoblinBehavior {
    final GoblinEntity mob;
    private final String entityId;
    final CombatConfidenceController confidence;
    final CombatBehaviorController awareness;
    private static final Double ONE = 1D, ZERO = 0D;
    private static final List<String> AMBUSH_ACTIONS =
            List.of("grapple", "skill_identity", "plunge", "melee");
    private static final Set<String> RETREATS =
            Set.of(
                    "panic_escape",
                    "cautious_retreat",
                    "warrior_retreat",
                    "ranged_retreat",
                    "seek_healing");
    private final GoalActionRegistry executors = new GoalActionRegistry();
    private final LookAtPlayerGoal look;
    private final SeekHealingCircleGoal healing;
    private final PanicEscapeGoal panic;
    private boolean looking;
    private CombatSituation situation;
    private long nextSearchPath;

    GoblinBehavior(GoblinEntity mob) {
        this.mob = mob;
        entityId =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(mob.getType())
                        .toString();
        confidence =
                new CombatConfidenceController(
                        mob,
                        ConfidenceConfig::settings,
                        mob::isAlliedTo,
                        ally ->
                                ally.getPersistentData().getBoolean("RiftfallRetreating")
                                        ? settings().retreatingMultiplier()
                                        : 1,
                        (target, factors) -> {
                            if (mob.isInRift()) {
                                factors.put("self.in_rift", 1D);
                            }
                            if (GoblinRally.active(mob)) {
                                factors.put("self.rallied", 1D);
                            }
                        },
                        16,
                        10);
        awareness =
                new CombatBehaviorController(
                        mob,
                        confidence,
                        100,
                        this::sense,
                        () -> GoblinTactics.interrupted(mob) || mob.isPanting());
        look = new LookAtPlayerGoal(mob, Player.class, 8);
        healing =
                new SeekHealingCircleGoal(
                        mob,
                        () -> ConfidenceConfig.healingSettingsFor(entityId),
                        () -> situation == null ? 0 : situation.confidence().points(),
                        caster ->
                                caster instanceof GoblinEntity shaman
                                        && shaman.isShaman()
                                        && mob.isAlliedTo(shaman));
        panic =
                new PanicEscapeGoal(
                        mob,
                        () -> ConfidenceConfig.panicSettingsFor(entityId),
                        () -> !mob.isShaman() && !GoblinRally.active(mob),
                        () -> !mob.isInRift(),
                        () -> GoblinTactics.interrupted(mob),
                        (state, pos) -> GoblinPanicVisuals.canDig(mob, state, pos),
                        new GoblinPanicVisuals(mob));
        executors.register("panic_escape", panic, () -> true);
        executors.register("seek_healing", healing, () -> false);
        executors.register(
                "regroup",
                new RegroupGoal(
                        mob,
                        other ->
                                other instanceof GoblinEntity goblin
                                        && !goblin.isBurrowing()
                                        && !goblin.isPanicFleeing(),
                        32,
                        10,
                        4,
                        1.05),
                () -> false);
        if (!mob.isShaman()) {
            executors.register(
                    "return_to_shaman",
                    new RegroupGoal(
                            mob,
                            other ->
                                    other instanceof GoblinEntity goblin
                                            && goblin.isShaman()
                                            && !goblin.isBurrowing()
                                            && !goblin.isPanicFleeing(),
                            48,
                            12,
                            6,
                            1.05,
                            4),
                    () -> false);
        }
        for (var slot : org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.ACTIVE_SLOTS) {
            var skillGoal = new org.Azkerash.riftfall.behavior.skills.MobSkillGoal(mob, slot);
            executors.register(
                    org.Azkerash.riftfall.behavior.skills.MobSkillGoal.behaviorId(slot),
                    skillGoal,
                    skillGoal::committed);
        }
        if (!mob.isRanged()) {
            executors.register("intercept", new GoblinInterceptGoal(mob), () -> false);
            executors.register("rear_approach", new GoblinRearAmbush(mob), () -> false);
            executors.register("cautious_retreat", new GoblinTactics.Retreat(mob), () -> false);
            executors.register(
                    "warrior_retreat", new GoblinTactics.WarriorRetreat(mob), () -> false);
            var grapple = new GoblinGrapple(mob);
            executors.register("grapple", grapple, () -> true);
            var plunge = new GoblinTactics.Plunge(mob);
            executors.register("plunge", plunge, () -> true);
            var melee = new GoblinTactics.Melee(mob);
            executors.register("melee", melee, () -> melee.remaining > 0);
        } else {
            executors.register("ranged_retreat", new GoblinTactics.Retreat(mob), () -> false);
            executors.register("position", new GoblinSquadTactics.Position(mob), () -> false);
            if (mob.isArcher()) {
                executors.register("bow", new GoblinTactics.Bow(mob), () -> false);
            }
            if (mob.isShaman()) {
                var staff = new GoblinShamanActions.StaffClub(mob);
                executors.register("staff", staff, () -> staff.windingUp);
                executors.register("rally", new GoblinRallyGoal(mob), () -> true);
                executors.register("heal", new GoblinShamanActions.Heal(mob), () -> true);
                executors.register("cast", MobSpellcasting.castingAction(mob), () -> true);
            }
        }
        executors.register("loot", new org.Azkerash.riftfall.loot.GoblinLootGoal(mob), () -> false);
        executors.register("wander", new WaterAvoidingRandomStrollGoal(mob, .8), () -> false);
    }

    private ConfidenceConfig.Settings settings() {
        return confidence.settings();
    }

    private void sense(CombatSituation snapshot, Map<String, Double> signals) {
        situation = snapshot;
        boolean rallied = GoblinRally.active(mob);
        signals.put("self.rallied", rallied ? ONE : ZERO);
        signals.put("self.in_rift", mob.isInRift() ? ONE : ZERO);
        if (rallied) {
            signals.put("engage", ONE);
            signals.put("bold", ONE);
        }
        executors.beginDecision();
        GoblinInterceptDash.grant(mob);
        if (!mob.isRanged()
                && mob.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                && !mob.getPersistentData().getBoolean("RiftfallBackstabGranted")) {
            var slot = yesman.epicfight.skill.SkillSlots.IDENTITY;
            if (org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.equipped(mob, slot) == null) {
                org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.equip(
                        mob, slot, RiftfallSkills.BACKSTAB.get());
            }
            mob.getPersistentData().putBoolean("RiftfallBackstabGranted", true);
        }
        panic.observe(snapshot);
        boolean rearOpening = snapshot.targetVisible() && GoblinRearAmbush.opening(mob);
        boolean committedAmbush = executors.anyCommitted(AMBUSH_ACTIONS);
        boolean escaping =
                panic.active() && panic.escapeAllowed() && !rearOpening && !committedAmbush;
        if (!snapshot.targetVisible()) {
            mob.getPersistentData().putBoolean("RiftfallFlanking", false);
            mob.getPersistentData().putBoolean("RiftfallScreening", false);
        }
        boolean interrupted = GoblinTactics.interrupted(mob);
        signals.put("self.interrupted", interrupted ? ONE : ZERO);
        signals.put("self.melee_role", mob.isRanged() ? 0D : 1D);
        signals.put("cover.safe", GoblinTactics.coverSafe(mob) ? ONE : ZERO);
        signals.put(
                "target.exposed",
                snapshot.targetVisible() && GoblinTactics.exposedToStaff(snapshot.target())
                        ? ONE
                        : ZERO);
        signals.put("target.unaware", rearOpening ? ONE : ZERO);
        var committedAction =
                interrupted || escaping ? null : executors.firstContinuingCommitment();
        for (var entry : executors.entries()) {
            String id = entry.id();
            boolean combat = !id.equals("wander") && !id.equals("loot");
            // An unwatched opening temporarily overrides flight; a latched grapple or airborne leap
            // finishes normally.
            if (id.equals("panic_escape") && (rearOpening || committedAmbush)) {
                entry.publish(signals, false);
                continue;
            }
            if (escaping && !id.equals("panic_escape")) {
                entry.publish(signals, false);
                continue;
            }
            // Hidden targets cannot start new attacks or track through walls. Committed swings may
            // finish naturally.
            boolean allowed =
                    combat
                            ? (snapshot.targetVisible()
                                    || entry.committed()
                                    || id.equals("heal")
                                    || id.equals("rally")
                                    || id.equals("seek_healing")
                                    || id.equals("panic_escape")
                                    || id.equals("regroup")
                                    || id.equals("return_to_shaman"))
                            : !snapshot.context().active("target.present");
            // Do not pathfind or advance idle ability cooldowns while another animation owns the
            // action slot.
            boolean advancing =
                    id.equals("melee")
                            || id.equals("grapple")
                            || (id.equals("skill_identity")
                                    && org.Azkerash.riftfall.behavior.skills.MobSkillLoadout
                                                    .equipped(
                                                            mob,
                                                            yesman.epicfight.skill.SkillSlots
                                                                    .IDENTITY)
                                            == RiftfallSkills.BACKSTAB.get())
                            || id.equals("plunge");
            boolean defensive =
                    id.equals("melee") && snapshot.context().get("target.distance") <= 1.8;
            boolean willing =
                    !advancing
                            || snapshot.context().active("engage")
                            || rearOpening
                            || defensive
                            || entry.committed();
            if (id.equals("cautious_retreat")) {
                willing = !snapshot.context().active("engage") && !rearOpening;
            }
            boolean retreatBlocked = rallied && RETREATS.contains(id);
            boolean ready =
                    !retreatBlocked
                            && !mob.isPanting()
                            && !interrupted
                            && allowed
                            && willing
                            && (committedAction == null || committedAction == entry)
                            && entry.prepare();
            entry.publish(signals, ready);
        }
        signals.put("ready.pant", mob.isPanting() ? ONE : ZERO);
        signals.put(
                "stamina.current",
                mob.staminaState().current(org.Azkerash.riftfall.stamina.MobStamina.maximum(mob)));
        signals.put("stamina.maximum", org.Azkerash.riftfall.stamina.MobStamina.maximum(mob));
        signals.put("stamina.panting", mob.isPanting() ? ONE : ZERO);
        signals.put(
                "stamina.regen_multiplier",
                org.Azkerash.riftfall.stamina.StaminaState.regenerationMultiplier(
                        mob.isPanting(), mob.staminaFleeing(), mob.staminaConfident()));
        panic.signals(signals);
        healing.signals(signals);
        if (!mob.isRanged()
                && mob.getPersistentData().getBoolean("RiftfallSeekingHealing")
                && snapshot.targetVisible()) {
            mob.retreatThreat = snapshot.target();
            mob.retreatCoverUntil = mob.level().getGameTime() + 40;
        }
        var notes = new net.minecraft.nbt.CompoundTag();
        if (panic.active()) {
            notes.putString(
                    "panic",
                    mob.isInRift()
                            ? "scared; escape blocked inside rift; ordinary retreat/regroup"
                            : mob.isWaitingUnderground()
                                    ? "underground; waiting for current voice to finish"
                                    : panic.burrowing()
                                            ? "burrowing; interruptible until complete"
                                            : "fleeing to find safe burrow ground");
        }
        if (healing.wanted()) {
            notes.putString("healing", healing.reason());
        }
        mob.getPersistentData().put("RiftfallBehaviorNotes", notes);
        var result = snapshot.confidence();
        var data = mob.getPersistentData();
        data.putDouble("RiftfallConfidence", result.value());
        data.putDouble("RiftfallConfidencePoints", result.points());
        data.putBoolean("RiftfallConfident", result.atLeast(settings().boldPoints()));
        var breakdown = new net.minecraft.nbt.CompoundTag();
        result.contributions().forEach(breakdown::putDouble);
        data.put("RiftfallConfidenceBreakdown", breakdown);
    }

    WeightedBehaviorGoal goal() {
        Map<String, BehaviorAction> actions = executors.actions();
        // Every policy ID has an implementation, even for roles where it can never become ready.
        for (String id : GoblinBehaviorProfile.engine().behaviorIds()) {
            actions.putIfAbsent(id, s -> {});
        }
        actions.put(
                "idle",
                new BehaviorAction() {
                    @Override
                    public void tick(CombatSituation s) {
                        mob.getNavigation().stop();
                        look();
                    }

                    @Override
                    public void stop() {
                        stopLook();
                    }
                });
        actions.put("interrupted", s -> mob.getNavigation().stop());
        actions.put(
                "pant",
                s -> {
                    mob.setSprinting(false);
                    mob.stopUsingItem();
                    mob.getNavigation().stop();
                });
        actions.put(
                "watch",
                s -> {
                    mob.getNavigation().stop();
                    if (s.target() != null) {
                        mob.getLookControl().setLookAt(s.target(), 45, 45);
                    }
                });
        actions.put(
                "search",
                new BehaviorAction() {
                    @Override
                    public void start(CombatSituation s) {
                        nextSearchPath = 0;
                    }

                    @Override
                    public void tick(CombatSituation s) {
                        if (s.lastSeenPosition() == null || mob.tickCount < nextSearchPath) {
                            return;
                        }
                        nextSearchPath = mob.tickCount + 10;
                        var pos = s.lastSeenPosition();
                        if (mob.position().distanceToSqr(pos) < 1) {
                            mob.getNavigation().stop();
                        } else {
                            mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1);
                        }
                    }

                    @Override
                    public void stop() {
                        mob.getNavigation().stop();
                    }
                });
        var wander = executors.action("wander");
        actions.put(
                "wander",
                new BehaviorAction() {
                    @Override
                    public void start(CombatSituation s) {
                        wander.start(s);
                    }

                    @Override
                    public void tick(CombatSituation s) {
                        wander.tick(s);
                        look();
                    }

                    @Override
                    public void stop() {
                        wander.stop();
                        stopLook();
                    }
                });
        return new WeightedBehaviorGoal(awareness, GoblinBehaviorProfile.engine(), actions);
    }

    private void look() {
        if (looking && !look.canContinueToUse()) {
            stopLook();
        }
        if (!looking && look.canUse()) {
            look.start();
            looking = true;
        }
        if (looking) {
            look.tick();
        }
    }

    private void stopLook() {
        if (looking) {
            look.stop();
        }
        looking = false;
    }

    double confidence() {
        return situation == null ? 0 : situation.confidence().value();
    }

    boolean bold() {
        return GoblinRally.active(mob)
                || situation != null && situation.confidence().atLeast(settings().boldPoints());
    }
}

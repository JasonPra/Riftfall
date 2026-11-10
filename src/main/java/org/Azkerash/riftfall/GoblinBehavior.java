package org.Azkerash.riftfall;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import org.Azkerash.riftfall.behavior.*;
import org.Azkerash.riftfall.confidence.*;

/** Per-goblin engine integration. Only the shared weighted goal schedules combat actions. */
final class GoblinBehavior {
    final GoblinEntity mob;
    final CombatConfidenceController confidence;
    final CombatBehaviorController awareness;
    private final Map<String,GoalBehaviorAction> executors=new LinkedHashMap<>();
    private final Map<String,BooleanSupplier> commitments=new HashMap<>();
    private final LookAtPlayerGoal look;
    private final SeekHealingCircleGoal healing;
    private final PanicEscapeGoal panic;
    private boolean looking;
    private CombatSituation situation;
    private long nextSearchPath;
    GoblinBehavior(GoblinEntity mob) {
        this.mob=mob;
        confidence=new CombatConfidenceController(mob,ConfidenceConfig::settings,mob::isAlliedTo,
            ally->ally.getPersistentData().getBoolean("RiftfallRetreating")?settings().retreatingMultiplier():1,
            (target,factors)->{},16,10);
        awareness=new CombatBehaviorController(mob,confidence,100,this::sense);
        look=new LookAtPlayerGoal(mob,Player.class,8);
        healing=new SeekHealingCircleGoal(mob,()->ConfidenceConfig.healingSettingsFor(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()),
            ()->situation==null?0:situation.confidence().points(),caster->caster instanceof GoblinEntity shaman && shaman.isShaman() && mob.isAlliedTo(shaman));
        panic=new PanicEscapeGoal(mob,()->ConfidenceConfig.panicSettingsFor(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()),
            ()->!mob.isShaman(),()->!mob.isInRift(),()->GoblinTactics.interrupted(mob),
            (state,pos)->GoblinPanicVisuals.canDig(mob,state,pos),new GoblinPanicVisuals(mob));
        add("panic_escape",panic,()->true);
        add("seek_healing",healing,()->false);
        add("regroup",new RegroupGoal(mob,other->other instanceof GoblinEntity goblin && !goblin.isBurrowing() && !goblin.isPanicFleeing(),32,10,4,1.05),()->false);
        if (!mob.isRanged()) {
            add("cautious_retreat",new GoblinTactics.Retreat(mob),()->false);
            add("warrior_retreat",new GoblinTactics.WarriorRetreat(mob),()->false);
            var grapple=new GoblinGrapple(mob);add("grapple",grapple,()->true);
            var plunge=new GoblinTactics.Plunge(mob);add("plunge",plunge,()->true);
            var melee=new GoblinTactics.Melee(mob);add("melee",melee,()->melee.remaining>0);
        } else {
            add("ranged_retreat",new GoblinTactics.Retreat(mob),()->false);
            add("position",new GoblinSquadTactics.Position(mob),()->false);
            if (mob.isArcher()) add("bow",new GoblinTactics.Bow(mob),()->false);
            if (mob.isShaman()) {
                var staff=new GoblinTactics.StaffClub(mob);add("staff",staff,()->staff.windingUp);
                add("heal",new GoblinTactics.Heal(mob),()->true);
                add("cast",MobSpellcasting.castingAction(mob),()->true);
            }
        }
        add("wander",new WaterAvoidingRandomStrollGoal(mob,.8),()->false);
    }
    private ConfidenceConfig.Settings settings() { return confidence.settings(); }
    private void add(String id,Goal executor,BooleanSupplier committed) {
        executors.put(id,new GoalBehaviorAction(executor));commitments.put(id,committed);
    }
    private void sense(CombatSituation snapshot,Map<String,Double> signals) {
        situation=snapshot;
        panic.observe(snapshot);
        boolean escaping=panic.active()&&panic.escapeAllowed();
        if (!snapshot.targetVisible()) {
            mob.getPersistentData().putBoolean("RiftfallFlanking",false);
            mob.getPersistentData().putBoolean("RiftfallScreening",false);
        }
        boolean interrupted=GoblinTactics.interrupted(mob);
        signals.put("self.interrupted",interrupted?1D:0D);
        signals.put("self.melee_role",mob.isRanged()?0D:1D);
        signals.put("cover.safe",GoblinTactics.coverSafe(mob)?1D:0D);
        signals.put("target.exposed",snapshot.targetVisible() && GoblinTactics.exposedToStaff(snapshot.target())?1D:0D);
        Map<String,Boolean> prepared=new HashMap<>();String committed=null;
        if (!interrupted && !escaping) for (var entry:executors.entrySet()) {
            if (entry.getValue().running() && commitments.get(entry.getKey()).getAsBoolean()) {
                boolean continuing=entry.getValue().prepare();prepared.put(entry.getKey(),continuing);
                if (continuing) { committed=entry.getKey();break; }
            }
        }
        String committedAction=committed;
        executors.forEach((id,action)-> {
            boolean combat=!id.equals("wander");
            if(escaping&&!id.equals("panic_escape")){signals.put("ready."+id,0D);signals.put("committed."+id,0D);return;}
            // Hidden targets cannot start new attacks or track through walls. Committed swings may finish naturally.
            boolean allowed=combat?(snapshot.targetVisible() || action.running() && commitments.get(id).getAsBoolean() || id.equals("heal") || id.equals("seek_healing") || id.equals("panic_escape") || id.equals("regroup")):
                !snapshot.context().active("target.present");
            // Do not pathfind or advance idle ability cooldowns while another animation owns the action slot.
            boolean advancing=id.equals("melee") || id.equals("grapple") || id.equals("plunge");
            boolean defensive=id.equals("melee") && snapshot.context().get("target.distance")<=1.8;
            boolean willing=!advancing || snapshot.context().active("engage") || defensive || action.running() && commitments.get(id).getAsBoolean();
            if (id.equals("cautious_retreat")) willing=!snapshot.context().active("engage");
            boolean ready=!interrupted && allowed && willing && (committedAction==null || committedAction.equals(id))
                && prepared.computeIfAbsent(id,key->action.prepare());
            signals.put("ready."+id,ready?1D:0D);
            signals.put("committed."+id,ready && action.running() && commitments.get(id).getAsBoolean()?1D:0D);
        });
        panic.signals(signals);
        healing.signals(signals);
        if (!mob.isRanged() && mob.getPersistentData().getBoolean("RiftfallSeekingHealing") && snapshot.targetVisible()) {
            mob.retreatThreat=snapshot.target();mob.retreatCoverUntil=mob.level().getGameTime()+40;
        }
        var notes=new net.minecraft.nbt.CompoundTag();if(panic.active())notes.putString("panic",mob.isInRift()?"scared; escape blocked inside rift; ordinary retreat/regroup":mob.isWaitingUnderground()?"underground; waiting for current voice to finish":panic.burrowing()?"burrowing; interruptible until complete":"fleeing to find safe burrow ground");if (healing.wanted()) notes.putString("healing",healing.reason());
        mob.getPersistentData().put("RiftfallBehaviorNotes",notes);
        var result=snapshot.confidence();var data=mob.getPersistentData();
        data.putDouble("RiftfallConfidence",result.value());data.putDouble("RiftfallConfidencePoints",result.points());
        data.putBoolean("RiftfallConfident",result.atLeast(settings().boldPoints()));
        var breakdown=new net.minecraft.nbt.CompoundTag();result.contributions().forEach(breakdown::putDouble);data.put("RiftfallConfidenceBreakdown",breakdown);
    }
    WeightedBehaviorGoal goal() {
        Map<String,BehaviorAction> actions=new LinkedHashMap<>(executors);
        // Every policy ID has an implementation, even for roles where it can never become ready.
        for (String id:GoblinBehaviorProfile.engine().behaviorIds()) actions.putIfAbsent(id,s->{});
        actions.put("idle",new BehaviorAction() {
            @Override public void tick(CombatSituation s) { mob.getNavigation().stop();look(); }
            @Override public void stop() { stopLook(); }
        });
        actions.put("interrupted",s->mob.getNavigation().stop());
        actions.put("watch",s->{
            mob.getNavigation().stop();if (s.target()!=null) mob.getLookControl().setLookAt(s.target(),45,45);
        });
        actions.put("search",new BehaviorAction() {
            @Override public void start(CombatSituation s) { nextSearchPath=0; }
            @Override public void tick(CombatSituation s) {
                if (s.lastSeenPosition()==null || mob.tickCount<nextSearchPath) return;
                nextSearchPath=mob.tickCount+10;var pos=s.lastSeenPosition();
                if (mob.position().distanceToSqr(pos)<1) mob.getNavigation().stop();
                else mob.getNavigation().moveTo(pos.x,pos.y,pos.z,1);
            }
            @Override public void stop() { mob.getNavigation().stop(); }
        });
        var wander=executors.get("wander");
        actions.put("wander",new BehaviorAction() {
            @Override public void start(CombatSituation s) { wander.start(s); }
            @Override public void tick(CombatSituation s) { wander.tick(s);look(); }
            @Override public void stop() { wander.stop();stopLook(); }
        });
        return new WeightedBehaviorGoal(awareness,GoblinBehaviorProfile.engine(),actions);
    }
    private void look() {
        if (looking && !look.canContinueToUse()) stopLook();
        if (!looking && look.canUse()) { look.start();looking=true; }
        if (looking) look.tick();
    }
    private void stopLook() { if (looking) look.stop();looking=false; }
    double confidence() { return situation==null?0:situation.confidence().value(); }
    boolean bold() { return situation!=null && situation.confidence().atLeast(settings().boldPoints()); }
}

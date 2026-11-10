# Reusable facial animation

The shared `facial` package handles presentation state, blinking, idle glances, combat-target tracking and temporary preview controls. It has no goblin type checks, confidence scoring, fear simulation, combat actions or navigation. Warriors, archers and shamans are its first integrations.

## What works now

- Neutral, angry, surprised, scared and uncertain faces.
- Automatic blinking, with open → half → closed → half → open stages. Each mob has independent timing (normally three to eight seconds between blinks; goblin blinks take six ticks / 0.3 seconds).
- Idle eyes wander in small glances rather than moving in synchronization with every other mob.
- When aggroed, eyes track the **visible combat target's eye position**, using angles relative to the mob's current head yaw and pitch. An already aligned head naturally has centered pupils. Eye travel is limited and smoothed.
- Acquiring a new visible combat target briefly selects surprised (one second), then the profile's combat expression (angry by default). Brief occlusion does not repeatedly retrigger surprise; losing the target returns to idle presentation.
- Scared is selected automatically while a goblin is panicking, including when the in-rift flag blocks escape. Uncertain remains available for previews. Choosing a face manually does not itself change combat behavior; see [panic integration](panic-escape.md).

Existing movesets, combat tremors, held-item placement, ears and shaman helmet are preserved. The face is a named `ModelPart` hierarchy: left/right sockets, flat eye whites, independently sliding pupils, upper eyelids, brows, and a small hinged mouth panel. No spheres or rotating eyeballs are used. Pupils are bounded inside the square whites and continue tracking behind closed lids. Frame interpolation smooths the motion between server ticks.

The vanilla layer follows the model head; the Epic Fight layer binds this same rig to the animated `Head` joint. The body armature and existing movesets remain intact. These are separate facial pivots, not extra joints inserted into Epic Fight's body armature. The mouth panel samples the original mouth/teeth pixels directly, preserving its flat silhouette and appearance at rest. It only tilts and drops slightly when opened; no protruding chin or new row of teeth is added. Future speech code can drive the jaw without changing eye tracking or expressions.

The original entity skins are used directly. The moved facial artwork is in the separate atlas area beginning at (32,0); the mouth panel samples (33,4) through (38,5), cropped to the five visible mouth pixels and centered beneath the nose. One static material atlas supplies amber eye, dark pupil/brow and skin colors for the articulated parts. No face-frame images or modified copies of the skins are generated.

## Test commands

Operator permission is required. Commands work with any entity implementing `FacialParticipant`, not just goblins. For example:

```mcfunction
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] expression angry
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] expression surprised
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] expression scared
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] expression uncertain
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] expression neutral
```

Use `expression cycle` to cycle all five expressions every two seconds, or `expression auto` to restore automatic expression selection. Change the entity type to `riftfall:goblin_archer` or `riftfall:goblin_shaman` to test those skins.

```mcfunction
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes left
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes right
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes up
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes down
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes center
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] eyes auto
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] blink once
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] blink closed
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] blink open
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] blink auto
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] inspect
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] reset
```

`blink half` is also available. `inspect` shows expression, blink stage, pupil coordinates and the actual tracked target ID/head-relative angles (or idle gaze). For a stable expression comparison, use `blink open` and `eyes center`; finish with `reset`. Forced closed eyes remain closed until changed/reset. Overrides are per-instance cosmetic previews and do not persist through unload/reload. They synchronize to everyone watching the entity, unlike the personal behavior hologram.

No-AI entities can still blink and glance while posing; they do not acquire a combat eye target. Target tracking requires a live, visible, attackable, non-allied target; creative and spectator players are excluded.

## Integration for another mob

1. Implement `FacialParticipant` on the mob.
2. Define a synchronized integer data accessor, initialized with `FaceFrame.NEUTRAL.pack()`.
3. Create one controller per entity after `super(...)`:

```java
face = new FacialController(this, FacialProfile.defaults(),
    packed -> entityData.set(FACE, packed));
```

4. Return it from `facialController()` and decode the synchronized value in `facialFrame()`:

```java
@Override public FacialController facialController() { return face; }
@Override public FaceFrame facialFrame() {
    return FaceFrame.unpack(entityData.get(FACE));
}
```

The shared entity-tick listener updates opted-in entities server-side. Synchronization uses one packed integer, changed only when the presentation frame changes. Clients use that frame to select/render the face; they do not independently invent combat targets. The controller stores only per-entity animation state.

`FacialProfile` supplies species-specific blink/glance intervals, surprise duration, yaw/pitch range, eye smoothing, idle expression, combat expression and blink phase duration. `withBlinkTiming(2,2)` gives goblins two half-closed ticks, two closed ticks and two reopening ticks, while preserving their three-to-eight-second blink interval. `FaceAnimator` is the pure state machine and can be tested without a Minecraft world. A custom animation source can feed it `Input(targetId, relativeYaw, relativePitch)` directly.

The rendering side is separate. `FacialRigPose` converts presentation state into pupil offsets, eyelid closure, brow pivots and jaw rotation. `ArticulatedFaceRig.Layout` supplies eye spacing, eye height and face depth; `Style` supplies eye/pupil proportions, pupil travel, brow size and resting expression. `AtlasPanel` binds an exact mouth region to head-local bounds, without cube padding or side faces. Another species can supply its own layout or renderer while consuming the same frame/controller.

`GoblinFacialLayer` binds that reusable rig to vanilla goblin heads; `PatchedGoblinFacialLayer` binds it to Epic Fight's `Head` joint. `GoblinFaceTextures` selects the untouched original skins and caches only the static material atlas until resource reload. `GoblinFaceBinding` centralizes the moved source region, mouth bounds, crueler resting eye style and Epic Fight head-surface correction.

The synchronized frame carries gaze on both axes from -100 to 100, blink 0–2, and jaw override -1 (expression controlled) or 0–15. The public preview `gaze` method uses horizontal -2..2 / vertical -1..1; animation smooths and normalizes this internally.

For jaw/speech integration, call `animator().jaw(0..15)` every tick from a future audio/phoneme source; `jaw(-1)` restores expression control. `talking(true)` is only a repeating preview oscillator, not voice playback or lip synchronization. `reset()` clears all previews.

```mcfunction
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] jaw talk
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] jaw 15
/riftfall face @e[type=riftfall:goblin,sort=nearest,limit=1] jaw auto
```

To move the mouth again, update `GoblinFaceBinding.MOUTH`'s atlas `u/v` and, if needed, model bounds. The current north-face binding mirrors U to match Minecraft's original head UV orientation. Mouth UVs span exact pixel edges; transparent pixels stay transparent. At rest, one texture pixel maps to 1.1 × 1.0625 model pixels on the vanilla head. Epic Fight's different head dimensions are accounted for by the head-surface transform in `PatchedGoblinFacialLayer`.

Combat render timing uses Epic Fight's fourth layer float (`partialTicks`). Its third float is head pitch and must never be used as a blend factor. Facial blends are bounded to [0,1] as a second guard against parts escaping the face during blinks. The mouth panel centers the visible fang-to-fang span; the atlas region's asymmetric transparent padding does not shift it sideways.

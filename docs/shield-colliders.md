# Physical shield blocking

Shield guards use oriented boxes attached to the Epic Fight armature. A weapon
must touch the shield before the defender's body. There is no shield-facing cone.
Epic Fight weapon sweeps use their actual OBB, line, or plane colliders; vanilla
attacks, which have no animated weapon collider, use a strike segment. Projectiles
use their movement segment. Shield-piercing damage and piercing arrows bypass it.

Ordinary guard clangs, including goblin shield blocks, play ten semitones lower
(pitch multiplier about 0.561), retaining the original random variation. Only
successful timed parries retain the original guard clang pitch. Holding the Parry
skill beyond its parry window produces the lower ordinary-block pitch.

Player guards retain Epic Fight's guard skill, stamina, and reactions. Goblins use
an NPC adapter in `GoblinShieldGuard`, returning Epic Fight's `BLOCKED` result,
playing its clash/recoil, and consuming shield durability. A blocked hit does not
play the health-loss scream. Sword guarding keeps Epic Fight's original rules.

Press **F3+B** to see magenta physical shield boxes while a shield is raised.
Existing F8 cyan item bounds are still available for model alignment.

## Model dimensions

`ShieldModels` reads JSON item elements, inherited geometry, element rotations,
third-person display transforms, and Epic Fight item-skin transforms. Bounds have
one model pixel of padding on each side. The buckler uses its own model dimensions
and the same forearm attachment correction as its renderer. Vanilla's Java-rendered
shield has an explicit profile matching its plate and handle.

Custom Java renderers do not expose their geometry as item-model JSON. Supply a
server data profile for these shields; otherwise they use vanilla-sized bounds
and log a warning. Client resource packs cannot redefine authoritative hitboxes.

For item `example:tower_shield`, create
`data/example/shield_colliders/tower_shield.json`:

```json
{
  "from": [-6, -11, -5],
  "to": [6, 11, 2],
  "display": {
    "thirdperson_righthand": {
      "rotation": [0, 90, 0],
      "translation": [10, 6, -4]
    },
    "thirdperson_lefthand": {
      "rotation": [0, 90, 0],
      "translation": [10, 6, 12]
    }
  }
}
```

Coordinates and display translations are in model pixels, before the item's
third-person transform. Padding is added automatically. The example uses vanilla
shield geometry; replace it with the custom renderer's bounds and transforms.
Datapack reload clears cached profiles. Add-ons can alternatively register a
`ShieldModels.Model` through `ShieldModels.register(itemId, model)`; registered
models supply their final bounds, including any desired padding.

The Epic Fight mixins target the project's pinned version, 21.17.3.1. Recheck their
injection points when upgrading Epic Fight, especially the player guard listener.

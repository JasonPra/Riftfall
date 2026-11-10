# GeckoLib outlined entity buffer compatibility

With GeckoLib 4.9.3, an Iron's Spells Necromancer wearing its Tarnished Crown can lose its torso, arms and some leg geometry when Epic Fight highlights it. The same issue occurs with ordinary glowing outlines and regular helmets.

`GeckoLibOutlineBufferMixin` patches `GeoRenderer.checkAndRefreshBuffer` on the client. It applies only to `GeoEntityRenderer` instances, an `OutlineBufferSource`, an expired consumer, and the initial model render (not layer rerenders). It uses GeckoLib's own recursive `bufferNeedsRefresh` check, then requests the entire replacement consumer once. Valid consumers and separate item/armor renderers continue through the original code.

## Cause

An outlined entity uses a paired vertex consumer for the visible model and its outline. Rendering headgear changes render type/texture and finishes the old batches. GeckoLib's original refresh can replace each expired half with another complete pair, so both children deliver vertices to the same underlying buffers. A quad `A B C D` becomes `A A B B C C D D`: two zero-area quads. The replacement must be used as a whole, without nesting it in the old pair.

This patch does not replace a mob model, renderer, animations, equipment, or textures. It is registered only in the client mixin list; dedicated servers do not load its rendering classes. Dependencies remain pinned to the requested files.

## Validation

- Full Gradle build and the existing 112 tests pass.
- Dedicated server and real client start successfully with the mixin applied.
- Native Necromancer checks cover the Tarnished Crown, iron helmet, enchanted crown, bare head, chest armor and a held sword.
- Both Epic Fight's red highlight and Minecraft's white glowing outline preserve the full body, including during live spellcasting.
- A live debugger inspected the actual patched consumer after expired buffers on `torso`, `right_arm` and `right_leg`. Each underlying body/outline builder receives each input vertex exactly once; the unpatched version delivered it twice.

For a manual regression check, summon a stationary outlined Necromancer:

```mcfunction
/summon irons_spellbooks:necromancer ~ ~ ~ {NoAI:1b,Glowing:1b,Tags:["outline_test"],ArmorItems:[{},{},{},{id:"irons_spellbooks:tarnished_helmet",count:1}]}
```

Check its full body, replace the head slot with an iron helmet and an enchanted crown, then test Epic Fight's targeting with `Glowing:0b` as well. A second glowing mob outside Epic Fight's selected target provides a separate white-outline control. Test normal AI/casting afterward.

Recheck this workaround when upgrading GeckoLib or Minecraft. It targets a private GeckoLib helper and a specific paired-consumer restoration defect; it can be retired once the upstream behavior is corrected and equivalent runtime checks pass.

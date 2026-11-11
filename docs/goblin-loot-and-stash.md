# Goblin loot and escaped thieves

All three goblin variants have 36 storage slots, plus normal worn equipment. Storage accepts any item and respects Minecraft's stack limits. It is exposed through vanilla `InventoryCarrier.getInventory()` and serialized as `RiftfallInventory`; item components, enchantments and quantities are preserved.

Goblins collect reachable dropped items with no pickup delay. Idle goblins can walk to nearby loot through the weighted `loot` behavior. Combat, panic, healing and searching have higher priority. Automatic scavenging respects `mobGriefing`. A full inventory leaves excess items on the ground, but can still accept an equipment upgrade by swapping worn gear and dropping the displaced item if storage has no room. Loot carriers are persistent rather than disappearing through ordinary distance despawning.

## Equipment selection

Weapons are compared using damage attributes (including Epic Fight capability modifiers) and Sharpness; armor uses protection, toughness and knockback resistance. Shamans also value mana capacity, regeneration and spell power. Enchantments and remaining durability break ties. Curses are penalized; binding effects prevent normal equipment swaps. Replaced gear goes into storage or drops nearby if no slot is available. Upgrades wait for committed attack/capture/burrow animations to end.

- Warriors and shamans equip one-handed melee weapons. Warriors can also equip shields.
- Only archers equip bows; they can upgrade to better or enchanted bows without changing roles. Other goblins store bows.
- Greatswords, longswords, tachi, spears, tridents, polearms and other recognized two-handed categories cannot be equipped. They can be carried as loot. Both Epic Fight parent categories and single-wield style metadata are checked, so add-on weapons can participate.
- `riftfall:goblin_forbidden_weapons` is an extra deny tag. `riftfall:goblin_one_handed_weapons` can identify uncategorized one-handed equipment; it cannot override the greatsword/polearm restrictions.
- The bone staff now declares a one-handed sword capability with its original collider. The shaman's custom club animations remain its combat moveset.
- Collecting a spellbook, staff or skill book does not grant spells or skills. Spellcasting remains restricted by the existing entity role.

Armor renders through Minecraft's humanoid armor layer and Epic Fight's corresponding patched layer. Jewelry can be stored, but this feature does not automatically equip Curios; its existing shared inventory/API remains available separately.

## Death and escape

On death, storage and picked-up worn equipment drop exactly once, in addition to the goblin's normal loot. Picked equipment is tracked separately from generated equipment to avoid duplicate vanilla drops. Returning stolen items does not depend on randomized drop chances or the normal mob-loot gamerule; it also returns picked-up vanishing-cursed items. Items already broken or consumed are naturally gone.

If a loot carrier completes its permitted burrow escape, the species adapter saves the full entity before the shared escape executor removes the active copy. Empty goblins retain ordinary escape behavior. The existing in-rift restriction still blocks burrowing out. Failed preservation prevents removal.

Records live in the world's overworld saved-data file `data/riftfall_escaped_goblins.dat`, keyed by UUID. Each includes the full entity NBT (inventory, worn equipment, health, attributes, attachments such as Curios/mana, and original UUID), source dimension, escape tick and data version. World saves persist the records; restart does not spawn them. They are retained until a future system consumes them.

There is no encounter generation, rift spawning, automatic respawn or record-consumption logic yet. The future encounter system can read copies through `EscapedGoblinStash.get(server).record(uuid)` and `ids()`; it must later implement an atomic consume/spawn transition so a saved thief cannot be duplicated.

Operator inspection:

- `/riftfall inventory inspect <goblin>` shows capacity, occupied slots, stored stacks and picked equipment slots.
- `/riftfall stash list` shows the saved thief count and up to 100 UUIDs.
- `/riftfall stash inspect <uuid>` shows a complete saved record without spawning or removing it.

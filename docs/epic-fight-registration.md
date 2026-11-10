# Epic Fight registration migration

The goblin shortsword uses Epic Fight 21.17.3.1's `MovesetRegister` and
`ProviderConditionalRegister` in `GoblinCombatRegistries`. Both registers attach
to Riftfall's mod event bus. The weapon preset references their deferred holders.

| Offhand | Style | Ground attacks, in order |
| --- | --- | --- |
| Empty | `GOBLIN_FRENZY` | Sword swipe, reverse swipe, frenzy |
| Shield | `ONE_HAND` | Slash, backswing, frenzy, shield overhead |
| Other occupied hand | `ONE_HAND` | Same existing occupied-hand combo |

Both movesets retain sword dash and goblin plunge in the final dash/aerial slots.
Each equipment condition has a distinct registry ID, including the shield and
ordinary occupied-hand conditions, which share a style but have different offhand
compatibility rules. Epic Fight's base offhand compatibility check still applies.
No mounted attack set or innate skill is introduced.

The preset retains the weapon category, collider, sounds, and main-hand-only
placement. Goblin AI still chooses its attacks through `GoblinSwordCombat.select`;
the animation files and their timing are unchanged by this migration.

Client event subscribers let NeoForge infer the event bus from the subscribed
event types instead of supplying the deprecated `EventBusSubscriber.Bus.MOD`.

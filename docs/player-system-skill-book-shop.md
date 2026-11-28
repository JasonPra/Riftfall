# Epic Fight skill books in Player System's Normal shop

The rotating Daily section of the Normal shop can offer twelve Epic Fight skill books alongside its original goods. Player System's separate direct-learning skill catalogue remains unchanged. Books are actual `epicfight:skillbook` stacks with an `epicfight:skill` component; buying one does not automatically learn or equip it. Epic Fight's normal learning, prerequisite and equipment rules apply.

Each book has stock 1, weight 2, and is nonpermanent. Ordinary gear commonly has weight 12, so books are occasional offers rather than guaranteed stock. The chance depends on the player's eligible catalogue and shop configuration. Player System continues to control coins, discounts, rank gating, rotation, refreshes, inventory capacity checks and purchase limits. Reopening the panel does not generate a fresh selection.

| Skill | Base coin price | Minimum rank |
| --- | ---: | --- |
| Roll | 120 | F− |
| Step | 150 | F− |
| Guard | 180 | F− |
| Endurance | 180 | E− |
| Stamina Pillager | 220 | E− |
| Technician | 350 | D− |
| Emergency Escape | 350 | D− |
| Parrying | 500 | C− |
| Impact Guard | 400 | C− |
| Hypervitality | 400 | C− |
| Forbidden Strength | 600 | B− |
| Phantom Ascent | 700 | A− |

Entries live under `data/riftfall/playersystem/shop/epic_fight_*.json`, so a server datapack can tune their prices, ranks, weights and stock. The small ShopEntry mixin supplies translated listing names such as “Skill Book: Roll”: Player System's shop protocol transmits a translation key, not an item's components or tooltip, and would otherwise label every book identically. Other shop entries retain their normal labels.

Validation uses the installed Player System sampler against its original shop catalogue to check rank gates and occasional appearances. A native server check loads all twelve entries and skill holders, checks that the skills are learnable, buys a currently offered book with coins, verifies the delivered skill and charged amount, and rejects both sold-out and unoffered purchases. The temporary server fixture is excluded from the release jar.

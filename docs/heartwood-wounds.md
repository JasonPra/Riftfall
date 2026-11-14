# Heartwood wound healing

A stripped patch on a living Heartwood branch gathers honey-like sap runs and rounded drips, thickens into a coating, hardens into red-amber resin, then grows bark back from its edges. Some timber stays visible between the viscous sap runs. Its emissive overlay uses a soft light floor rather than full brightness; the wood retains ordinary lighting. Resin has burgundy-red color, amber inclusions and glassy highlights.

Each patch keeps its own saved age. Stripping another patch does not reset older wounds. Collecting sap restarts the collected patch’s healing clock. Ordinary tree growth and radius changes do not bypass this clock. Only wounded segments carry block entities; healthy tree blocks have no healing ticker.

## Initial timing

| Loaded living-tree time since stripping or bottling | Appearance |
| --- | --- |
| 0–5 minutes | Exposed heartwood |
| 5–10 minutes | Thin honey-like sap runs |
| 10–20 minutes | Thicker sap coating |
| 20–40 minutes | Red-amber resin |
| 40–47 minutes | Bark starts closing over the resin |
| 47–54 minutes | Bark covers most of the wound |
| 54–60 minutes | Small resin scars remain |
| 60 minutes | Ordinary bark restored |

These are initial gameplay timings for review. Progress uses loaded server ticks, not elapsed offline time. Root connectivity is checked through already-loaded blocks, without loading chunks. Detached branches pause, as do wounds whose root connection cannot currently be established. Root soil with no fertility can still belong to a living tree; fertility is not used as a death flag.

Healing restores the matching ordinary branch state while preserving thickness and waterlogging. Once all its patches have closed, the wound block entity is removed. An open patch yields bark only when first stripped. It can yield bark again after the complete healing cycle. Sap can be collected into bottles during its wet stages; healing does not create automatic item drops.

Placed building logs retain normal stripping behavior and do not heal. Cut end grain still rejects stripping. Bark shoulders continue to follow their adjoining side patch.

## Collecting sap

Right-click a wet wound with a glass bottle to obtain **Bottled Heartwood Sap** (`riftfall:heartwood_sap_bottle`). Wet sap is available during the 5–20 minute portion of the cycle. Hardened resin and closing bark cannot be bottled.

Bottling removes the sap from the clicked surface patch and resets that patch to exposed wood at age zero. It remains wounded and cannot be stripped again. Fresh sap gathers again after approximately five minutes of loaded, root-connected time. Repeated collection keeps that wound open; leaving it alone permits the full one-hour healing cycle. Other wounds keep their own progress.

Survival collection consumes one glass bottle and uses vanilla filled-container inventory handling. Bottled sap stacks to 16 and has an empty glass bottle crafting remainder for future recipes. It is currently an ingredient rather than food.

## Developer review

The existing operator permission on `/legacytree` applies to these commands:

```mcfunction
/legacytree wound <x> <y> <z>
/legacytree wound <x> <y> <z> advance 10
```

Target a wounded segment or its thick-trunk shell. The first command reports patch stages and loaded living-tree ages. `advance` adds the requested number of minutes to that segment's wounds for review; it also requires a loaded living root connection. It does not alter the world's clock or normal healing speed.

Regenerate the material sprites with `python3 tools/assets/generate-heartwood-wounds.py`. Wet stages use stable animated coverage with a subtle shimmer; there are no fully invisible animation frames. Partial bark recovery uses the same procedural bark material as the healthy trunk.

Initial native interaction checks completed a living wound’s full cycle and confirmed that a detached segment would not heal. Bottle interactions through a thick-trunk shell verified bottle consumption, healing restart, sap regrowth, repeated-click protection, rejection of resin collection, and blocked axe stripping during healing. Day/night stage previews use accelerated patch ages. Broader pre-shipping verification is deferred until the visuals are accepted.

[Daytime stage preview](previews/heartwood-wound-day.png) · [Nighttime glow preview](previews/heartwood-wound-night.png) · [Bottled sap](previews/heartwood-sap-bottle.png)

The preview is ordered left to right: top row is exposed wood, thin sap and thick sap; middle row is resin and two stages of closing bark; bottom row is late closing bark, healed bark and a freshly bottled surface whose healing has restarted.

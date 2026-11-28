# Action wheels

Press **grave/backtick (`)** to open the client-side action wheels. Rebind **Riftfall
→ Action wheels** in Minecraft's Controls screen if desired. Click a wheel, then
click an action; **1–6** also selects clockwise from the top. Scroll changes pages
when a wheel has more than six entries. Right-click or Backspace goes back;
Escape backs out of a subwheel and then closes the root. The opener also closes
it. Hovering alone never activates anything. The world continues running.

The initial groups cover Menus & books, Magic, Combat, and Utility. Menu/utility
shortcuts and the bundled native spell-selection menus move to wheel-only mode.
Immediate combat controls retain their direct bindings by default, including
casting, attacking, guarding, and movement. The Combat wheel offers additional tap
access to selected combat actions. **All Player System mappings, including its
ability keys, and vanilla controls are excluded from assignment and rebinding.**

## Editing

Choose **Edit wheels** at the bottom of the wheel. Cycle the group selector, search
installed keybindings by their label, category, or translation ID, and use **+ / −**
to add or remove them. Create additional named groups as needed. Groups and action
lists can have multiple pages. Each action can be **Wheel only** or **Keep direct
key**; this policy is shared if the action appears in more than one group.

Wheel-only assignment saves the previous key and modifier before unbinding it.
Removing its last assignment or choosing Keep direct key restores that saved
binding, unless a new direct binding has since been assigned in Controls. **Restore
all direct keys** restores every managed shortcut while leaving the wheels usable.
This does not replace Minecraft's unrelated keybinding settings. It may restore
preexisting conflicts if the original keybinds conflicted.

Preferences and saved keys live in `config/riftfall-action-wheels.json`. Changes
persist across worlds/restarts. Keep that file until using Restore all direct keys;
deleting it first also deletes the saved recovery bindings. Missing mods' entries
are retained in preferences and hidden from the current wheel. Invalid configuration
files are left intact and prevent automatic rebinding rather than being overwritten.

## Input compatibility

Ordinary actions queue a single click and a short press on the selected mod's own
KeyMapping after the wheel closes, then release it. No global key event is broadcast
and no server permissions or action requirements are bypassed. Actions that need a
held item, target, learned spell, or particular game state retain those requirements.
Hold-to-charge, modifier-only, and GUI-only shortcuts should keep direct keys; the
wheel provides tap actions, not a latched combat button.

Dedicated adapters support Ars Nouveau's raw-code key handler, Iron's spell wheel's
toggle mode, and Quintessence's hold-to-select wheel. The latter two remain open for
normal click selection without holding the old shortcut. Ars radial menus opened
from this wheel likewise use click selection. Other future mods that poll physical
GLFW keys directly may need an adapter rather than a standard KeyMapping pulse.

The feature is client-only and needs no server payload or server configuration.

Every visible panel has a distinct pixel material. Labels wrap and fit inside their
sector without ellipses; hovering also shows the original full keybinding label.
The eight deterministic material textures can be regenerated with
`python tools/generate_wheel_textures.py` (Pillow).

# FantaClient GUI implementation

The main in-game click GUI has been rebuilt around the supplied `GUI-REFERENCE.png` design:

- Dark translucent overlay keeps the Minecraft world visible behind the menu.
- Charcoal panel, orange accent line, FantaClient header, left navigation rail and orange selected-category state.
- Two-column module cards with orange enabled switches and muted disabled switches.
- Sidebar categories: Modules, HUD, Base Hunt, Combat and Utility.
- Mouse-wheel scrolling through module cards when a category has more than 14 entries.
- Left-click a module card to toggle it; right-click a module card to open its module settings.
- Scan, Waypoint, Settings and Close actions are clickable in the bottom bar.
- HUD Move opens a compact matching editor card. Drag the HUD preview to move it, use the scale bar or mouse wheel to resize it, and Reset Position to restore defaults.
- The supplied screenshot is included as `GUI-REFERENCE.png` for reference; the live interface is drawn by Java and is not a static screenshot overlay.

The Fanta Orange theme, Compact Layout, Right Shift menu key, W unbound-at-start behavior and existing module implementation are retained. Java/Gradle compilation must still be verified by the repository's GitHub Actions workflow.

# FantaClient – Demon Edition

Client-side Fabric mod project for **Minecraft Java 1.21.11**. The theme is orange / red / purple. The in-game menu opens with **Right Shift**; press **G** to save a waypoint.

## Build on GitHub

1. Create a GitHub repository and upload the contents of this folder (so `build.gradle` is in the repository root).
2. Push to the `main` branch, or run the **Build FantaClient** workflow from the Actions tab.
3. Download the `FantaClient-1.21.11` artifact from the completed workflow run. The built mod JAR is inside `build/libs/`.
4. Install Fabric Loader for Minecraft 1.21.11 and the matching Fabric API, then place the JAR in `.minecraft/mods`.

Local build: `./gradlew build` (or `gradle build` if Gradle is installed). Java 21 is required.

## Current functionality

- Right Shift opens the FantaClient ClickGUI.
- Toggle Sprint automatically sprints while moving forward.
- Fullbright raises gamma and restores the previous gamma value when disabled.
- Nearby scanner checks only chunks already loaded on the client, in a configurable-in-code radius (24 blocks) and a vertical band around the player.
- Chest ESP / Storage ESP / Shulker ESP / Spawner ESP / Portal Finder / Ore Hints add matching nearby block clues to the scanner list.
- Sus Chunk Finder scores loaded chunks using detected storage, portal, and spawner clues. It is a heuristic, not proof that a base exists.
- Cave Finder adds rough underground air-pocket clues; false positives are expected.
- Waypoints: press G or click the button to save your current coordinates. The latest saved waypoint is shown in the menu.
- Coordinates and current chunk coordinates, inventory totem count, armor durability, nearby player radar, crosshair target health, nearby ender pearls, and low-durability warnings are available in their respective menu modules.

## Important limitations

This is a **client-side scanner**, not an x-ray of server data. It can only inspect blocks/entities the server has sent to the client in currently loaded chunks. It does not reveal unloaded chunks, hidden inventories, or server-side data. The current ESP-style modules report detections in the ClickGUI; they do not yet draw persistent 3D boxes through walls. Chunk scores and cave hints are approximate and can produce false positives.

Combat features are informational/practice helpers only. There is no autonomous player-attacking bot, packet spoofing, anti-cheat bypass, or server permission bypass. Respect each server's rules.

## Controls

- **Right Shift** — open ClickGUI
- **G** — save waypoint at current position

## Settings, keybinds and config sharing

- Open the ClickGUI (default Right Shift; can be changed in Settings).
- Open **Settings / Config** for scanner radius, scan interval, marker limit and Sus Chunk score threshold.
- Left-click a module tile to toggle it. Right-click a module tile, then press any key to assign a custom toggle hotkey. Press Escape to cancel.
- The menu and waypoint actions also have rebind buttons in Settings.
- Config is saved automatically to `.minecraft/config/fantaclient/config.json`.
- **Export Config (Copy)** copies a portable JSON profile to the clipboard. Share that text with a friend; they can paste it using **Import Config (Paste)**. Profiles include module states, hotkeys and scanner settings.
- **Save Config** writes the current profile immediately.

Note: module hotkeys are client-side. Avoid assigning the same key to multiple actions. The scanner only inspects chunks currently loaded by the client; it cannot reveal server-side hidden data.


## Full Auto PvP Bot (Singleplayer)

The experimental `Full Auto PvP Bot (Singleplayer)` module is intentionally restricted to an integrated singleplayer world. It selects the nearest other player within 18 blocks, turns toward them, approaches with forward/sprint input, and attacks in melee range when the vanilla attack cooldown is ready. It pauses while a screen is open and releases movement keys when disabled or blocked. It does not run on multiplayer servers.

This is a prototype and should be tested in a local/LAN test world with a consenting second player. It does not perform crystal combat, inventory swaps, potion use, pathfinding around obstacles, or autonomous looting.


## Added hotbar helpers

- **Auto Tool**: chooses the fastest tool in the hotbar for the block under the crosshair.
- **Auto Eat (Hotbar)**: uses food from the hotbar when hunger falls below 18, then restores the previous slot.

These are client-side helpers. Server rules may prohibit automated actions; use them only where permitted.


## Added client utilities

This source includes FPS Counter, Ping Display, Direction HUD, Speedometer, Player Radar+, and Mob Radar+. Radar modules display only entities already tracked by the local client; they do not reveal hidden players, unloaded chunks, or server-side data. These are informational client-side features and do not bypass server anti-cheat.


## Additional PvP and base-hunting modules

This build adds Base Activity Finder, Redstone Clue Finder, Workstation Finder, and Buried Storage Finder. These scan only blocks in chunks already loaded by the client and show coordinate clues in the menu; they do not reveal unloaded chunks or server-hidden bases. Base Activity Finder includes common workstations, redstone components, bells, bee blocks, and campfires. Buried Storage Finder extends the vertical scan downward.

PvP additions include Enemy Gear HUD (reads the aimed-at living entity's visible equipment), Totem Alert, Low Health Alert, and Crystal Warning. These are informational client-side alerts and do not bypass server anti-cheat.


## FantaClient cosmetics update

- The only visual theme is now **Fanta Orange**. Arcane Purple and Crimson Night have been removed from the settings cycle.
- **Compact Layout** is now the permanent default and cannot be switched off. Older configs are normalized to these choices.
- Added the **Fanta Totem** cosmetics module and an included resource pack that replaces the vanilla Totem of Undying item texture with a pixel-art orange soda bottle.

### Enabling the Fanta Totem texture

1. Copy the `FantaTotem-ResourcePack` folder (or make a ZIP of its contents) into your Minecraft `.minecraft/resourcepacks` folder.
2. In Minecraft, open **Options → Resource Packs** and move **FantaClient - Fanta Totem Bottle Texture** to the enabled side.
3. Toggle **Fanta Totem** in the FantaClient menu as your cosmetic preference. The texture itself is controlled by Minecraft's resource-pack system, so enabling/disabling the pack is what actually changes the item texture.

The pack changes the Totem of Undying texture for the local client only; it does not change the item or texture on the server or for other players.

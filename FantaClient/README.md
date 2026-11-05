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

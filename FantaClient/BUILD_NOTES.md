# FantaClient Demon Edition — build notes

Target: Minecraft Java 1.21.11, Fabric, Java 21.

The 1.21.11 screen input API uses `Click` and `KeyInput` records. The GUI screens have
been updated to use those signatures. Armor status now reads equipment through
`LivingEntity#getEquippedStack(EquipmentSlot)` rather than the removed inventory armor list.

The Gradle build pins Fabric Loom 1.14.10. The GitHub Actions workflow is at the
repository root in `.github/workflows/build.yml` and builds the project from `FantaClient/`.
It uploads every JAR produced by Gradle, including sources/dev JARs when those tasks
are enabled by the project.

Build locally from the repository root with:

    cd FantaClient
    gradle clean build --no-daemon

A successful CI run is still required to verify the full Minecraft/Fabric dependency
resolution and compilation in GitHub Actions.


## Multiplayer compatibility

Auto Tool and Auto Eat (Hotbar) use normal client-side slot selection and item-use
actions, so they can operate in multiplayer as well as singleplayer. The experimental
Full Auto PvP Bot is no longer hard-blocked in multiplayer; it sends ordinary movement
and attack actions through the client. Multiplayer servers remain authoritative and
may reject actions, apply anti-cheat checks, or prohibit these modules. No server-side
protections are bypassed.


## More PvP / base-hunting additions
- Added Base Activity Finder, Redstone Clue Finder, Workstation Finder, and Buried Storage Finder. Scans client-loaded chunks only.
- Added Enemy Gear HUD, Totem Alert, Low Health Alert, and Crystal Warning.
- Compact module tiles to fit the expanded module list.
- Compilation has not been verified in this environment; use the repository GitHub Actions workflow to build.

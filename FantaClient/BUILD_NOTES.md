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

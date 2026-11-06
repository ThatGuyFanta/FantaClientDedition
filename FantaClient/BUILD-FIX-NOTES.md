# Build fix

Fixed the Minecraft 1.21.11 GUI matrix stack API calls:
- `Matrix3x2fStack.push()` -> `pushMatrix()`
- `Matrix3x2fStack.pop()` -> `popMatrix()`

This addresses the four `cannot find symbol` errors reported in `HudOverlay.java` and `HudMoveScreen.java`.
Run `gradle clean build --no-daemon` in GitHub Actions to verify the complete project; this environment did not run the Gradle build.

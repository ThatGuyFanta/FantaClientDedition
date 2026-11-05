# FantaClient performance / GUI crash fix

- Removed explicit Minecraft background blur calls from FantaScreen and SettingsScreen.
- Replaced the blurred backdrop with a lightweight flat dark background in Fanta Orange styling.
- Reduced default scanner radius from 24 to 12 blocks.
- Increased default scan interval from 40 to 100 ticks (5 seconds).
- Reduced default marker cap to 80 and vertical scan range.

The supplied crash log identifies `IllegalStateException: Can only blur once per frame` in `FantaScreen`.
This patch addresses that render path. Compilation still needs to be confirmed by the repository's Gradle build.

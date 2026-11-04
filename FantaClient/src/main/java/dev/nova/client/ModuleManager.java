package dev.nova.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/** Client-side helpers. Scans only chunks already present in the client world. */
public final class ModuleManager {
    private ModuleManager() {}

    private static final Map<String, Boolean> enabled = new LinkedHashMap<>();
    private static final List<Finding> findings = new ArrayList<>();
    private static final List<BlockPos> waypoints = new ArrayList<>();
    private static final Map<String, Integer> hotkeys = new LinkedHashMap<>();
    private static long lastScanTick = -100;
    private static String scanStatus = "Waiting for a world";
    private static int scanRadius = 24;
    private static int scanInterval = 40;
    private static int maxFindings = 120;
    private static int susChunkThreshold = 5;
    private static Double previousGamma = null;
    private static String autoPvpStatus = "Off";
    private static PlayerEntity autoPvpTarget = null;

    public record Finding(String type, BlockPos pos, int score) {}

    public static void registerDefaults() {
        for (String name : List.of(
                "Sus Chunk Finder", "Chest ESP", "Storage ESP", "Shulker ESP", "Spawner ESP",
                "Portal Finder", "Cave Finder", "Chunk Info", "Waypoints", "Entity Radar",
                "Coordinates HUD", "Fullbright", "Toggle Sprint", "Target HUD", "Totem Counter",
                "CPS Counter", "Pearl Alerts", "Combo Trainer", "Crystal Helper", "Auto Eat",
                "Armor Status", "Durability Warnings", "Mob Radar", "Ore Hints", "Logout Spots",
                "Full Auto PvP Bot (Singleplayer)")) {
            enabled.put(name, false);
            hotkeys.put(name, -1);
        }
        hotkeys.put("Open Menu",  GLFW_KEY_RIGHT_SHIFT);
        hotkeys.put("Add Waypoint", GLFW_KEY_G);
        ConfigManager.load();
    }

    private static final int GLFW_KEY_RIGHT_SHIFT = 344;
    private static final int GLFW_KEY_G = 71;

    public static Map<String, Integer> hotkeys() { return Collections.unmodifiableMap(hotkeys); }
    public static int hotkey(String name) { return hotkeys.getOrDefault(name, -1); }
    public static void setHotkey(String name, int key) { if (hotkeys.containsKey(name)) { hotkeys.put(name, key); ConfigManager.save(); } }
    public static int scanInterval() { return scanInterval; }
    public static void setScanInterval(int ticks) { scanInterval = Math.max(10, Math.min(400, ticks)); ConfigManager.save(); }
    public static int maxFindings() { return maxFindings; }
    public static void setMaxFindings(int value) { maxFindings = Math.max(20, Math.min(500, value)); ConfigManager.save(); }
    public static int susChunkThreshold() { return susChunkThreshold; }
    public static void setSusChunkThreshold(int value) { susChunkThreshold = Math.max(2, Math.min(20, value)); ConfigManager.save(); }

    public static boolean isEnabled(String name) { return enabled.getOrDefault(name, false); }
    public static void setEnabled(String name, boolean value) {
        enabled.put(name, value);
        if (name.equals("Fullbright")) applyFullbright(value);
        ConfigManager.save();
    }
    public static Map<String, Boolean> states() { return Collections.unmodifiableMap(enabled); }
    public static List<Finding> findings() { return Collections.unmodifiableList(findings); }
    public static List<BlockPos> waypoints() { return Collections.unmodifiableList(waypoints); }
    public static String scanStatus() { return scanStatus; }
    public static String autoPvpStatus() { return autoPvpStatus; }
    public static int scanRadius() { return scanRadius; }
    public static void setScanRadius(int radius) { scanRadius = Math.max(8, Math.min(48, radius)); ConfigManager.save(); }

    private static void applyFullbright(boolean on) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            if (on) {
                if (previousGamma == null) previousGamma = client.options.getGamma().getValue();
                client.options.getGamma().setValue(16.0);
            } else if (previousGamma != null) {
                client.options.getGamma().setValue(previousGamma);
                previousGamma = null;
            }
        }
    }

    public static void addWaypoint() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        BlockPos pos = client.player.getBlockPos().toImmutable();
        if (!waypoints.contains(pos)) waypoints.add(pos);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (isEnabled("Toggle Sprint") && client.currentScreen == null && client.player.forwardSpeed > 0.0F) {
            client.player.setSprinting(true);
        }
        tickAutoPvp(client);
        boolean wantsScan = isEnabled("Sus Chunk Finder") || isEnabled("Chest ESP") || isEnabled("Storage ESP")
                || isEnabled("Shulker ESP") || isEnabled("Spawner ESP") || isEnabled("Portal Finder")
                || isEnabled("Cave Finder") || isEnabled("Ore Hints");
        if (wantsScan && client.world.getTime() - lastScanTick >= scanInterval) scanLoadedArea(client);
    }

    public static void scanNow() { scanLoadedArea(MinecraftClient.getInstance()); }

    /**
     * Autonomous combat prototype, deliberately restricted to an integrated singleplayer world.
     * It never operates on multiplayer servers. It selects the nearest other player, faces them,
     * walks into melee range, and attacks when the vanilla attack cooldown is ready.
     */
    private static void tickAutoPvp(MinecraftClient client) {
        boolean enabledNow = isEnabled("Full Auto PvP Bot (Singleplayer)");
        if (!enabledNow) {
            releaseAutoPvpKeys(client);
            autoPvpTarget = null;
            autoPvpStatus = "Off";
            return;
        }
        if (!client.isInSingleplayer()) {
            releaseAutoPvpKeys(client);
            autoPvpTarget = null;
            autoPvpStatus = "Blocked: singleplayer/test world only";
            return;
        }
        if (client.currentScreen != null || client.interactionManager == null) {
            releaseAutoPvpKeys(client);
            autoPvpStatus = "Paused";
            return;
        }

        PlayerEntity self = client.player;
        PlayerEntity target = null;
        double bestDistance = 18.0 * 18.0;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator()) continue;
            double distance = self.squaredDistanceTo(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = candidate;
            }
        }
        autoPvpTarget = target;
        if (target == null) {
            releaseAutoPvpKeys(client);
            autoPvpStatus = "Searching: no player within 18 blocks";
            return;
        }

        var delta = target.getEyePos().subtract(self.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (MathHelper.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(delta.y, horizontal) * (180.0 / Math.PI));
        self.setYaw(yaw);
        self.setPitch(pitch);

        double distance = Math.sqrt(bestDistance);
        boolean move = distance > 3.0;
        client.options.forwardKey.setPressed(move);
        client.options.sprintKey.setPressed(move && distance > 6.0);
        if (distance <= 3.6 && self.getAttackCooldownProgress(0.5F) >= 0.9F) {
            client.interactionManager.attackEntity(self, target);
            self.swingHand(Hand.MAIN_HAND);
            autoPvpStatus = "Attacking " + target.getName().getString();
        } else {
            autoPvpStatus = "Tracking " + target.getName().getString() + " · " + String.format(Locale.ROOT, "%.1fm", distance);
        }
    }

    private static void releaseAutoPvpKeys(MinecraftClient client) {
        if (client.options != null) {
            client.options.forwardKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
        }
    }


    private static void scanLoadedArea(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            scanStatus = "Join a world before scanning";
            return;
        }
        findings.clear();
        Map<Long, Integer> chunkScores = new HashMap<>();
        BlockPos center = client.player.getBlockPos();
        int minY = Math.max(client.world.getBottomY(), center.getY() - 12);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + 12);
        int radius = scanRadius;
        int checked = 0;

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                int cx = x >> 4, cz = z >> 4;
                if (!client.world.isChunkLoaded(cx, cz)) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    Block block = state.getBlock();
                    String type = null;
                    int score = 0;
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST) {
                        type = "Chest"; score = 3;
                    } else if (block == Blocks.BARREL) {
                        type = "Barrel"; score = 3;
                    } else if (block instanceof ShulkerBoxBlock) {
                        type = "Shulker"; score = 4;
                    } else if (block == Blocks.SPAWNER) {
                        type = "Spawner"; score = 5;
                    } else if (block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL) {
                        type = "Portal"; score = 2;
                    } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                            || block == Blocks.ANCIENT_DEBRIS) {
                        type = "Ore hint"; score = 1;
                    }
                    if (type == null && isEnabled("Cave Finder") && y < center.getY() - 3 && state.isAir()) {
                        int solidSides = 0;
                        if (!client.world.getBlockState(pos.north()).isAir()) solidSides++;
                        if (!client.world.getBlockState(pos.south()).isAir()) solidSides++;
                        if (!client.world.getBlockState(pos.east()).isAir()) solidSides++;
                        if (!client.world.getBlockState(pos.west()).isAir()) solidSides++;
                        if (!client.world.getBlockState(pos.down()).isAir() && !client.world.getBlockState(pos.up()).isAir()
                                && solidSides >= 3 && findings.size() < maxFindings) {
                            findings.add(new Finding("Cave pocket (heuristic)", pos.toImmutable(), 1));
                        }
                    }
                    if (type != null) {
                        checked++;
                        boolean show = (type.equals("Chest") && isEnabled("Chest ESP"))
                                || ((type.equals("Barrel")) && isEnabled("Storage ESP"))
                                || (type.equals("Shulker") && isEnabled("Shulker ESP"))
                                || (type.equals("Spawner") && isEnabled("Spawner ESP"))
                                || (type.equals("Portal") && isEnabled("Portal Finder"))
                                || (type.equals("Ore hint") && isEnabled("Ore Hints"));
                        if (show && findings.size() < maxFindings) findings.add(new Finding(type, pos.toImmutable(), score));
                        long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
                        chunkScores.merge(key, score, Integer::sum);
                    }
                }
            }
        }
        if (isEnabled("Sus Chunk Finder")) {
            for (Map.Entry<Long, Integer> entry : chunkScores.entrySet()) {
                if (entry.getValue() >= susChunkThreshold) {
                    int cx = (int) (entry.getKey() >> 32), cz = (int) (long) entry.getKey();
                    findings.add(new Finding("Sus chunk (heuristic " + entry.getValue() + ")",
                            new ChunkPos(cx, cz).getStartPos(), entry.getValue()));
                }
            }
        }
        lastScanTick = client.world.getTime();
        scanStatus = "Scanned loaded area · " + checked + " block clues · " + findings.size() + " markers";
    }
}

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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.Items;

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
    private static int scanRadius = 12;
    private static int themeIndex = 0;
    private static boolean showWatermark = true;
    private static boolean compactLayout = true;

    public static int themeIndex() { return themeIndex; }
    public static void setThemeIndex(int value) { themeIndex = 0; ConfigManager.save(); }
    public static boolean showWatermark() { return showWatermark; }
    public static void setShowWatermark(boolean value) { showWatermark = value; ConfigManager.save(); }
    public static boolean compactLayout() { return compactLayout; }
    public static void setCompactLayout(boolean value) { compactLayout = true; ConfigManager.save(); }
    public static int themeOrange() { return 0xFFFF7818; }
    public static int themeRed() { return 0xFFFFA13B; }
    public static int themePurple() { return 0xFFFFD2A3; }
    public static String themeName() { return "FANTA ORANGE"; }

    private static int scanInterval = 100;
    private static int maxFindings = 80;
    private static int susChunkThreshold = 5;
    private static Double previousGamma = null;
    private static String autoPvpStatus = "Off";
    private static PlayerEntity autoPvpTarget = null;
    private static int autoEatOriginalSlot = -1;
    private static long lastAutoEatTick = -100;
    private static double previousX, previousY, previousZ;
    private static double speedBlocksPerSecond = 0.0;
    private static boolean havePreviousPosition = false;

    public record Finding(String type, BlockPos pos, int score) {}

    public static void registerDefaults() {
        for (String name : List.of(
                "Sus Chunk Finder", "Chest ESP", "Storage ESP", "Shulker ESP", "Spawner ESP",
                "Portal Finder", "Cave Finder", "Chunk Info", "Waypoints", "Entity Radar",
                "Coordinates HUD", "Fullbright", "Toggle Sprint", "Target HUD", "Totem Counter",
                "CPS Counter", "Pearl Alerts", "Combo Trainer", "Crystal Helper", "Auto Eat",
                "Armor Status", "Durability Warnings", "Mob Radar", "Ore Hints", "Logout Spots",
                "Full Auto PvP Bot (Singleplayer)", "Auto Tool", "Auto Eat (Hotbar)",
                "FPS Counter", "Ping Display", "Direction HUD", "Speedometer",
                "Player Radar+", "Mob Radar+", "Base Activity Finder", "Redstone Clue Finder",
                "Workstation Finder", "Buried Storage Finder", "Enemy Gear HUD", "Totem Alert",
                "Low Health Alert", "Crystal Warning", "Fanta Totem",
                "Aim Assist", "TriggerBot", "Auto Shield", "Void Warning", "Fire Resistance Reminder", "HUD Move")) {
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
        tickAimAssist(client);
        tickTriggerBot(client);
        tickAutoShield(client);
        tickAutoTool(client);
        tickAutoEat(client);
        updateSpeed(client);
        boolean wantsScan = isEnabled("Sus Chunk Finder") || isEnabled("Chest ESP") || isEnabled("Storage ESP")
                || isEnabled("Shulker ESP") || isEnabled("Spawner ESP") || isEnabled("Portal Finder")
                || isEnabled("Cave Finder") || isEnabled("Ore Hints")
                || isEnabled("Base Activity Finder") || isEnabled("Redstone Clue Finder")
                || isEnabled("Workstation Finder") || isEnabled("Buried Storage Finder");
        if (wantsScan && client.world.getTime() - lastScanTick >= scanInterval) scanLoadedArea(client);
    }

    public static double speedBlocksPerSecond() { return speedBlocksPerSecond; }

    public static int pingMillis(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return -1;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static void updateSpeed(MinecraftClient client) {
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        if (havePreviousPosition) {
            double dx = x - previousX, dy = y - previousY, dz = z - previousZ;
            speedBlocksPerSecond = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
        }
        previousX = x; previousY = y; previousZ = z;
        havePreviousPosition = true;
    }

    public static void scanNow() { scanLoadedArea(MinecraftClient.getInstance()); }

    /**
     * Client-side combat automation. It can send ordinary movement and attack actions in
     * multiplayer, but server rules and anti-cheat may reject or flag automated behavior.
     */
    private static void tickAutoPvp(MinecraftClient client) {
        boolean enabledNow = isEnabled("Full Auto PvP Bot (Singleplayer)");
        if (!enabledNow) {
            releaseAutoPvpKeys(client);
            autoPvpTarget = null;
            autoPvpStatus = "Off";
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


    /**
     * Gentle aim assistance toward the nearest visible player in a narrow field of view.
     * It only adjusts the local camera; it does not send attacks or bypass server checks.
     */
    private static void tickAimAssist(MinecraftClient client) {
        if (!isEnabled("Aim Assist") || client.currentScreen != null) return;
        PlayerEntity self = client.player;
        PlayerEntity target = null;
        double bestDistance = 6.0 * 6.0;
        float bestAngle = 38.0F;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator()) continue;
            double distance = self.squaredDistanceTo(candidate);
            if (distance >= bestDistance) continue;
            var delta = candidate.getEyePos().subtract(self.getEyePos());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
            float yawDifference = wrapAngle(targetYaw - self.getYaw());
            float targetPitch = (float) -(MathHelper.atan2(delta.y, horizontal) * (180.0 / Math.PI));
            float pitchDifference = targetPitch - self.getPitch();
            float angle = (float) Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference);
            if (angle <= bestAngle) {
                bestAngle = angle;
                bestDistance = distance;
                target = candidate;
            }
        }
        if (target == null) return;
        var delta = target.getEyePos().subtract(self.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) -(MathHelper.atan2(delta.y, horizontal) * (180.0 / Math.PI));
        self.setYaw(self.getYaw() + wrapAngle(targetYaw - self.getYaw()) * 0.18F);
        self.setPitch(self.getPitch() + (targetPitch - self.getPitch()) * 0.18F);
    }

    /** Attacks a player only when they are already under the crosshair and in melee range. */
    private static void tickTriggerBot(MinecraftClient client) {
        if (!isEnabled("TriggerBot") || client.currentScreen != null
                || client.interactionManager == null
                || !(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof PlayerEntity target)) return;
        PlayerEntity self = client.player;
        if (target == self || !target.isAlive() || target.isSpectator()
                || self.squaredDistanceTo(target) > 4.2 * 4.2
                || self.getAttackCooldownProgress(0.5F) < 0.92F) return;
        client.interactionManager.attackEntity(self, target);
        self.swingHand(Hand.MAIN_HAND);
    }


    /** Holds the shield use action while a nearby player is directly under the crosshair. */
    private static void tickAutoShield(MinecraftClient client) {
        if (!isEnabled("Auto Shield")) return;
        boolean shouldBlock = false;
        if (client.currentScreen == null
                && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity target
                && target != client.player && target.isAlive()
                && client.player.squaredDistanceTo(target) <= 5.0 * 5.0) {
            shouldBlock = client.player.getMainHandStack().isOf(Items.SHIELD)
                    || client.player.getOffHandStack().isOf(Items.SHIELD);
        }
        client.options.useKey.setPressed(shouldBlock);
    }

    private static float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    /** Selects the fastest tool in the hotbar for the block under the crosshair. */
    private static void tickAutoTool(MinecraftClient client) {
        if (!isEnabled("Auto Tool") || client.currentScreen != null
                || !(client.crosshairTarget instanceof BlockHitResult hit)) return;
        BlockState state = client.world.getBlockState(hit.getBlockPos());
        int bestSlot = client.player.getInventory().getSelectedSlot();
        float bestSpeed = client.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = client.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        client.player.getInventory().setSelectedSlot(bestSlot);
    }

    /** Eats from the hotbar when hunger is low; restores the previous slot afterwards. */
    private static void tickAutoEat(MinecraftClient client) {
        if (!isEnabled("Auto Eat (Hotbar)") || client.currentScreen != null
                || client.interactionManager == null) {
            if (autoEatOriginalSlot >= 0) {
                client.player.getInventory().setSelectedSlot(autoEatOriginalSlot);
                autoEatOriginalSlot = -1;
            }
            return;
        }
        int food = client.player.getHungerManager().getFoodLevel();
        if (food >= 18) {
            if (autoEatOriginalSlot >= 0) {
                client.player.getInventory().setSelectedSlot(autoEatOriginalSlot);
                autoEatOriginalSlot = -1;
            }
            return;
        }
        int foodSlot = -1;
        for (int slot = 0; slot < 9; slot++) {
            var stack = client.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD)) {
                foodSlot = slot;
                break;
            }
        }
        if (foodSlot < 0) return;
        if (autoEatOriginalSlot < 0) autoEatOriginalSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(foodSlot);
        if (client.world.getTime() - lastAutoEatTick >= 4) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            lastAutoEatTick = client.world.getTime();
        }
    }

    private static void releaseAutoPvpKeys(MinecraftClient client) {
        if (client.options != null) {
            client.options.forwardKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
        }
    }


    private static boolean isRedstoneClue(Block block) {
        return block == Blocks.HOPPER || block == Blocks.DISPENSER || block == Blocks.DROPPER
                || block == Blocks.OBSERVER || block == Blocks.PISTON || block == Blocks.STICKY_PISTON
                || block == Blocks.REPEATER || block == Blocks.COMPARATOR || block == Blocks.REDSTONE_WIRE
                || block == Blocks.LEVER || block == Blocks.REDSTONE_BLOCK;
    }

    private static boolean isWorkstation(Block block) {
        return block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER || block == Blocks.ENCHANTING_TABLE || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL || block == Blocks.BREWING_STAND
                || block == Blocks.CARTOGRAPHY_TABLE || block == Blocks.FLETCHING_TABLE || block == Blocks.SMITHING_TABLE
                || block == Blocks.GRINDSTONE || block == Blocks.STONECUTTER || block == Blocks.LOOM
                || block == Blocks.COMPOSTER || block == Blocks.LECTERN;
    }

    private static boolean isBaseActivityBlock(Block block) {
        return block == Blocks.BELL || block == Blocks.BEEHIVE || block == Blocks.BEE_NEST
                || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE || block == Blocks.BARREL;
    }

    private static void scanLoadedArea(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            scanStatus = "Join a world before scanning";
            return;
        }
        findings.clear();
        Map<Long, Integer> chunkScores = new HashMap<>();
        BlockPos center = client.player.getBlockPos();
        int verticalDown = isEnabled("Base Activity Finder") || isEnabled("Buried Storage Finder") ? 16 : 8;
        int verticalUp = isEnabled("Base Activity Finder") ? 12 : 8;
        int minY = Math.max(client.world.getBottomY(), center.getY() - verticalDown);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + verticalUp);
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
                    } else if (isRedstoneClue(block)) {
                        type = "Redstone clue"; score = 2;
                    } else if (isWorkstation(block)) {
                        type = "Workstation"; score = 2;
                    } else if (isBaseActivityBlock(block)) {
                        type = "Base activity"; score = 1;
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
                                || (type.equals("Ore hint") && isEnabled("Ore Hints"))
                                || (type.equals("Redstone clue") && (isEnabled("Redstone Clue Finder") || isEnabled("Base Activity Finder")))
                                || (type.equals("Workstation") && (isEnabled("Workstation Finder") || isEnabled("Base Activity Finder")))
                                || (type.equals("Base activity") && isEnabled("Base Activity Finder"))
                                || ((type.equals("Chest") || type.equals("Barrel") || type.equals("Shulker")) && isEnabled("Buried Storage Finder"));
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

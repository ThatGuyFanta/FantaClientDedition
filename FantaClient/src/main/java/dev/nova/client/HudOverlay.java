package dev.nova.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight in-world HUD renderer. All enabled HUD modules render outside the click GUI. */
public final class HudOverlay {
    private static int x = 8;
    private static int y = 8;
    private static float scale = 1.0f;
    private HudOverlay() {}

    public static int x() { return x; }
    public static int y() { return y; }
    public static float scale() { return scale; }
    public static void setLayout(int newX, int newY, float newScale) {
        x = Math.max(0, Math.min(2000, newX));
        y = Math.max(0, Math.min(1200, newY));
        scale = Math.max(0.5f, Math.min(2.5f, newScale));
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        List<String> lines = buildLines(client);
        if (lines.isEmpty()) return;
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float)x, (float)y);
        matrices.scale(scale, scale);
        int yy = 0;
        for (String line : lines) {
            int color = line.startsWith("!") ? ModuleManager.themeRed()
                    : line.startsWith("~") ? ModuleManager.themePurple() : ModuleManager.themeOrange();
            String clean = line.substring(1);
            context.fill(-3, yy - 2, client.textRenderer.getWidth(clean) + 4, yy + 10, 0x88090000);
            context.drawTextWithShadow(client.textRenderer, clean, 0, yy, color);
            yy += 12;
        }
        matrices.popMatrix();
    }

    static List<String> buildLines(MinecraftClient client) {
        List<String> lines = new ArrayList<>();
        if (ModuleManager.isEnabled("FPS Counter")) lines.add("F" + "FPS: " + client.getCurrentFps());
        if (ModuleManager.isEnabled("Ping Display")) {
            int ping = ModuleManager.pingMillis(client);
            lines.add("~PING: " + (ping < 0 ? "N/A" : ping + " ms"));
        }
        if (client.player == null || client.world == null) return lines;
        if (ModuleManager.isEnabled("Coordinates HUD")) {
            var p = client.player.getBlockPos();
            lines.add("FXYZ: " + p.getX() + " / " + p.getY() + " / " + p.getZ());
        }
        if (ModuleManager.isEnabled("Direction HUD")) {
            float yaw = client.player.getYaw();
            String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            lines.add("~FACING: " + dirs[Math.floorMod(Math.round(yaw / 45.0f), 8)] + " (" + Math.round(yaw) + "°)");
        }
        if (ModuleManager.isEnabled("Speedometer")) lines.add("~SPEED: " + String.format(Locale.ROOT, "%.2f blocks/s", ModuleManager.speedBlocksPerSecond()));
        if (ModuleManager.isEnabled("Armor Status")) {
            StringBuilder armor = new StringBuilder("ARMOR: ");
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                var stack = client.player.getEquippedStack(slot);
                armor.append(stack.isEmpty() ? "--" : stack.getMaxDamage() - stack.getDamage()).append("  ");
            }
            lines.add("~" + armor);
        }
        if (ModuleManager.isEnabled("Totem Counter")) {
            int count = 0;
            for (int i = 0; i < client.player.getInventory().size(); i++)
                if (client.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) count++;
            lines.add("F TOTEMS: " + count);
        }
        if (ModuleManager.isEnabled("Target HUD") && client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living)
            lines.add("!TARGET: " + living.getName().getString() + "  " + String.format(Locale.ROOT, "%.1f", living.getHealth()) + " HP");
        if (ModuleManager.isEnabled("Totem Alert")) {
            boolean hasTotem = false;
            for (int i = 0; i < client.player.getInventory().size(); i++) if (client.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) { hasTotem = true; break; }
            if (!hasTotem) lines.add("!WARNING: NO TOTEMS");
        }
        if (ModuleManager.isEnabled("Low Health Alert") && client.player.getHealth() <= 10.0f)
            lines.add("!LOW HEALTH: " + String.format(Locale.ROOT, "%.1f HP", client.player.getHealth()));
        if (ModuleManager.isEnabled("Void Warning") && client.player.getY() <= client.world.getBottomY() + 8)
            lines.add("!VOID WARNING");
        if (ModuleManager.isEnabled("Fire Resistance Reminder") && client.world.getRegistryKey().getValue().getPath().equals("the_nether") && !client.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
            lines.add("!NETHER: NO FIRE RESISTANCE");
        if (ModuleManager.isEnabled("Crystal Warning")) {
            int count = client.world.getEntitiesByClass(EndCrystalEntity.class, client.player.getBoundingBox().expand(8), e -> true).size();
            if (count > 0) lines.add("!CRYSTALS NEARBY: " + count);
        }
        if (ModuleManager.isEnabled("Pearl Alerts")) {
            int count = client.world.getEntitiesByClass(EnderPearlEntity.class, client.player.getBoundingBox().expand(32), e -> true).size();
            lines.add("~PEARLS NEARBY: " + count);
        }
        if (ModuleManager.isEnabled("Player Radar+")) {
            int count = 0;
            for (var player : client.world.getPlayers()) if (player != client.player && !player.isSpectator() && player.squaredDistanceTo(client.player) <= 64 * 64) count++;
            lines.add("~PLAYERS NEARBY: " + count);
        }
        if (ModuleManager.isEnabled("Mob Radar+")) {
            int count = 0;
            for (var entity : client.world.getEntities()) if (entity instanceof LivingEntity living && entity != client.player && !living.isDead() && entity.squaredDistanceTo(client.player) <= 32 * 32) count++;
            lines.add("~MOBS NEARBY: " + count);
        }
        boolean showFindings = ModuleManager.isEnabled("Chest ESP") || ModuleManager.isEnabled("Storage ESP")
                || ModuleManager.isEnabled("Shulker ESP") || ModuleManager.isEnabled("Spawner ESP")
                || ModuleManager.isEnabled("Portal Finder") || ModuleManager.isEnabled("Ore Hints")
                || ModuleManager.isEnabled("Redstone Clue Finder") || ModuleManager.isEnabled("Workstation Finder")
                || ModuleManager.isEnabled("Base Activity Finder") || ModuleManager.isEnabled("Buried Storage Finder");
        if (showFindings) {
            int shown = 0;
            for (ModuleManager.Finding finding : ModuleManager.findings()) {
                if (shown++ >= 5) break;
                var pos = finding.pos();
                lines.add("~" + finding.type() + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            }
            if (shown == 0) lines.add("~NO SCAN CLUES YET · PRESS SCAN IN MENU");
        }
        return lines;
    }
}

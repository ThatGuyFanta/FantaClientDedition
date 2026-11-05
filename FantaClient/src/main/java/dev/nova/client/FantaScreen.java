package dev.nova.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

public final class FantaScreen extends Screen {
    private static final int ORANGE = 0xFFFF7818;
    private static final int RED = 0xFFFF3548;
    private static final int PURPLE = 0xFFAD42FF;
    private static final String[][] MODULES = {
            {"Sus Chunk Finder", "Base hunting", "Heuristic: scores loaded chunks with detected storage, portals and spawners"},
            {"Chest ESP", "Base hunting", "Lists chest and trapped chest blocks in loaded nearby chunks"},
            {"Storage ESP", "Base hunting", "Lists barrels and other supported storage blocks nearby"},
            {"Shulker ESP", "Base hunting", "Lists loaded shulker box blocks"},
            {"Spawner ESP", "Base hunting", "Lists loaded mob spawners"},
            {"Portal Finder", "Base hunting", "Lists loaded Nether and End portal blocks"},
            {"Cave Finder", "Base hunting", "Reserved for cave topology analysis"},
            {"Chunk Info", "Utility", "Shows your current chunk coordinates"},
            {"Waypoints", "Utility", "Press G to save your current position"},
            {"Entity Radar", "Utility", "Reserved for nearby entity radar"},
            {"Coordinates HUD", "Utility", "Shows coordinates in this menu"},
            {"Fullbright", "Utility", "Sets client gamma to maximum; toggle off to restore normal gamma"},
            {"Toggle Sprint", "Movement", "Automatically sprints while moving forward"},
            {"Target HUD", "PvP", "Reserved for target information display"},
            {"Totem Counter", "PvP", "Counts totems in your inventory"},
            {"CPS Counter", "PvP", "Reserved for click-rate display"},
            {"Pearl Alerts", "PvP", "Reserved for pearl activity alerts"},
            {"Combo Trainer", "PvP", "Practice feedback only; no automated attacks"},
            {"Crystal Helper", "PvP", "Practice/planning helper; no automatic combat"},
            {"Auto Eat", "Utility", "Reserved for safe food reminder"},
            {"Armor Status", "Utility", "Shows equipped armor durability"},
            {"Durability Warnings", "Utility", "Reserved for low-durability alerts"},
            {"Mob Radar", "Utility", "Reserved for nearby mob list"},
            {"Ore Hints", "Exploration", "Lists loaded diamond ore and ancient debris blocks"},
            {"Logout Spots", "Utility", "Reserved for manually saved player locations"},
            {"Full Auto PvP Bot (Singleplayer)", "PvP", "Autonomous target, aim, approach and melee attack; restricted to integrated singleplayer worlds"}
    };

    private int left, top;
    private String bindingModule = null;

    public FantaScreen() { super(Text.literal("FantaClient - Demon Edition")); }

    @Override
    protected void init() {
        clearChildren();
        left = (width - 540) / 2;
        top = Math.max(8, (height - 480) / 2);
        for (int i = 0; i < MODULES.length; i++) {
            final String name = MODULES[i][0];
            int col = i % 3, row = i / 3;
            int x = left + 10 + col * 174;
            int y = top + 49 + row * 24;
            boolean on = ModuleManager.isEnabled(name);
            addDrawableChild(ButtonWidget.builder(label(name, on), b -> {
                boolean next = !ModuleManager.isEnabled(name);
                ModuleManager.setEnabled(name, next);
                b.setMessage(label(name, next));
            }).dimensions(x, y, 168, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("SCAN"), b -> ModuleManager.scanNow())
                .dimensions(left + 10, top + 268, 126, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("WAYPOINT"), b -> ModuleManager.addWaypoint())
                .dimensions(left + 142, top + 268, 126, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SETTINGS / CONFIG"), b -> client.setScreen(new SettingsScreen(this)))
                .dimensions(left + 274, top + 268, 126, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("CLOSE"), b -> close())
                .dimensions(left + 406, top + 268, 124, 20).build());
    }

    private Text label(String name, boolean on) {
        int key = ModuleManager.hotkey(name);
        String keyLabel = key < 0 ? "" : " §6[" + keyName(key) + "]";
        return Text.literal((on ? "§a[ON] §f" : "§8[OFF] §f") + name + keyLabel);
    }

    private String keyName(int key) {
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(java.util.Locale.ROOT);
        return switch (key) { case 344 -> "RSHIFT"; case 256 -> "ESC"; case 32 -> "SPACE"; default -> "KEY " + key; };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 1) {
            String[] module = hoveredModule((int) click.x(), (int) click.y());
            if (module != null) {
                bindingModule = module[0];
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (bindingModule != null) {
            int keyCode = input.key();
            if (keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                ModuleManager.setHotkey(bindingModule, keyCode);
                ConfigManager.save();
            }
            bindingModule = null;
            clearAndInit();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(left, top, left + 540, top + 480, 0xF014101D);
        context.fill(left, top, left + 540, top + 3, ORANGE);
        context.fill(left, top + 3, left + 180, top + 5, RED);
        context.fill(left + 180, top + 3, left + 360, top + 5, PURPLE);
        context.fill(left + 360, top + 3, left + 540, top + 5, ORANGE);
        context.drawTextWithShadow(textRenderer, "FantaClient", left + 12, top + 11, ORANGE);
        context.drawTextWithShadow(textRenderer, "DEMON EDITION  //  FABRIC 1.21.11", left + 12, top + 25, PURPLE);
        context.drawTextWithShadow(textRenderer, "BASE HUNTING  /  UTILITY  /  PVP", left + 340, top + 15, 0xFFFFA5A5);
        context.drawTextWithShadow(textRenderer, bindingModule == null ? "MODULES · RIGHT-CLICK A TILE TO REBIND" : "PRESS A KEY FOR " + bindingModule,
                left + 12, top + 39, 0xFFFFA45A);
        super.render(context, mouseX, mouseY, delta);

        int infoY = top + 298;
        context.drawTextWithShadow(textRenderer, "SCANNER", left + 12, infoY, ORANGE);
        if (ModuleManager.isEnabled("Full Auto PvP Bot (Singleplayer)")) {
            context.drawTextWithShadow(textRenderer, "AUTO PVP: " + ModuleManager.autoPvpStatus(), left + 12, infoY + 15, RED);
        }
        context.drawTextWithShadow(textRenderer, ModuleManager.scanStatus(), left + 80, infoY, 0xFFE6D9EE);
        if (client != null && client.player != null) {
            BlockPos p = client.player.getBlockPos();
            context.drawTextWithShadow(textRenderer, "XYZ: " + p.getX() + " / " + p.getY() + " / " + p.getZ()
                    + "    CHUNK: " + (p.getX() >> 4) + " / " + (p.getZ() >> 4), left + 12, infoY + 15, 0xFFFFB46D);
            int totems = 0;
            for (int i = 0; i < client.player.getInventory().size(); i++) {
                if (client.player.getInventory().getStack(i).isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) totems++;
            }
            if (ModuleManager.isEnabled("Totem Counter"))
                context.drawTextWithShadow(textRenderer, "TOTEMS: " + totems, left + 12, infoY + 30, PURPLE);
            if (ModuleManager.isEnabled("Armor Status")) {
                StringBuilder armor = new StringBuilder("ARMOR: ");
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    var stack = client.player.getEquippedStack(slot);
                    armor.append(stack.isEmpty() ? "--" : (stack.getMaxDamage() - stack.getDamage())).append("  ");
                }
                context.drawTextWithShadow(textRenderer, armor.toString(), left + 130, infoY + 30, 0xFFE6D9EE);
            }
        }
        List<ModuleManager.Finding> found = ModuleManager.findings();
        context.drawTextWithShadow(textRenderer, "NEARBY CLUES (loaded chunks only)", left + 12, infoY + 47, PURPLE);
        int y = infoY + 61;
        int shown = Math.min(5, found.size());
        for (int i = 0; i < shown; i++) {
            ModuleManager.Finding f = found.get(i);
            BlockPos p = f.pos();
            context.drawTextWithShadow(textRenderer, f.type() + "  @  " + p.getX() + ", " + p.getY() + ", " + p.getZ(),
                    left + 12, y, 0xFFE8E0ED);
            y += 12;
        }
        if (found.isEmpty()) context.drawTextWithShadow(textRenderer, "No clues yet. Enable a scanner and press SCAN LOADED AREA.", left + 12, y, 0xFFB9A9C4);
        if (client != null && client.player != null && client.world != null) {
            int extraY = top + 425;
            if (ModuleManager.isEnabled("Waypoints") && !ModuleManager.waypoints().isEmpty()) {
                BlockPos wp = ModuleManager.waypoints().get(ModuleManager.waypoints().size() - 1);
                context.drawTextWithShadow(textRenderer, "LAST WAYPOINT: " + wp.getX() + ", " + wp.getY() + ", " + wp.getZ(),
                        left + 12, extraY, ORANGE);
            }
            if (ModuleManager.isEnabled("Entity Radar")) {
                StringBuilder radar = new StringBuilder("NEARBY PLAYERS: ");
                int count = 0;
                for (var player : client.world.getPlayers()) {
                    if (player == client.player || player.squaredDistanceTo(client.player) > 48 * 48) continue;
                    if (count++ > 0) radar.append(", ");
                    radar.append(player.getName().getString()).append(" (").append((int) Math.sqrt(player.squaredDistanceTo(client.player))).append("m)");
                    if (count == 3) break;
                }
                if (count == 0) radar.append("none in 48 blocks");
                context.drawTextWithShadow(textRenderer, radar.toString(), left + 12, extraY + 12, PURPLE);
            }
            if (ModuleManager.isEnabled("Target HUD") && client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity living) {
                context.drawTextWithShadow(textRenderer, "TARGET: " + living.getName().getString() + "  HP: "
                        + String.format(java.util.Locale.ROOT, "%.1f", living.getHealth()) + "/"
                        + String.format(java.util.Locale.ROOT, "%.1f", living.getMaxHealth()), left + 12, extraY + 24, RED);
            }
            if (ModuleManager.isEnabled("Pearl Alerts")) {
                int pearls = client.world.getEntitiesByClass(EnderPearlEntity.class,
                        client.player.getBoundingBox().expand(32), pearl -> true).size();
                context.drawTextWithShadow(textRenderer, "ENDER PEARLS NEARBY: " + pearls, left + 12, extraY + 36, PURPLE);
            }
            if (ModuleManager.isEnabled("Durability Warnings")) {
                for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
                    var stack = client.player.getInventory().getStack(slot);
                    if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 20) {
                        context.drawTextWithShadow(textRenderer, "LOW DURABILITY: " + stack.getName().getString(), left + 12, extraY + 48, RED);
                        break;
                    }
                }
            }
        }
        if (hoveredModule(mouseX, mouseY) != null) {
            String[] info = hoveredModule(mouseX, mouseY);
            context.drawTooltip(textRenderer, Text.literal(info[2]), mouseX, mouseY);
        }
    }

    private String[] hoveredModule(int mouseX, int mouseY) {
        for (int i = 0; i < MODULES.length; i++) {
            int x = left + 10 + (i % 3) * 174;
            int y = top + 49 + (i / 3) * 24;
            if (mouseX >= x && mouseX <= x + 168 && mouseY >= y && mouseY <= y + 20) return MODULES[i];
        }
        return null;
    }

    @Override public boolean shouldPause() { return false; }
}

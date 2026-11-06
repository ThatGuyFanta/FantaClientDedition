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
            {"Full Auto PvP Bot (Singleplayer)", "PvP", "Automated target, aim, approach and melee attack; servers may block or flag it"},
            {"Auto Tool", "Utility", "Multiplayer-compatible: selects the fastest hotbar tool for the block under your crosshair"},
            {"Auto Eat (Hotbar)", "Utility", "Uses hotbar food below 18 hunger and restores your previous slot"},
            {"FPS Counter", "Visuals", "Shows the current client frame rate"},
            {"Ping Display", "Visuals", "Shows your measured connection latency when available"},
            {"Direction HUD", "Visuals", "Shows the direction your player is facing"},
            {"Speedometer", "Visuals", "Shows approximate movement speed in blocks per second"},
            {"Player Radar+", "Visuals", "Lists nearby player names and distances from entities currently tracked by your client"},
            {"Mob Radar+", "Visuals", "Lists nearby living mobs and distances; only entities currently known to your client"},
            {"Base Activity Finder", "Base hunting", "Finds loaded-chunk clues: workstations, redstone devices, bells, bee blocks and campfires"},
            {"Redstone Clue Finder", "Base hunting", "Finds loaded hoppers, observers, pistons, repeaters and other redstone components"},
            {"Workstation Finder", "Base hunting", "Finds loaded crafting, smelting, enchanting and villager workstation blocks"},
            {"Buried Storage Finder", "Base hunting", "Extends the scan downward to find loaded chests, barrels and shulkers"},
            {"Enemy Gear HUD", "PvP", "Shows the armor and held item of the entity you're aiming at"},
            {"Totem Alert", "PvP", "Warns when your inventory has no totem of undying"},
            {"Low Health Alert", "PvP", "Displays a warning when your health drops below five hearts"},
            {"Crystal Warning", "PvP", "Warns when end crystals are nearby; it does not attack or place crystals"},
            {"Aim Assist", "PvP", "Gently adjusts aim toward nearby players already within your field of view"},
            {"TriggerBot", "PvP", "Attacks a player already under your crosshair when melee cooldown is ready"},
            {"Auto Shield", "PvP", "Raises a shield when a nearby player is directly under your crosshair"},
            {"Void Warning", "Survival", "Warns when you are close to the bottom of the dimension"},
            {"Fire Resistance Reminder", "Survival", "Reminds you when you are in the Nether without fire resistance"},
            {"Fanta Totem", "Cosmetics", "Enable the included Fanta Totem resource pack in Options > Resource Packs to replace the vanilla totem texture with a Fanta bottle"},
            {"HUD Move", "Visuals", "Drag the HUD cluster and use the mouse wheel to resize it"}
    };

    private int left, top;
    private String bindingModule = null;
    private String activeCategory = "Modules";
    private int moduleScroll = 0;
    private boolean searchFocused = false;
    private String searchQuery = "";
    private static final String[] CATEGORIES = {"Modules", "HUD", "Base Hunt", "Combat", "Utility"};

    public FantaScreen() { super(Text.literal("FantaClient - Demon Edition")); }

    @Override
    protected void init() {
        clearChildren();
        left = Math.max(8, (width - 660) / 2);
        top = Math.max(8, (height - 430) / 2);
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
        int mx = (int) click.x();
        int my = (int) click.y();
        if (click.button() == 0) {
            if (inside(mx, my, left + 448, top + 72, 196, 27)) {
                searchFocused = true;
                return true;
            }
            searchFocused = false;
            // Sidebar navigation.
            for (int i = 0; i < CATEGORIES.length; i++) {
                int y = top + 94 + i * 43;
                if (inside(mx, my, left + 12, y, 122, 34)) {
                    activeCategory = CATEGORIES[i];
                    moduleScroll = 0;
                    return true;
                }
            }
            // Module cards toggle on left click.
            String[] module = hoveredModule(mx, my);
            if (module != null) {
                ModuleManager.setEnabled(module[0], !ModuleManager.isEnabled(module[0]));
                if (module[0].equals("HUD Move") && ModuleManager.isEnabled("HUD Move"))
                    client.setScreen(new HudMoveScreen());
                return true;
            }
            // Bottom action strip.
            int by = top + 382;
            if (inside(mx, my, left + 154, by, 112, 28)) { ModuleManager.scanNow(); return true; }
            if (inside(mx, my, left + 274, by, 112, 28)) { ModuleManager.addWaypoint(); return true; }
            if (inside(mx, my, left + 394, by, 132, 28)) { client.setScreen(new SettingsScreen(this)); return true; }
            if (inside(mx, my, left + 12, by, 122, 28)) { close(); return true; }
        }
        if (click.button() == 1) {
            String[] module = hoveredModule(mx, my);
            if (module != null) {
                client.setScreen(new ModuleSettingsScreen(this, module[0]));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private List<String[]> visibleModules() {
        List<String[]> result = new ArrayList<>();
        for (String[] module : MODULES) {
            String category = module[1];
            boolean include = switch (activeCategory) {
                case "HUD" -> category.equals("Visuals") || module[0].contains("HUD") || module[0].equals("Armor Status") || module[0].equals("Totem Counter");
                case "Base Hunt" -> category.equals("Base hunting") || category.equals("Exploration");
                case "Combat" -> category.equals("PvP");
                case "Utility" -> !category.equals("PvP") && !category.equals("Base hunting") && !category.equals("Exploration") && !category.equals("Visuals") && !module[0].contains("HUD");
                default -> true;
            };
            if (include && (searchQuery.isBlank() || module[0].toLowerCase(java.util.Locale.ROOT).contains(searchQuery.toLowerCase(java.util.Locale.ROOT))))
                result.add(module);
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, visibleModules().size() - 14);
        moduleScroll = Math.max(0, Math.min(max, moduleScroll + (verticalAmount < 0 ? 2 : -2)));
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (searchFocused) {
            int key = input.key();
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                moduleScroll = 0;
                return true;
            }
            String typed = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) typed = " ";
            if (typed != null && typed.length() == 1 && (Character.isLetterOrDigit(typed.charAt(0)) || " -_+".contains(typed))) {
                searchQuery += typed.toLowerCase(java.util.Locale.ROOT);
                moduleScroll = 0;
                return true;
            }
        }
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
        // Keep the Minecraft world visible behind the dashboard; no expensive blur pass.
        context.fill(0, 0, width, height, 0x88060409);

        // Outer frame and main panel.
        context.fill(left - 2, top - 2, left + 662, top + 432, 0xEE050407);
        context.fill(left, top, left + 660, top + 430, 0xF0120E16);
        context.fill(left, top, left + 660, top + 3, ModuleManager.themeOrange());
        context.fill(left + 1, top + 3, left + 659, top + 5, 0xFF60300F);

        // Header with Fanta branding.
        context.fill(left + 14, top + 13, left + 54, top + 53, 0xFF2A1710);
        context.fill(left + 14, top + 13, left + 18, top + 53, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "◉", left + 23, top + 23, 0xFFFF8A24);
        context.drawTextWithShadow(textRenderer, "FantaClient", left + 66, top + 13, 0xFFFF8A24);
        context.drawTextWithShadow(textRenderer, "DEMON  •  1.21.11", left + 67, top + 31, 0xFFFFB46D);
        context.drawTextWithShadow(textRenderer, "CLIENT DASHBOARD", left + 485, top + 27, 0xFFBDAEBF);
        context.fill(left + 12, top + 64, left + 648, top + 65, 0xFF493044);

        // Sidebar.
        context.fill(left + 8, top + 72, left + 142, top + 418, 0xFF100C14);
        context.drawTextWithShadow(textRenderer, "NAVIGATION", left + 20, top + 80, 0xFFB8A7B8);
        String[] icons = {"▦", "⌗", "◎", "⚔", "⚙"};
        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = top + 94 + i * 43;
            boolean selected = activeCategory.equals(CATEGORIES[i]);
            if (selected) {
                context.fill(left + 12, y, left + 134, y + 34, 0xFFB95708);
                context.fill(left + 12, y, left + 15, y + 34, ModuleManager.themeOrange());
            } else if (inside(mouseX, mouseY, left + 12, y, 122, 34)) {
                context.fill(left + 12, y, left + 134, y + 34, 0xFF2B202D);
            }
            context.drawTextWithShadow(textRenderer, icons[i], left + 22, y + 11, selected ? 0xFFFFFFFF : 0xFFFFA044);
            context.drawTextWithShadow(textRenderer, CATEGORIES[i], left + 43, y + 11, selected ? 0xFFFFFFFF : 0xFFBEB5C2);
        }
        context.fill(left + 18, top + 335, left + 132, top + 336, 0xFF493044);
        context.drawTextWithShadow(textRenderer, "♛  FantaClient", left + 22, top + 352, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "ORANGE EDITION", left + 22, top + 369, 0xFF9C8B9F);

        // Main content title and search-style header.
        context.drawTextWithShadow(textRenderer, activeCategory, left + 158, top + 78, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "MODULES  /  RIGHT-CLICK FOR SETTINGS", left + 158, top + 94, 0xFFB5A6B7);
        context.fill(left + 448, top + 72, left + 644, top + 99, 0xFF211B26);
        context.drawTextWithShadow(textRenderer, searchQuery.isEmpty() ? "⌕  Search modules..." : "⌕  " + searchQuery + (searchFocused ? "_" : ""),
                left + 459, top + 81, searchQuery.isEmpty() ? 0xFF8F8494 : 0xFFF1EAF3);

        // Two-column module cards. Scroll with the mouse wheel when a category has more than 14 entries.
        List<String[]> shown = visibleModules();
        int count = Math.min(14, Math.max(0, shown.size() - moduleScroll));
        for (int i = 0; i < count; i++) {
            String[] module = shown.get(i + moduleScroll);
            int col = i % 2;
            int row = i / 2;
            int x = left + 156 + col * 244;
            int y = top + 108 + row * 36;
            boolean enabled = ModuleManager.isEnabled(module[0]);
            boolean hover = inside(mouseX, mouseY, x, y, 234, 30);
            context.fill(x, y, x + 234, y + 30, hover ? 0xFF302632 : 0xFF211B25);
            context.fill(x, y, x + 2, y + 30, enabled ? ModuleManager.themeOrange() : 0xFF514653);
            String shortName = module[0].length() > 25 ? module[0].substring(0, 23) + ".." : module[0];
            context.drawTextWithShadow(textRenderer, shortName, x + 11, y + 10, 0xFFF1EAF3);
            // Orange pill for enabled, muted pill for disabled.
            context.fill(x + 190, y + 6, x + 225, y + 24, enabled ? 0xFFE8750B : 0xFF514B55);
            context.fill(enabled ? x + 208 : x + 192, y + 8, enabled ? x + 223 : x + 207, y + 22, 0xFFEFE8F1);
        }
        if (shown.isEmpty()) context.drawTextWithShadow(textRenderer, "No modules in this section", left + 158, top + 130, 0xFFBBAEBE);
        if (shown.size() > 14) {
            context.drawTextWithShadow(textRenderer, "Scroll to see more  •  " + (moduleScroll + 1) + "-" + (moduleScroll + count) + " / " + shown.size(),
                    left + 158, top + 365, 0xFF9F91A2);
        }

        // Bottom action bar.
        int by = top + 382;
        context.fill(left + 148, top + 374, left + 648, top + 414, 0xFF1B141F);
        drawAction(context, left + 154, by, 112, "SCAN", mouseX, mouseY);
        drawAction(context, left + 274, by, 112, "WAYPOINT", mouseX, mouseY);
        drawAction(context, left + 394, by, 132, "SETTINGS", mouseX, mouseY);
        drawAction(context, left + 12, by, 122, "CLOSE", mouseX, mouseY);

        // Small live status strip, kept compact like the reference design.
        if (client != null && client.player != null) {
            BlockPos pos = client.player.getBlockPos();
            context.drawTextWithShadow(textRenderer, "XYZ  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ(),
                    left + 158, top + 418, 0xFFFFB46D);
            context.drawTextWithShadow(textRenderer, "FPS  " + client.getCurrentFps(), left + 390, top + 418, ModuleManager.themeOrange());
        } else {
            context.drawTextWithShadow(textRenderer, "FANTA ORANGE  •  FABRIC CLIENT", left + 158, top + 418, 0xFFFFB46D);
        }
    }

    private void drawAction(DrawContext context, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 28);
        context.fill(x, y, x + w, y + 28, hover ? 0xFFB95708 : 0xFF30202A);
        context.fill(x, y, x + w, y + 1, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 10, 0xFFFFFFFF);
    }

    private String[] hoveredModule(int mouseX, int mouseY) {
        List<String[]> shown = visibleModules();
        int count = Math.min(14, Math.max(0, shown.size() - moduleScroll));
        for (int i = 0; i < count; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = left + 156 + col * 244;
            int y = top + 108 + row * 36;
            if (inside(mouseX, mouseY, x, y, 234, 30)) return shown.get(i + moduleScroll);
        }
        return null;
    }

    @Override public boolean shouldPause() { return false; }
}

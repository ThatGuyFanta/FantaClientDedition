package dev.nova.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private String binding = null;
    private String status = "Right-click a module in the main menu to open its settings";
    private int left, top;

    public SettingsScreen(Screen parent) { super(Text.literal("FantaClient Settings")); this.parent = parent; }

    @Override protected void init() {
        clearChildren(); left = (width - 420) / 2; top = (height - 390) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("SCAN RADIUS: " + ModuleManager.scanRadius()), b -> {
            int next = ModuleManager.scanRadius() >= 32 ? 8 : ModuleManager.scanRadius() + 8;
            ModuleManager.setScanRadius(next); b.setMessage(Text.literal("SCAN RADIUS: " + next));
        }).dimensions(left + 12, top + 48, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SCAN SPEED: " + ModuleManager.scanInterval() + " ticks"), b -> {
            int next = ModuleManager.scanInterval() <= 20 ? 40 : ModuleManager.scanInterval() <= 40 ? 100 : 20;
            ModuleManager.setScanInterval(next); b.setMessage(Text.literal("SCAN SPEED: " + next + " ticks"));
        }).dimensions(left + 218, top + 48, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("MAX MARKERS: " + ModuleManager.maxFindings()), b -> {
            int next = ModuleManager.maxFindings() >= 500 ? 50 : ModuleManager.maxFindings() + 50;
            ModuleManager.setMaxFindings(next); b.setMessage(Text.literal("MAX MARKERS: " + next));
        }).dimensions(left + 12, top + 78, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BIND MENU KEY"), b -> startBinding("Open Menu"))
                .dimensions(left + 218, top + 78, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SUS SCORE: " + ModuleManager.susChunkThreshold()), b -> {
            int next = ModuleManager.susChunkThreshold() >= 20 ? 2 : ModuleManager.susChunkThreshold() + 2;
            ModuleManager.setSusChunkThreshold(next); b.setMessage(Text.literal("SUS SCORE: " + next));
        }).dimensions(left + 12, top + 108, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BIND WAYPOINT KEY"), b -> startBinding("Add Waypoint"))
                .dimensions(left + 12, top + 138, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("EXPORT CONFIG (COPY)"), b -> {
            client.keyboard.setClipboard(ConfigManager.exportJson()); status = "Config JSON copied to clipboard";
        }).dimensions(left + 218, top + 108, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("IMPORT CONFIG (PASTE)"), b -> {
            boolean ok = ConfigManager.importJson(client.keyboard.getClipboard());
            status = ok ? "Config imported and saved" : "Clipboard does not contain a valid FantaClient config";
            init();
        }).dimensions(left + 12, top + 168, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SAVE CONFIG"), b -> { ConfigManager.save(); status = "Config saved locally"; })
                .dimensions(left + 218, top + 168, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("WATERMARK: " + (ModuleManager.showWatermark() ? "ON" : "OFF")), b -> {
            ModuleManager.setShowWatermark(!ModuleManager.showWatermark());
            b.setMessage(Text.literal("WATERMARK: " + (ModuleManager.showWatermark() ? "ON" : "OFF")));
        }).dimensions(left + 218, top + 198, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BACK"), b -> client.setScreen(parent))
                .dimensions(left + 12, top + 340, 396, 22).build());
    }

    private void startBinding(String action) { binding = action; status = "Press any key to bind " + action + " (ESC cancels)"; }

    @Override public boolean keyPressed(KeyInput input) {
        if (binding != null) {
            int keyCode = input.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) status = "Binding cancelled";
            else { ModuleManager.setHotkey(binding, keyCode); ConfigManager.save(); status = binding + " bound to key code " + keyCode; }
            binding = null; return true;
        }
        return super.keyPressed(input);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Matching dark dashboard styling; no blur is requested.
        context.fill(0, 0, width, height, 0xFF08060B);
        context.fill(left - 2, top - 2, left + 422, top + 392, 0xFF050407);
        context.fill(left, top, left + 420, top + 390, 0xFF120D18);
        context.fill(left, top, left + 420, top + 3, ModuleManager.themeOrange());
        context.fill(left + 10, top + 10, left + 410, top + 40, 0xFF241522);
        context.fill(left + 10, top + 10, left + 13, top + 40, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "FantaClient  /  SETTINGS", left + 21, top + 15, 0xFFFF8A24);
        context.drawTextWithShadow(textRenderer, "FANTA ORANGE  •  COMPACT LAYOUT  •  CONFIG SHARING", left + 12, top + 30, 0xFFBBA9C7);
        context.fill(left + 10, top + 45, left + 410, top + 238, 0xFF1A1321);
        context.fill(left + 10, top + 250, left + 410, top + 330, 0xFF1A1321);
        context.drawTextWithShadow(textRenderer, "COSMETICS LOCKED: FANTA ORANGE + COMPACT", left + 12, top + 258, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "Fanta Totem texture: enable the included resource pack", left + 12, top + 274, ModuleManager.themePurple());
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, status, left + 12, top + 286, 0xFFFFB46D);
        context.drawTextWithShadow(textRenderer, "Profiles are portable JSON: copy, send, then paste to import.", left + 12, top + 306, 0xFFE6D9EE);
        context.drawTextWithShadow(textRenderer, "Module hotkeys: right-click a module tile in the main menu.", left + 12, top + 322, 0xFFE6D9EE);
    }
    @Override public boolean shouldPause() { return false; }
}

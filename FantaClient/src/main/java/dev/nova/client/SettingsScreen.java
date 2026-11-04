package dev.nova.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class SettingsScreen extends Screen {
    private static final int ORANGE = 0xFFFF7818, RED = 0xFFFF3548, PURPLE = 0xFFAD42FF;
    private final Screen parent;
    private String binding = null;
    private String status = "Right-click a module in the main menu to bind its hotkey";
    private int left, top;

    public SettingsScreen(Screen parent) { super(Text.literal("FantaClient Settings")); this.parent = parent; }

    @Override protected void init() {
        clearChildren(); left = (width - 420) / 2; top = (height - 330) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("SCAN RADIUS: " + ModuleManager.scanRadius()), b -> {
            int next = ModuleManager.scanRadius() >= 48 ? 8 : ModuleManager.scanRadius() + 8;
            ModuleManager.setScanRadius(next); b.setMessage(Text.literal("SCAN RADIUS: " + next));
        }).dimensions(left + 12, top + 48, 190, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SCAN SPEED: " + ModuleManager.scanInterval() + " ticks"), b -> {
            int next = ModuleManager.scanInterval() <= 10 ? 40 : ModuleManager.scanInterval() <= 40 ? 100 : 10;
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
        addDrawableChild(ButtonWidget.builder(Text.literal("BACK"), b -> client.setScreen(parent))
                .dimensions(left + 12, top + 270, 396, 22).build());
    }

    private void startBinding(String action) { binding = action; status = "Press any key to bind " + action + " (ESC cancels)"; }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) status = "Binding cancelled";
            else { ModuleManager.setHotkey(binding, keyCode); ConfigManager.save(); status = binding + " bound to key code " + keyCode; }
            binding = null; return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(left, top, left + 420, top + 330, 0xF014101D);
        context.fill(left, top, left + 420, top + 3, ORANGE);
        context.fill(left, top + 3, left + 210, top + 5, RED);
        context.fill(left + 210, top + 3, left + 420, top + 5, PURPLE);
        context.drawTextWithShadow(textRenderer, "FantaClient // DEMON SETTINGS", left + 12, top + 15, ORANGE);
        context.drawTextWithShadow(textRenderer, "MODULE SETTINGS · HOTKEYS · CONFIG SHARING", left + 12, top + 30, PURPLE);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, status, left + 12, top + 220, 0xFFFFB46D);
        context.drawTextWithShadow(textRenderer, "Profiles are portable JSON: copy, send, then paste to import.", left + 12, top + 238, 0xFFE6D9EE);
        context.drawTextWithShadow(textRenderer, "Module hotkeys: right-click a module tile in the main menu.", left + 12, top + 252, 0xFFE6D9EE);
    }
    @Override public boolean shouldPause() { return false; }
}

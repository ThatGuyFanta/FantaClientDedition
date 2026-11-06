package dev.nova.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Per-module settings page opened with right-click in the module list. */
public final class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final String module;
    private boolean binding;
    private String status = "Configure this module";
    private int left, top;
    public ModuleSettingsScreen(Screen parent, String module) {
        super(Text.literal("FantaClient - " + module));
        this.parent = parent;
        this.module = module;
    }
    @Override protected void init() {
        clearChildren(); left = (width - 360) / 2; top = (height - 230) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("MODULE: " + (ModuleManager.isEnabled(module) ? "ON" : "OFF")), b -> {
            ModuleManager.setEnabled(module, !ModuleManager.isEnabled(module));
            b.setMessage(Text.literal("MODULE: " + (ModuleManager.isEnabled(module) ? "ON" : "OFF")));
        }).dimensions(left + 12, top + 48, 336, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BIND KEY"), b -> { binding = true; status = "Press a key (ESC cancels)"; })
                .dimensions(left + 12, top + 78, 160, 22).build());
        if (isHudModule(module)) addDrawableChild(ButtonWidget.builder(Text.literal("OPEN HUD MOVER"), b -> {
            ModuleManager.setEnabled("HUD Move", true);
            client.setScreen(new HudMoveScreen());
        }).dimensions(left + 188, top + 78, 160, 22).build());
        if (isScanner(module)) {
            addDrawableChild(ButtonWidget.builder(Text.literal("SCAN RADIUS: " + ModuleManager.scanRadius()), b -> {
                int next = ModuleManager.scanRadius() >= 32 ? 8 : ModuleManager.scanRadius() + 8;
                ModuleManager.setScanRadius(next); b.setMessage(Text.literal("SCAN RADIUS: " + next));
            }).dimensions(left + 12, top + 108, 160, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("SCAN SPEED: " + ModuleManager.scanInterval()), b -> {
                int next = ModuleManager.scanInterval() <= 20 ? 40 : ModuleManager.scanInterval() <= 40 ? 100 : 20;
                ModuleManager.setScanInterval(next); b.setMessage(Text.literal("SCAN SPEED: " + next));
            }).dimensions(left + 188, top + 108, 160, 22).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("BACK"), b -> client.setScreen(parent))
                .dimensions(left + 12, top + 180, 336, 22).build());
    }
    private static boolean isHudModule(String name) {
        return name.contains("HUD") || name.equals("FPS Counter") || name.equals("Ping Display") || name.equals("Direction HUD") || name.equals("Speedometer") || name.equals("Armor Status") || name.equals("Totem Counter") || name.equals("Player Radar+") || name.equals("Mob Radar+");
    }
    private static boolean isScanner(String name) {
        return name.contains("Finder") || name.contains("ESP") || name.contains("Chunk") || name.equals("Portal Finder") || name.equals("Ore Hints");
    }
    @Override public boolean keyPressed(KeyInput input) {
        if (binding) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) ModuleManager.setHotkey(module, input.key());
            binding = false; status = "Key binding saved"; return true;
        }
        return super.keyPressed(input);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF09060C);
        context.fill(left, top, left + 360, top + 230, 0xF014101D);
        context.fill(left, top, left + 360, top + 3, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "MODULE SETTINGS", left + 12, top + 14, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, module, left + 12, top + 30, 0xFFFFD2A3);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, binding ? "PRESS A KEY · ESC TO CANCEL" : status, left + 12, top + 158, 0xFFFFB46D);
    }
    @Override public boolean shouldPause() { return false; }
}

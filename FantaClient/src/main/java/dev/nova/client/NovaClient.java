package dev.nova.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class NovaClient implements ClientModInitializer {
    private static final Set<Integer> held = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.registerDefaults();
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> HudOverlay.render(drawContext));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pollHotkeys(client);
            ModuleManager.tick(client);
        });
    }

    private static void pollHotkeys(MinecraftClient client) {
        if (client.getWindow() == null) return;
        long window = client.getWindow().getHandle();
        for (var entry : ModuleManager.hotkeys().entrySet()) {
            int key = entry.getValue();
            if (key < 0) continue;
            boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            boolean wasDown = held.contains(key);
            if (down && !wasDown) onHotkey(client, entry.getKey());
            if (down) held.add(key); else held.remove(key);
        }
    }

    private static void onHotkey(MinecraftClient client, String action) {
        if (action.equals("Open Menu")) {
            if (client.currentScreen == null) client.setScreen(new FantaScreen());
            return;
        }
        if (action.equals("Add Waypoint")) { ModuleManager.addWaypoint(); return; }
        if (client.currentScreen != null) return;
        if (ModuleManager.states().containsKey(action)) ModuleManager.setEnabled(action, !ModuleManager.isEnabled(action));
    }
}

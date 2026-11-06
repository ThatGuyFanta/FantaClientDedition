package dev.nova.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Local profiles and portable JSON config sharing. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("fantaclient");
    private static final Path FILE = DIR.resolve("config.json");
    private ConfigManager() {}

    public static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(toJson()), StandardCharsets.UTF_8);
        } catch (IOException e) { System.err.println("[FantaClient] Could not save config: " + e.getMessage()); }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try { fromJson(GSON.fromJson(Files.readString(FILE), JsonObject.class)); }
        catch (Exception e) { System.err.println("[FantaClient] Could not load config: " + e.getMessage()); }
    }

    public static String exportJson() { return GSON.toJson(toJson()); }

    public static boolean importJson(String json) {
        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            if (object == null || !object.has("modules")) return false;
            fromJson(object);
            save();
            return true;
        } catch (Exception e) { return false; }
    }

    private static JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Map.Entry<String, Boolean> entry : ModuleManager.states().entrySet()) modules.addProperty(entry.getKey(), entry.getValue());
        root.add("modules", modules);
        JsonObject keys = new JsonObject();
        for (Map.Entry<String, Integer> entry : ModuleManager.hotkeys().entrySet()) keys.addProperty(entry.getKey(), entry.getValue());
        root.add("hotkeys", keys);
        JsonObject settings = new JsonObject();
        settings.addProperty("scanRadius", ModuleManager.scanRadius());
        settings.addProperty("scanInterval", ModuleManager.scanInterval());
        settings.addProperty("maxFindings", ModuleManager.maxFindings());
        settings.addProperty("susChunkThreshold", ModuleManager.susChunkThreshold());
        settings.addProperty("themeIndex", ModuleManager.themeIndex());
        settings.addProperty("showWatermark", ModuleManager.showWatermark());
        settings.addProperty("compactLayout", ModuleManager.compactLayout());
        settings.addProperty("hudX", HudOverlay.x());
        settings.addProperty("hudY", HudOverlay.y());
        settings.addProperty("hudScale", HudOverlay.scale());
        root.add("settings", settings);
        return root;
    }

    private static void fromJson(JsonObject root) {
        if (root == null) return;
        JsonObject modules = root.getAsJsonObject("modules");
        if (modules != null) for (Map.Entry<String, JsonElement> e : modules.entrySet())
            if (e.getValue().isJsonPrimitive()) ModuleManager.setEnabled(e.getKey(), e.getValue().getAsBoolean());
        JsonObject keys = root.getAsJsonObject("hotkeys");
        if (keys != null) for (Map.Entry<String, JsonElement> e : keys.entrySet())
            if (e.getValue().isJsonPrimitive()) {
                int key = e.getValue().getAsInt();
                // W remains unbound on startup; it is reserved for normal Minecraft movement.
                ModuleManager.setHotkey(e.getKey(), key == 87 ? -1 : key);
            }
        JsonObject settings = root.getAsJsonObject("settings");
        if (settings != null) {
            if (settings.has("scanRadius")) ModuleManager.setScanRadius(settings.get("scanRadius").getAsInt());
            if (settings.has("scanInterval")) ModuleManager.setScanInterval(settings.get("scanInterval").getAsInt());
            if (settings.has("maxFindings")) ModuleManager.setMaxFindings(settings.get("maxFindings").getAsInt());
            if (settings.has("susChunkThreshold")) ModuleManager.setSusChunkThreshold(settings.get("susChunkThreshold").getAsInt());
            if (settings.has("themeIndex")) ModuleManager.setThemeIndex(0);
            if (settings.has("showWatermark")) ModuleManager.setShowWatermark(settings.get("showWatermark").getAsBoolean());
            if (settings.has("compactLayout")) ModuleManager.setCompactLayout(true);
            if (settings.has("hudX") && settings.has("hudY") && settings.has("hudScale"))
                HudOverlay.setLayout(settings.get("hudX").getAsInt(), settings.get("hudY").getAsInt(), settings.get("hudScale").getAsFloat());
        }
    }
}

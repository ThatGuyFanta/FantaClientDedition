package dev.nova.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** In-game HUD editor with a draggable preview and a compact control card. */
public final class HudMoveScreen extends Screen {
    private boolean dragging;
    private double grabX, grabY;

    public HudMoveScreen() { super(Text.literal("FantaClient - HUD Move")); }

    private int panelX() { return Math.max(8, width - 232); }
    private int panelY() { return Math.max(8, (height - 190) / 2); }
    private boolean inPanel(double x, double y) {
        return x >= panelX() && x <= panelX() + 220 && y >= panelY() && y <= panelY() + 190;
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Light dim only: keep the live world visible while arranging the HUD.
        context.fill(0, 0, width, height, 0x44060008);

        // Draw the real HUD at its current position as a live draggable preview.
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate((float)HudOverlay.x(), (float)HudOverlay.y());
        matrices.scale(HudOverlay.scale(), HudOverlay.scale());
        int yy = 0;
        var lines = HudOverlay.buildLines(client);
        if (lines.isEmpty()) lines = java.util.List.of("FFPS: preview", "FXYZ: coordinates", "~FACING: north");
        for (String line : lines) {
            String clean = line.substring(1);
            int color = line.startsWith("!") ? ModuleManager.themeRed()
                    : line.startsWith("~") ? ModuleManager.themePurple() : ModuleManager.themeOrange();
            context.fill(-4, yy - 3, textRenderer.getWidth(clean) + 5, yy + 11, 0xAA09060C);
            context.drawTextWithShadow(textRenderer, clean, 0, yy, color);
            yy += 13;
        }
        matrices.pop();

        // HUD Move card, matching the FantaClient dashboard styling.
        int px = panelX(), py = panelY();
        context.fill(px - 2, py - 2, px + 222, py + 192, 0xEE050407);
        context.fill(px, py, px + 220, py + 190, 0xF0141018);
        context.fill(px, py, px + 220, py + 2, ModuleManager.themeOrange());
        context.fill(px + 10, py + 10, px + 210, py + 42, 0xFF241923);
        context.drawTextWithShadow(textRenderer, "✥  HUD Move", px + 18, py + 21, 0xFFFF8A24);
        context.drawTextWithShadow(textRenderer, "Scale", px + 14, py + 57, 0xFFF1EAF3);
        context.drawTextWithShadow(textRenderer, String.format(java.util.Locale.ROOT, "%.1fx", HudOverlay.scale()), px + 174, py + 57, 0xFFF1EAF3);
        context.fill(px + 14, py + 79, px + 206, py + 87, 0xFF514653);
        int knob = px + 14 + Math.round((HudOverlay.scale() - 0.5f) / 2.0f * 192);
        context.fill(px + 14, py + 79, knob, py + 87, ModuleManager.themeOrange());
        context.fill(knob - 5, py + 75, knob + 5, py + 91, 0xFFFF8A24);
        context.drawTextWithShadow(textRenderer, "Reset Position", px + 14, py + 105, 0xFFF1EAF3);
        context.fill(px + 14, py + 128, px + 206, py + 158, 0xFF2A202B);
        context.fill(px + 14, py + 128, px + 206, py + 130, ModuleManager.themeOrange());
        context.drawTextWithShadow(textRenderer, "Reset", px + 96, py + 139, 0xFFFFB46D);
        context.drawTextWithShadow(textRenderer, "Drag HUD  •  Wheel to resize", px + 14, py + 170, 0xFFB8A9BC);
        context.drawTextWithShadow(textRenderer, "ESC: save and close", 12, height - 18, 0xFFE3D7E8);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            int px = panelX(), py = panelY();
            if (inPanel(click.x(), click.y())) {
                if (click.y() >= py + 72 && click.y() <= py + 96) {
                    setScaleFromMouse(click.x());
                    ConfigManager.save();
                    return true;
                }
                if (click.y() >= py + 128 && click.y() <= py + 158) {
                    HudOverlay.setLayout(8, 8, 1.0f);
                    ConfigManager.save();
                    return true;
                }
                return true;
            }
            dragging = true;
            grabX = click.x() - HudOverlay.x();
            grabY = click.y() - HudOverlay.y();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void setScaleFromMouse(double mouseX) {
        int px = panelX();
        float value = 0.5f + (float)Math.max(0, Math.min(1, (mouseX - (px + 14)) / 192.0)) * 2.0f;
        HudOverlay.setLayout(HudOverlay.x(), HudOverlay.y(), value);
    }

    @Override public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging && click.button() == 0) {
            HudOverlay.setLayout((int)(click.x() - grabX), (int)(click.y() - grabY), HudOverlay.scale());
            return true;
        }
        if (click.button() == 0 && inPanel(click.x(), click.y())) {
            setScaleFromMouse(click.x());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(Click click) {
        if (click.button() == 0) { dragging = false; ConfigManager.save(); return true; }
        return super.mouseReleased(click);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!inPanel(mouseX, mouseY)) {
            HudOverlay.setLayout(HudOverlay.x(), HudOverlay.y(), HudOverlay.scale() + (verticalAmount > 0 ? 0.1f : -0.1f));
            ConfigManager.save();
        }
        return true;
    }

    @Override public void close() {
        dragging = false;
        ModuleManager.setEnabled("HUD Move", false);
        ConfigManager.save();
        if (client != null) client.setScreen(null);
    }
    @Override public boolean shouldPause() { return false; }
}

package com.example.fastbeo;

import java.io.IOException;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;

/** Drag the HUD elements where you want them. Esc or Done saves. */
public class GuiHudEditor extends GuiScreen {
    private final GuiScreen parent;
    private int dragging = -1;
    private int offX, offY;

    public GuiHudEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 105, height - 30, 100, 20, "Reset all"));
        buttonList.add(new GuiButton(2, width / 2 + 5, height - 30, 100, 20, "Done"));
    }

    private ScaledResolution sr() {
        return new ScaledResolution(mc);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0x60000000);
        drawCenteredString(fontRendererObj, "HUD Editor - drag the boxes, Esc/Done to save",
                width / 2, 10, 0xFFFFFF);

        ScaledResolution sr = sr();
        for (int i = 0; i < HudLayout.NAMES.length; i++) {
            int[] s = Hud.size(i);
            int x = HudLayout.left(i, s[0], sr);
            int y = HudLayout.top(i, s[1], sr);

            boolean active = dragging == i || (dragging < 0 && inside(mx, my, x, y, s[0], s[1]));
            drawRect(x, y, x + s[0], y + s[1], active ? 0x904FC3F7 : 0x60404040);

            int border = active ? 0xFFFFFFFF : 0xFF4FC3F7;
            drawRect(x, y, x + s[0], y + 1, border);
            drawRect(x, y + s[1] - 1, x + s[0], y + s[1], border);
            drawRect(x, y, x + 1, y + s[1], border);
            drawRect(x + s[0] - 1, y, x + s[0], y + s[1], border);

            String name = HudLayout.NAMES[i];
            int tw = fontRendererObj.getStringWidth(name);
            fontRendererObj.drawStringWithShadow(name,
                    x + (s[0] - tw) / 2, y + (s[1] - 8) / 2, 0xFFFFFF);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;

        ScaledResolution sr = sr();
        // Last drawn element is on top, so check from the end
        for (int i = HudLayout.NAMES.length - 1; i >= 0; i--) {
            int[] s = Hud.size(i);
            int x = HudLayout.left(i, s[0], sr);
            int y = HudLayout.top(i, s[1], sr);
            if (inside(mx, my, x, y, s[0], s[1])) {
                dragging = i;
                offX = mx - x;
                offY = my - y;
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging < 0) return;
        int[] s = Hud.size(dragging);
        int nl = Math.max(0, Math.min(mx - offX, width - s[0]));
        int nt = Math.max(0, Math.min(my - offY, height - s[1]));
        HudLayout.moveTo(dragging, nl, nt, s[0]);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (dragging >= 0) {
            HudLayout.save();
            dragging = -1;
        }
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 1) {
            HudLayout.reset();
        } else if (b.id == 2) {
            HudLayout.save();
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            HudLayout.save();
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

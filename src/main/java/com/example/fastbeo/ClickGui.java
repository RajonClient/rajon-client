package com.example.fastbeo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

public class ClickGui extends GuiScreen {
    private static final int W = 110, H = 16;
    private static final int ACCENT = 0xFF4FC3F7;

    // Entries >= 100 are tool buttons, not modules
    private static final int TOOL_ALT = 100, TOOL_HUD = 101, TOOL_CFG = 102;
    private static final int[] TOOLS = { TOOL_ALT, TOOL_HUD, TOOL_CFG };

    private static final int CATS = Modules.CATEGORY_NAMES.length;

    // Remembered while the game is running
    private static final int[] px = new int[CATS];
    private static final int[] py = new int[CATS];
    private static final boolean[] collapsed = new boolean[CATS];
    private static final boolean[] expanded = new boolean[Modules.NAMES.length];
    private static boolean placed = false;

    private int dragging = -1;
    private int dragX, dragY;
    private int listening = -1;               // module index waiting for a key, -1 = none
    private Settings.Setting sliding = null;  // slider being dragged
    private int slideX;

    // One visible row of a panel
    private static class Row {
        static final int MODULE = 0, TOOL = 1, BIND = 2, SETTING = 3;
        final int type;
        final int index;                 // module index or tool id
        final Settings.Setting setting;  // only for SETTING rows
        final int y;

        Row(int type, int index, Settings.Setting setting, int y) {
            this.type = type;
            this.index = index;
            this.setting = setting;
            this.y = y;
        }
    }

    @Override
    public void initGui() {
        if (placed) return;
        int x = 10, y = 10;
        for (int c = 0; c < CATS; c++) {
            if (x + W > width && x > 10) {
                x = 10;
                y += 150;
            }
            px[c] = x;
            py[c] = y;
            x += W + 8;
        }
        placed = true;
    }

    // Modules of a category, plus the tool buttons in the last category (Misc)
    private static int[] entries(int c) {
        int n = 0;
        for (int i = 0; i < Modules.NAMES.length; i++) {
            if (Modules.CATEGORY[i] == c) n++;
        }
        boolean tools = c == CATS - 1;
        int[] out = new int[n + (tools ? TOOLS.length : 0)];
        int k = 0;
        for (int i = 0; i < Modules.NAMES.length; i++) {
            if (Modules.CATEGORY[i] == c) out[k++] = i;
        }
        if (tools) {
            for (int t : TOOLS) out[k++] = t;
        }
        return out;
    }

    private static List<Row> rows(int c) {
        List<Row> list = new ArrayList<Row>();
        int y = py[c] + H;
        for (int e : entries(c)) {
            if (e >= 100) {
                list.add(new Row(Row.TOOL, e, null, y));
                y += H;
                continue;
            }
            list.add(new Row(Row.MODULE, e, null, y));
            y += H;
            if (expanded[e]) {
                list.add(new Row(Row.BIND, e, null, y));
                y += H;
                for (Settings.Setting s : Settings.of(e)) {
                    list.add(new Row(Row.SETTING, e, s, y));
                    y += H;
                }
            }
        }
        return list;
    }

    private static String toolLabel(int e) {
        if (e == TOOL_ALT) return "Alt Manager";
        if (e == TOOL_HUD) return "HUD Editor";
        return "Configs";
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String keyText(int module) {
        if (listening == module) return "...";
        int code = Binds.get(module);
        return code == 0 ? "-" : Keyboard.getKeyName(code);
    }

    private void textRight(String t, int x, int y, int color) {
        int tw = fontRendererObj.getStringWidth(t);
        fontRendererObj.drawStringWithShadow(t, x + W - tw - 4, y + 4, color);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        String description = null;

        for (int c = 0; c < CATS; c++) {
            int x = px[c], y = py[c];

            drawRect(x, y, x + W, y + H, 0xFF1B1B1B);
            drawRect(x, y + H - 1, x + W, y + H, ACCENT);
            fontRendererObj.drawStringWithShadow(Modules.CATEGORY_NAMES[c], x + 4, y + 4, 0xFFFFFF);
            textRight(collapsed[c] ? "+" : "-", x, y, 0x888888);

            if (collapsed[c]) continue;

            for (Row r : rows(c)) {
                boolean hover = inside(mx, my, x, r.y, W, H);

                if (r.type == Row.MODULE) {
                    boolean on = Modules.isOn(r.index);
                    int color = on ? (hover ? 0xFF43A047 : 0xFF2E7D32)
                                   : (hover ? 0xFF4A4A4A : 0xFF333333);
                    drawRect(x, r.y, x + W, r.y + H, color);
                    fontRendererObj.drawStringWithShadow(Modules.NAMES[r.index], x + 4, r.y + 4,
                            on ? 0xFFFFFF : 0xAAAAAA);
                    textRight(expanded[r.index] ? "v" : ">", x, r.y, 0x888888);
                    if (hover) description = Modules.DESCRIPTIONS[r.index];

                } else if (r.type == Row.TOOL) {
                    drawRect(x, r.y, x + W, r.y + H, hover ? 0xFF4A4A4A : 0xFF333333);
                    fontRendererObj.drawStringWithShadow(toolLabel(r.index), x + 4, r.y + 4, 0xAAAAAA);

                } else if (r.type == Row.BIND) {
                    drawRect(x, r.y, x + W, r.y + H, hover ? 0xFF2C2C2C : 0xFF222222);
                    fontRendererObj.drawStringWithShadow("Key", x + 8, r.y + 4, 0xCCCCCC);
                    textRight(keyText(r.index), x, r.y, listening == r.index ? 0xFFD54F : 0x8888FF);

                } else {
                    Settings.Setting s = r.setting;
                    drawRect(x, r.y, x + W, r.y + H, hover ? 0xFF2C2C2C : 0xFF222222);
                    if (s.toggle) {
                        if (s.on()) drawRect(x, r.y, x + W, r.y + H, 0xFF1F4D22);
                    } else {
                        int fill = (int) (W * (s.value - s.min) / (s.max - s.min));
                        drawRect(x, r.y, x + fill, r.y + H, 0xFF2B5F78);
                    }
                    fontRendererObj.drawStringWithShadow(s.name, x + 8, r.y + 4, 0xCCCCCC);
                    textRight(s.display(), x, r.y, 0xFFFFFF);
                }
            }
        }

        // Description of the hovered module and a short help line
        if (description != null) {
            fontRendererObj.drawStringWithShadow(description,
                    (width - fontRendererObj.getStringWidth(description)) / 2, height - 24, 0xFFFFFF);
        }
        String hint = listening >= 0
                ? "Press a key (Del = clear, Esc = cancel)"
                : "Left click: toggle   Right click: settings and key";
        fontRendererObj.drawStringWithShadow(hint,
                (width - fontRendererObj.getStringWidth(hint)) / 2, height - 12, 0x888888);

        super.drawScreen(mx, my, pt);
    }

    private void applySlider(int mx) {
        if (sliding == null) return;
        double frac = Math.max(0.0, Math.min(1.0, (mx - slideX) / (double) W));
        Settings.set(sliding, sliding.min + frac * (sliding.max - sliding.min));
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        for (int c = 0; c < CATS; c++) {
            int x = px[c], y = py[c];

            // Header: left = drag, right = collapse
            if (inside(mx, my, x, y, W, H)) {
                listening = -1;
                if (button == 0) {
                    dragging = c;
                    dragX = mx - x;
                    dragY = my - y;
                } else if (button == 1) {
                    collapsed[c] = !collapsed[c];
                }
                return;
            }

            if (collapsed[c]) continue;

            for (Row r : rows(c)) {
                if (!inside(mx, my, x, r.y, W, H)) continue;

                if (r.type == Row.MODULE) {
                    if (button == 0) {
                        listening = -1;
                        Modules.toggle(r.index);
                    } else if (button == 1) {
                        expanded[r.index] = !expanded[r.index];
                    }
                } else if (r.type == Row.TOOL) {
                    if (button == 0) {
                        listening = -1;
                        if (r.index == TOOL_ALT) mc.displayGuiScreen(new GuiAltManager(this));
                        else if (r.index == TOOL_HUD) mc.displayGuiScreen(new GuiHudEditor(this));
                        else mc.displayGuiScreen(new GuiConfigs(this));
                    }
                } else if (r.type == Row.BIND) {
                    if (button == 0) listening = r.index;
                } else if (button == 0) {
                    listening = -1;
                    if (r.setting.toggle) {
                        Settings.set(r.setting, r.setting.on() ? 0 : 1);
                    } else {
                        sliding = r.setting;
                        slideX = x;
                        applySlider(mx);
                    }
                }
                return;
            }
        }

        // Clicked on empty space
        listening = -1;
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging >= 0) {
            px[dragging] = mx - dragX;
            py[dragging] = my - dragY;
        }
        applySlider(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragging = -1;
        sliding = null;
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (listening >= 0) {
            if (key == Keyboard.KEY_ESCAPE) {
                listening = -1;
                return;
            }
            if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) {
                Binds.set(listening, 0);
            } else {
                Binds.set(listening, key);
            }
            listening = -1;
            return;
        }

        if (key == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

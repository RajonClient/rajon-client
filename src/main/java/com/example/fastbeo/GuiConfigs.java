package com.example.fastbeo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

public class GuiConfigs extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField nameField;
    private String status = "";
    private List<String> names = new ArrayList<String>();

    public GuiConfigs(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        nameField = new GuiTextField(0, fontRendererObj, width / 2 - 100, 50, 200, 20);
        nameField.setMaxStringLength(24);
        nameField.setFocused(true);
        buttonList.add(new GuiButton(1, width / 2 - 100, 76, 64, 20, "Save"));
        buttonList.add(new GuiButton(2, width / 2 - 32, 76, 64, 20, "Load"));
        buttonList.add(new GuiButton(3, width / 2 + 36, 76, 64, 20, "Delete"));
        buttonList.add(new GuiButton(4, width / 2 - 100, height - 30, 200, 20, "Back"));
        names = ConfigManager.list();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void doSave(String name) {
        if (!ConfigManager.validName(name) || ConfigManager.AUTO.equals(name)) {
            status = "Bad name: use 1-24 letters, numbers, - or _ (not 'autosave').";
            return;
        }
        status = ConfigManager.save(name) ? "Saved: " + name : "Could not save: " + name;
        names = ConfigManager.list();
    }

    private void doLoad(String name) {
        status = ConfigManager.load(name) ? "Loaded: " + name : "No config named: " + name;
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        String name = nameField.getText().trim();
        if (b.id == 1) {
            doSave(name);
        } else if (b.id == 2) {
            doLoad(name);
        } else if (b.id == 3) {
            if (ConfigManager.delete(name)) {
                status = "Deleted: " + name;
                names = ConfigManager.list();
            } else {
                status = "Could not delete: " + name;
            }
        } else if (b.id == 4) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            doSave(nameField.getText().trim());
            return;
        }
        nameField.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        nameField.mouseClicked(mx, my, button);
        if (button == 0) {
            for (int i = 0; i < names.size(); i++) {
                int y = 130 + i * 14;
                if (mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12) {
                    nameField.setText(names.get(i));
                    doLoad(names.get(i));
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Configs", width / 2, 18, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Auto-saved on every change, loaded at startup",
                width / 2, 32, 0x888888);
        nameField.drawTextBox();
        drawCenteredString(fontRendererObj, status, width / 2, 104, 0xFFD54F);

        drawString(fontRendererObj, "Saved configs (click to load):", width / 2 - 100, 118, 0x888888);
        for (int i = 0; i < names.size(); i++) {
            int y = 130 + i * 14;
            boolean hover = mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12;
            drawRect(width / 2 - 100, y, width / 2 + 100, y + 12, hover ? 0xFF4A4A4A : 0xFF2A2A2A);
            drawString(fontRendererObj, names.get(i), width / 2 - 96, y + 2, 0xFFFFFF);
        }
        super.drawScreen(mx, my, pt);
    }
}

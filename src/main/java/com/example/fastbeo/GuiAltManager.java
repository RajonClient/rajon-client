package com.example.fastbeo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.lwjgl.input.Keyboard;

public class GuiAltManager extends GuiScreen {
    // Saved names are kept in memory until the game closes
    private static final List<String> saved = new ArrayList<String>();

    private GuiTextField nameField;
    private String status = "";
    private final GuiScreen parent;

    public GuiAltManager(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        nameField = new GuiTextField(0, fontRendererObj, width / 2 - 100, 60, 200, 20);
        nameField.setMaxStringLength(16);
        nameField.setFocused(true);
        buttonList.add(new GuiButton(1, width / 2 - 100, 86, 98, 20, "Use name"));
        buttonList.add(new GuiButton(2, width / 2 + 2, 86, 98, 20, "Save + use"));
        buttonList.add(new GuiButton(3, width / 2 - 100, height - 30, 200, 20, "Back"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private boolean validName(String n) {
        return n.matches("[A-Za-z0-9_]{3,16}");
    }

    private void applyName(String name) {
        if (mc.getCurrentServerData() != null) {
            status = "Disabled while connected to a multiplayer server.";
            return;
        }
        if (!validName(name)) {
            status = "Name must be 3-16 chars: letters, numbers, underscore.";
            return;
        }
        try {
            String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes("UTF-8"))
                    .toString().replace("-", "");
            Session s = new Session(name, uuid, "0", "legacy");
            ReflectionHelper.setPrivateValue(Minecraft_class(), mc, s, "session", "field_71449_j");
            status = "Name set to: " + name + " (applies to worlds you open next)";
        } catch (Exception ex) {
            status = "Failed: " + ex.getClass().getSimpleName();
        }
    }

    private static Class<net.minecraft.client.Minecraft> Minecraft_class() {
        return net.minecraft.client.Minecraft.class;
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        String name = nameField.getText().trim();
        if (b.id == 1) {
            applyName(name);
        } else if (b.id == 2) {
            applyName(name);
            if (validName(name) && !saved.contains(name)) saved.add(name);
        } else if (b.id == 3) {
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
            applyName(nameField.getText().trim());
            return;
        }
        nameField.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        nameField.mouseClicked(mx, my, button);
        if (button == 0) {
            for (int i = 0; i < saved.size(); i++) {
                int y = 146 + i * 14;
                if (mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12) {
                    nameField.setText(saved.get(i));
                    applyName(saved.get(i));
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
        drawCenteredString(fontRendererObj, "Alt Manager (offline names)", width / 2, 20, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Current: " + mc.getSession().getUsername(),
                width / 2, 40, 0xAAAAAA);
        nameField.drawTextBox();
        drawCenteredString(fontRendererObj, status, width / 2, 112, 0xFFD54F);

        drawString(fontRendererObj, "Saved names (click to use):", width / 2 - 100, 132, 0x888888);
        for (int i = 0; i < saved.size(); i++) {
            int y = 146 + i * 14;
            boolean hover = mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12;
            drawRect(width / 2 - 100, y, width / 2 + 100, y + 12, hover ? 0xFF4A4A4A : 0xFF2A2A2A);
            drawString(fontRendererObj, saved.get(i), width / 2 - 96, y + 2, 0xFFFFFF);
        }
        super.drawScreen(mx, my, pt);
    }
}

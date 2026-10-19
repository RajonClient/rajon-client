package com.example.fastbeo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.lwjgl.input.Keyboard;

public class GuiAltManager extends GuiScreen {
    // Saved names, kept in .minecraft/config/farreach/alts.txt (one name per line)
    private static final List<String> saved = new ArrayList<String>();
    private static boolean loaded = false;

    private static File file() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/farreach");
        dir.mkdirs();
        return new File(dir, "alts.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        saved.clear();

        File f = file();
        if (!f.exists()) return;

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.matches("[A-Za-z0-9_]{3,16}") && !saved.contains(line)) saved.add(line);
            }
        } catch (Exception ex) {
            // Unreadable file: start with an empty list
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) { }
            }
        }
    }

    private static void save() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file()), "UTF-8"));
            for (String n : saved) {
                out.write(n);
                out.newLine();
            }
        } catch (Exception ex) {
            // Could not save: the names still work until the game closes
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) { }
            }
        }
    }

    private GuiTextField nameField;
    private String status = "";
    private final GuiScreen parent;

    public GuiAltManager(GuiScreen parent) {
        this.parent = parent;
        load();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        nameField = new GuiTextField(0, fontRendererObj, width / 2 - 100, 60, 200, 20);
        nameField.setMaxStringLength(16);
        nameField.setFocused(true);
        buttonList.add(new GuiButton(1, width / 2 - 100, 86, 64, 20, "Use"));
        buttonList.add(new GuiButton(2, width / 2 - 32, 86, 64, 20, "Save + use"));
        buttonList.add(new GuiButton(4, width / 2 + 36, 86, 64, 20, "Random"));
        buttonList.add(new GuiButton(3, width / 2 - 100, height - 30, 200, 20, "Back"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private static final Random RAND = new Random();

    private static final String[] FIRST = {
        "Swift", "Dark", "Pixel", "Frost", "Shadow", "Blaze", "Ninja", "Crazy", "Silent", "Lucky",
        "Mighty", "Cosmic", "Rapid", "Iron", "Storm", "Nova", "Turbo", "Ghost", "Wild", "Epic"
    };
    private static final String[] SECOND = {
        "Wolf", "Fox", "Knight", "Miner", "Raven", "Tiger", "Dragon", "Hunter", "Panda", "Falcon",
        "Builder", "Ranger", "Viper", "Golem", "Archer", "Shark", "Phoenix", "Cobra", "Bear", "Pilot"
    };

    // Random name like "SwiftFox42" (always a valid 3-16 character name)
    private String randomName() {
        String name;
        int tries = 0;
        do {
            name = FIRST[RAND.nextInt(FIRST.length)] + SECOND[RAND.nextInt(SECOND.length)];
            if (RAND.nextInt(4) != 0) name += RAND.nextInt(100);
            if (name.length() > 16) name = name.substring(0, 16);
            tries++;
        } while (saved.contains(name) && tries < 20);
        return name;
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
            if (validName(name) && !saved.contains(name)) {
                saved.add(name);
                save();
            }
        } else if (b.id == 4) {
            // Random alt: make a name, use it and keep it in the list
            String random = randomName();
            nameField.setText(random);
            applyName(random);
            if (validName(random) && !saved.contains(random) && mc.getCurrentServerData() == null) {
                saved.add(random);
                save();
            }
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
        if (button == 0 || button == 1) {
            for (int i = 0; i < saved.size(); i++) {
                int y = 146 + i * 14;
                if (mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12) {
                    if (button == 0) {
                        nameField.setText(saved.get(i));
                        applyName(saved.get(i));
                    } else {
                        // Right click removes the name from the list
                        status = "Removed: " + saved.get(i);
                        saved.remove(i);
                        save();
                    }
                    break;
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

        drawString(fontRendererObj, "Saved names (click = use, right click = remove):", width / 2 - 100, 132, 0x888888);
        for (int i = 0; i < saved.size(); i++) {
            int y = 146 + i * 14;
            boolean hover = mx >= width / 2 - 100 && mx < width / 2 + 100 && my >= y && my < y + 12;
            drawRect(width / 2 - 100, y, width / 2 + 100, y + 12, hover ? 0xFF4A4A4A : 0xFF2A2A2A);
            drawString(fontRendererObj, saved.get(i), width / 2 - 96, y + 2, 0xFFFFFF);
        }
        super.drawScreen(mx, my, pt);
    }
}

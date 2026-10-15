package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MainMenuHook {
    private static final int BUTTON_ID = 9000;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (e.gui instanceof GuiMainMenu) {
            e.buttonList.add(new GuiButton(BUTTON_ID, 5, 5, 100, 20, "Alt Manager"));
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if (e.gui instanceof GuiMainMenu && e.button.id == BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiAltManager(e.gui));
        }
    }
}

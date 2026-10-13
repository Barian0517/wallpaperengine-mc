package moe.barian.wallpaperengine.forge.gui;

import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

public class WallpaperSettingsScreen extends Screen {

    private final Screen lastScreen;
    private EditBox pathEditBox;

    public WallpaperSettingsScreen(Screen lastScreen) {
        super(Component.literal("Wallpaper Engine Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.pathEditBox = new EditBox(this.font, centerX - 150, centerY - 20, 300, 20, Component.literal("Wallpaper Path"));
        this.pathEditBox.setMaxLength(512);
        this.pathEditBox.setValue(WallpaperEngineManager.getInstance().getWallpaperPath());
        this.addRenderableWidget(this.pathEditBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save & Back"), (button) -> {
            WallpaperEngineManager.getInstance().setWallpaperPath(this.pathEditBox.getValue());
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX - 100, centerY + 20, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Test Launch"), (button) -> {
            WallpaperEngineManager.getInstance().setWallpaperPath(this.pathEditBox.getValue());
            // Need a path to project.json to test
            WallpaperEngineManager.getInstance().startWallpaper(this.pathEditBox.getValue() + "\\project.json");
        }).bounds(centerX + 5, centerY + 20, 95, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Enter Wallpaper Project path:", this.width / 2 - 150, this.height / 2 - 35, 0xA0A0A0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

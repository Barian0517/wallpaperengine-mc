package moe.barian.wallpaperengine.neoforge.gui;

import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import java.io.File;

public class WallpaperSettingsScreen extends Screen {

    private final Screen lastScreen;
    private EditBox pathEditBox;
    private EditBox fpsEditBox;
    private EditBox widthEditBox;
    private EditBox heightEditBox;
    private boolean enabled;
    private boolean debugHudEnabled;
    private boolean hideLogoEnabled;
    private boolean replaceAllBackgroundsEnabled;

    public WallpaperSettingsScreen(Screen lastScreen) {
        super(Component.translatable("gui.wallpaperengine_mc.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        WallpaperEngineManager manager = WallpaperEngineManager.getInstance();
        this.enabled = manager.isEnabled();
        this.debugHudEnabled = manager.isDebugHudEnabled();
        this.hideLogoEnabled = manager.isHideLogoEnabled();
        this.replaceAllBackgroundsEnabled = manager.isReplaceAllBackgroundsEnabled();
        
        int listTop = 40;
        int rowHeight = 24;
        
        int leftCol = this.width / 2 - 150;
        int rightCol = this.width / 2;
        int widgetWidth = 150;

        // Row 0: Enabled
        Button enabledButton = Button.builder(this.enabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"), (button) -> {
            this.enabled = !this.enabled;
            button.setMessage(this.enabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"));
        }).bounds(rightCol, listTop, widgetWidth, 20).build();
        this.addRenderableWidget(enabledButton);

        // Row 1: Path
        this.pathEditBox = new EditBox(this.font, rightCol, listTop + rowHeight, widgetWidth, 20, Component.translatable("gui.wallpaperengine_mc.path"));
        this.pathEditBox.setMaxLength(512);
        this.pathEditBox.setValue(manager.getWallpaperPath());
        this.addRenderableWidget(this.pathEditBox);

        // Row 2: FPS
        this.fpsEditBox = new EditBox(this.font, rightCol, listTop + rowHeight * 2, widgetWidth, 20, Component.translatable("gui.wallpaperengine_mc.fps"));
        this.fpsEditBox.setMaxLength(4);
        this.fpsEditBox.setValue(String.valueOf(manager.getTargetFps()));
        this.addRenderableWidget(this.fpsEditBox);
        
        // Row 3: Width
        this.widthEditBox = new EditBox(this.font, rightCol, listTop + rowHeight * 3, widgetWidth, 20, Component.translatable("gui.wallpaperengine_mc.width"));
        this.widthEditBox.setMaxLength(5);
        this.widthEditBox.setValue(String.valueOf(manager.getWidth()));
        this.addRenderableWidget(this.widthEditBox);
        
        // Row 4: Height
        this.heightEditBox = new EditBox(this.font, rightCol, listTop + rowHeight * 4, widgetWidth, 20, Component.translatable("gui.wallpaperengine_mc.height"));
        this.heightEditBox.setMaxLength(5);
        this.heightEditBox.setValue(String.valueOf(manager.getHeight()));
        this.addRenderableWidget(this.heightEditBox);
        
        // Row 5: Debug HUD
        Button debugHudButton = Button.builder(this.debugHudEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"), (button) -> {
            this.debugHudEnabled = !this.debugHudEnabled;
            button.setMessage(this.debugHudEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"));
        }).bounds(rightCol, listTop + rowHeight * 5, widgetWidth, 20).build();
        this.addRenderableWidget(debugHudButton);

        // Row 6: Hide Logo
        Button hideLogoButton = Button.builder(this.hideLogoEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"), (button) -> {
            this.hideLogoEnabled = !this.hideLogoEnabled;
            button.setMessage(this.hideLogoEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"));
        }).bounds(rightCol, listTop + rowHeight * 6, widgetWidth, 20).build();
        this.addRenderableWidget(hideLogoButton);

        // Row 7: Replace All Backgrounds
        Button replaceAllBgButton = Button.builder(this.replaceAllBackgroundsEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"), (button) -> {
            this.replaceAllBackgroundsEnabled = !this.replaceAllBackgroundsEnabled;
            button.setMessage(this.replaceAllBackgroundsEnabled ? Component.translatable("gui.wallpaperengine_mc.on") : Component.translatable("gui.wallpaperengine_mc.off"));
        }).bounds(rightCol, listTop + rowHeight * 7, widgetWidth, 20).build();
        this.addRenderableWidget(replaceAllBgButton);

        // Footer Buttons
        int footerY = this.height - 30;
        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.wallpaperengine_mc.cancel"), (button) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 - 105, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.wallpaperengine_mc.save"), (button) -> {
            try {
                int fps = Integer.parseInt(this.fpsEditBox.getValue());
                int w = Integer.parseInt(this.widthEditBox.getValue());
                int h = Integer.parseInt(this.heightEditBox.getValue());
                String path = this.pathEditBox.getValue().trim().replace("\"", "");
                
                manager.setSettings(path, w, h, fps, this.debugHudEnabled, this.enabled, this.hideLogoEnabled, this.replaceAllBackgroundsEnabled);
                
                if (new File(path).isDirectory()) {
                    path = new File(path, "project.json").getAbsolutePath();
                }
                manager.startWallpaper(path);
                this.minecraft.setScreen(this.lastScreen);
            } catch (Exception e) {}
        }).bounds(this.width / 2 + 5, footerY, 100, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render wallpaper if enabled, else do nothing (prevent vanilla blur)
        moe.barian.wallpaperengine.neoforge.gui.WallpaperRenderer.renderWallpaper(this, guiGraphics, false);
        
        // Semi-transparent overlay to make settings readable over wallpaper
        guiGraphics.fill(0, 0, this.width, this.height, 0xBB000000); 
        
        guiGraphics.fill(0, 0, this.width, 30, 0x88202020);          // Header
        guiGraphics.fill(0, this.height - 40, this.width, this.height, 0x88202020); // Footer
        
        // Borders
        guiGraphics.fill(0, 30, this.width, 31, 0xFF404040);
        guiGraphics.fill(0, this.height - 40, this.width, this.height - 39, 0xFF404040);
        
        int listTop = 40;
        int rowHeight = 24;
        int leftCol = this.width / 2 - 150;
        
        // Draw alternating row backgrounds for readability
        for (int i = 0; i < 8; i++) {
            if (i % 2 == 0) {
                guiGraphics.fill(leftCol - 10, listTop + i * rowHeight - 2, this.width / 2 + 160, listTop + i * rowHeight + 22, 0xFF181818);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        
        int listTop = 40;
        int rowHeight = 24;
        int leftCol = this.width / 2 - 150;

        // Draw Labels (Without drop shadow to look cleaner/crisper like MaLiLib)
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.enable"), leftCol, listTop + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.path"), leftCol, listTop + rowHeight + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.fps"), leftCol, listTop + rowHeight * 2 + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.width"), leftCol, listTop + rowHeight * 3 + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.height"), leftCol, listTop + rowHeight * 4 + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.debug_hud"), leftCol, listTop + rowHeight * 5 + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.hide_logo"), leftCol, listTop + rowHeight * 6 + 6, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wallpaperengine_mc.label.replace_all"), leftCol, listTop + rowHeight * 7 + 6, 0xAAAAAA, false);
        
    }
}

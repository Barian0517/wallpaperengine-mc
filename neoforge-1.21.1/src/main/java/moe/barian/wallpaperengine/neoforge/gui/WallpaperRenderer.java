package moe.barian.wallpaperengine.neoforge.gui;

import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;

public class WallpaperRenderer {

    private static DynamicTexture weTexture = null;
    private static ResourceLocation weTextureLocation = null;

    public static boolean renderWallpaper(Screen screen, GuiGraphics guiGraphics, boolean isTitleScreen) {
        WallpaperEngineManager manager = WallpaperEngineManager.getInstance();
        
        if (!manager.isEnabled()) return false;
        if (!isTitleScreen && !manager.isReplaceAllBackgroundsEnabled()) return false;
        
        String screenName = screen.getClass().getName().toLowerCase();
        
        // Exclude Chat and Map interfaces
        if (screenName.contains("chat") && !screenName.contains("settings") && !screenName.contains("options")) {
            return false;
        }
        if (screenName.contains("map") || screenName.contains("journeymap") || screenName.contains("xaero")) {
            return false;
        }

        if (manager.getLatestFrame() != null) {
            int width = manager.getWidth();
            int height = manager.getHeight();

            if (weTexture == null || weTexture.getPixels().getWidth() != width || weTexture.getPixels().getHeight() != height) {
                if (weTexture != null) {
                    weTexture.close();
                }
                weTexture = new DynamicTexture(width, height, true);
                weTextureLocation = ResourceLocation.fromNamespaceAndPath("wallpaperengine_mc", "we_bg_global");
                Minecraft.getInstance().getTextureManager().register(weTextureLocation, weTexture);
            }

            if (manager.isFrameUpdated()) {
                int[] pixels = manager.getLatestFrame();
                com.mojang.blaze3d.platform.NativeImage image = weTexture.getPixels();
                boolean allBlack = true;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = pixels[y * width + x];
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        if (r != 0 || g != 0 || b != 0) {
                            allBlack = false;
                        }
                        int abgr = (0xFF << 24) | (b << 16) | (g << 8) | r;
                        image.setPixelRGBA(x, y, abgr);
                    }
                }
                weTexture.upload();
                if (allBlack && manager.isDebugHudEnabled() && isTitleScreen) {
                    System.out.println("Captured frame is completely BLACK!");
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(weTextureLocation, 0, 0, screen.width, screen.height, 0, 0, width, height, width, height);
            
            if (manager.isDebugHudEnabled() && isTitleScreen) {
                guiGraphics.drawString(Minecraft.getInstance().font, "WE Connected!", 10, 10, 0x00FF00);
            }
            return true;
        } else {
            if (manager.isDebugHudEnabled() && isTitleScreen) {
                guiGraphics.drawString(Minecraft.getInstance().font, "WE Not Connected", 10, 10, 0xFF0000);
            }
            return false;
        }
    }
}

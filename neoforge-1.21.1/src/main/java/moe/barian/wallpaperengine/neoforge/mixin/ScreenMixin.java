package moe.barian.wallpaperengine.neoforge.mixin;

import moe.barian.wallpaperengine.neoforge.gui.WallpaperRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderWallpaper(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        boolean isTitleScreen = ((Object) this) instanceof TitleScreen;
        if (!isTitleScreen) {
            boolean rendered = WallpaperRenderer.renderWallpaper((Screen)(Object)this, guiGraphics, false);
            if (rendered) {
                ci.cancel();
            }
        }
    }
}

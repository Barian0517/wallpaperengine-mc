package moe.barian.wallpaperengine.neoforge.mixin;

import moe.barian.wallpaperengine.neoforge.gui.WallpaperRenderer;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void renderWallpaper(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WallpaperRenderer.renderWallpaper((net.minecraft.client.gui.screens.Screen)(Object)this, guiGraphics, false);
    }
}

package moe.barian.wallpaperengine.neoforge.mixin;

import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public class LogoRendererMixin {
    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V", at = @At("HEAD"), cancellable = true)
    private void hideLogo(net.minecraft.client.gui.GuiGraphics guiGraphics, int p_281268_, float p_281269_, CallbackInfo ci) {
        if (WallpaperEngineManager.getInstance().isHideLogoEnabled()) {
            ci.cancel();
        }
    }
}

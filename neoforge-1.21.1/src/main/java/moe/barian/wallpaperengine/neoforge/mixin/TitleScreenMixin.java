package moe.barian.wallpaperengine.neoforge.mixin;

import moe.barian.wallpaperengine.neoforge.gui.WallpaperSettingsScreen;
import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;
import java.nio.IntBuffer;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private DynamicTexture weTexture;
    private ResourceLocation weTextureLocation;
    private IntBuffer pixelBuffer;

    protected TitleScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addWallpaperEngineButton(CallbackInfo ci) {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int x = this.width - buttonWidth - 10;
        int y = this.height - buttonHeight - 10;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.wallpaperengine_mc.button"), (button) -> {
            this.minecraft.setScreen(new WallpaperSettingsScreen(this));
        }).bounds(x, y, buttonWidth, buttonHeight).build());
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderWallpaper(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        boolean rendered = moe.barian.wallpaperengine.neoforge.gui.WallpaperRenderer.renderWallpaper(this, guiGraphics, true);
        if (rendered) {
            ci.cancel();
        }
    }
}

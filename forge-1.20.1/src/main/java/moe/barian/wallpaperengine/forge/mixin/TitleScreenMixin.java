package moe.barian.wallpaperengine.forge.mixin;

import moe.barian.wallpaperengine.forge.gui.WallpaperSettingsScreen;
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

        this.addRenderableWidget(Button.builder(Component.literal("WE Settings"), (button) -> {
            this.minecraft.setScreen(new WallpaperSettingsScreen(this));
        }).bounds(x, y, buttonWidth, buttonHeight).build());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V"))
    private void renderWallpaper(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WallpaperEngineManager manager = WallpaperEngineManager.getInstance();
        if (manager.getLatestFrame() != null) {
            int width = manager.getWidth();
            int height = manager.getHeight();

            if (weTexture == null || weTexture.getPixels().getWidth() != width || weTexture.getPixels().getHeight() != height) {
                if (weTexture != null) {
                    weTexture.close();
                }
                if (pixelBuffer != null) {
                    MemoryUtil.memFree(pixelBuffer);
                }
                weTexture = new DynamicTexture(width, height, true);
                weTextureLocation = this.minecraft.getTextureManager().register("we_bg", weTexture);
                pixelBuffer = MemoryUtil.memAllocInt(width * height);
            }

            if (manager.isFrameUpdated()) {
                int[] pixels = manager.getLatestFrame();
                pixelBuffer.clear();
                pixelBuffer.put(pixels);
                pixelBuffer.flip();
                
                RenderSystem.bindTexture(weTexture.getId());
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(weTextureLocation, 0, 0, this.width, this.height, 0, 0, width, height, width, height);
        }
    }
}

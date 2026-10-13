package moe.barian.wallpaperengine.neoforge;

import moe.barian.wallpaperengine.core.WallpaperEngineManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod("wallpaperengine_mc")
public class WallpaperEngineMod {
    public WallpaperEngineMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
    }
    
    private void onClientSetup(final FMLClientSetupEvent event) {
        WallpaperEngineManager manager = WallpaperEngineManager.getInstance();
        if (manager.isEnabled()) {
            String path = manager.getWallpaperPath();
            if (new java.io.File(path).isDirectory()) {
                path = new java.io.File(path, "project.json").getAbsolutePath();
            }
            manager.startWallpaper(path);
        }
    }
}

package moe.barian.wallpaperengine.core;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WallpaperEngineManager {
    
    private static WallpaperEngineManager instance;
    private String wallpaperPath = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\wallpaper_engine\\projects\\myprojects";
    
    private Process weProcess;
    private HWND weHwnd;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread captureThread;
    
    private AtomicReference<int[]> latestFrame = new AtomicReference<>(null);
    private int frameWidth = 1280;
    private int frameHeight = 720;
    private int targetFps = 30;
    private boolean debugHudEnabled = true;
    private boolean enabled = true;
    private boolean hideLogoEnabled = false;
    private boolean replaceAllBackgroundsEnabled = true;
    private boolean frameUpdated = false;
    
    private WallpaperEngineManager() {
        loadConfig();
    }
    
    public static WallpaperEngineManager getInstance() {
        if (instance == null) {
            instance = new WallpaperEngineManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            File configFile = new File("config", "wallpaperengine_mc.properties");
            if (configFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    if (props.containsKey("wallpaperPath")) {
                        this.wallpaperPath = props.getProperty("wallpaperPath");
                    }
                    if (props.containsKey("frameWidth")) {
                        this.frameWidth = Integer.parseInt(props.getProperty("frameWidth"));
                    }
                    if (props.containsKey("frameHeight")) {
                        this.frameHeight = Integer.parseInt(props.getProperty("frameHeight"));
                    }
                    if (props.containsKey("targetFps")) {
                        this.targetFps = Integer.parseInt(props.getProperty("targetFps"));
                    }
                    if (props.containsKey("debugHudEnabled")) {
                        this.debugHudEnabled = Boolean.parseBoolean(props.getProperty("debugHudEnabled"));
                    }
                    if (props.containsKey("enabled")) {
                        this.enabled = Boolean.parseBoolean(props.getProperty("enabled"));
                    }
                    if (props.containsKey("hideLogoEnabled")) {
                        this.hideLogoEnabled = Boolean.parseBoolean(props.getProperty("hideLogoEnabled"));
                    }
                    if (props.containsKey("replaceAllBackgroundsEnabled")) {
                        this.replaceAllBackgroundsEnabled = Boolean.parseBoolean(props.getProperty("replaceAllBackgroundsEnabled"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveConfig() {
        try {
            File configDir = new File("config");
            if (!configDir.exists()) configDir.mkdirs();
            File configFile = new File(configDir, "wallpaperengine_mc.properties");
            
            Properties props = new Properties();
            props.setProperty("wallpaperPath", this.wallpaperPath);
            props.setProperty("frameWidth", String.valueOf(this.frameWidth));
            props.setProperty("frameHeight", String.valueOf(this.frameHeight));
            props.setProperty("targetFps", String.valueOf(this.targetFps));
            props.setProperty("debugHudEnabled", String.valueOf(this.debugHudEnabled));
            props.setProperty("enabled", String.valueOf(this.enabled));
            props.setProperty("hideLogoEnabled", String.valueOf(this.hideLogoEnabled));
            props.setProperty("replaceAllBackgroundsEnabled", String.valueOf(this.replaceAllBackgroundsEnabled));
            
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "Wallpaper Engine MC Configuration");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setWallpaperPath(String path) {
        this.wallpaperPath = path;
    }
    
    public String getWallpaperPath() {
        return this.wallpaperPath;
    }
    
    public void setSettings(String path, int width, int height, int fps, boolean debugHud, boolean enable, boolean hideLogo, boolean replaceAllBg) {
        this.wallpaperPath = path;
        this.frameWidth = width;
        this.frameHeight = height;
        this.targetFps = fps;
        this.debugHudEnabled = debugHud;
        this.enabled = enable;
        this.hideLogoEnabled = hideLogo;
        this.replaceAllBackgroundsEnabled = replaceAllBg;
        saveConfig();
    }
    
    public int getTargetFps() { return this.targetFps; }
    public boolean isDebugHudEnabled() { return this.debugHudEnabled; }
    public boolean isEnabled() { return this.enabled; }
    public boolean isHideLogoEnabled() { return this.hideLogoEnabled; }
    public boolean isReplaceAllBackgroundsEnabled() { return this.replaceAllBackgroundsEnabled; }
    
    public interface User32Ext extends User32 {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class);
        boolean PrintWindow(HWND hwnd, HDC hdcBlt, int nFlags);
        boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
    }
    
    public void startWallpaper(String projectPath) {
        if (!this.enabled) {
            stopWallpaper();
            return;
        }
        
        stopWallpaper();

        // Sanitize path (remove quotes if user pasted them)
        projectPath = projectPath.replace("\"", "").trim();
        
        try {
            File weDir = new File(projectPath).getParentFile();
            while (weDir != null && !new File(weDir, "wallpaper32.exe").exists() && !new File(weDir, "wallpaper64.exe").exists()) {
                if (weDir.getName().equalsIgnoreCase("steamapps")) {
                    File commonWe = new File(weDir, "common/wallpaper_engine");
                    if (new File(commonWe, "wallpaper32.exe").exists() || new File(commonWe, "wallpaper64.exe").exists()) {
                        weDir = commonWe;
                        break;
                    }
                }
                weDir = weDir.getParentFile();
            }
            if (weDir == null || (!new File(weDir, "wallpaper32.exe").exists() && !new File(weDir, "wallpaper64.exe").exists())) {
                weDir = new File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\wallpaper_engine");
            }
            if (!weDir.exists()) {
                System.err.println("Could not find wallpaper_engine directory from path: " + projectPath);
                return;
            }
            
            File weExe = new File(weDir, "wallpaper32.exe");
            if (!weExe.exists()) {
                weExe = new File(weDir, "wallpaper64.exe");
            }
            
            if (weExe.exists()) {
                ProcessBuilder pb = new ProcessBuilder(
                    weExe.getAbsolutePath(),
                    "-control", "openWallpaper",
                    "-file", projectPath,
                    "-playInWindow", "WEMinecraft",
                    "-width", String.valueOf(frameWidth),
                    "-height", String.valueOf(frameHeight),
                    "-borderless"
                );
                weProcess = pb.start();
                long pid = weProcess.pid();
                
                // Force play command to prevent WE from pausing
                ProcessBuilder playPb = new ProcessBuilder(
                    weExe.getAbsolutePath(),
                    "-control", "play"
                );
                playPb.start();
                
                // Wait and find HWND by window title
                weHwnd = null;
                long timeout = System.currentTimeMillis() + 8000;
                while (weHwnd == null && System.currentTimeMillis() < timeout) {
                    weHwnd = User32.INSTANCE.FindWindow(null, "WEMinecraft");
                    if (weHwnd != null) {
                        System.out.println("Found Wallpaper Engine HWND, applying DSH park method!");
                        
                        int virtualLeft = com.sun.jna.platform.win32.User32.INSTANCE.GetSystemMetrics(76); // SM_XVIRTUALSCREEN
                        int virtualTop = com.sun.jna.platform.win32.User32.INSTANCE.GetSystemMetrics(77);  // SM_YVIRTUALSCREEN
                        int virtualWidth = com.sun.jna.platform.win32.User32.INSTANCE.GetSystemMetrics(78); // SM_CXVIRTUALSCREEN
                        int virtualHeight = com.sun.jna.platform.win32.User32.INSTANCE.GetSystemMetrics(79); // SM_CYVIRTUALSCREEN
                        
                        int parkX = virtualLeft + virtualWidth - 1;
                        int parkY = virtualTop + virtualHeight - 1;
                        
                        int GWL_STYLE = -16;
                        int WS_CAPTION = 0x00C00000;
                        int WS_THICKFRAME = 0x00040000;
                        int style = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(weHwnd, GWL_STYLE);
                        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowLong(weHwnd, GWL_STYLE, style & ~WS_CAPTION & ~WS_THICKFRAME);
                        
                        int GWL_EXSTYLE = -20;
                        int WS_EX_APPWINDOW = 0x00040000;
                        int WS_EX_TOOLWINDOW = 0x00000080;
                        int exStyle = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(weHwnd, GWL_EXSTYLE);
                        int toolStyle = (exStyle & ~WS_EX_APPWINDOW) | WS_EX_TOOLWINDOW;
                        
                        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowLong(weHwnd, GWL_EXSTYLE, toolStyle);
                        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowPos(weHwnd, null, parkX, parkY, frameWidth, frameHeight, 0x0414);
                        break;
                    }
                    Thread.sleep(200);
                }
                
                if (weHwnd != null) {
                    System.out.println("Found Wallpaper Engine HWND by Title: " + weHwnd);
                    isRunning.set(true);
                    startCaptureThread();
                } else {
                    System.err.println("Could not find WE window with title WEMinecraft (PID: " + pid + ")");
                }
            } else {
                System.err.println("Could not find wallpaper32.exe or wallpaper64.exe");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void stopWallpaper() {
        isRunning.set(false);
        if (weProcess != null && weProcess.isAlive()) {
            weProcess.destroy();
        }
        weProcess = null;
        weHwnd = null;
        latestFrame.set(null);
    }
    
    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    captureWindow();
                    Thread.sleep(1000 / Math.max(1, targetFps));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.setName("WECaptureThread");
        captureThread.start();
    }
    
    private void captureWindow() {
        if (weHwnd == null) return;
        
        HDC hdcWindow = User32.INSTANCE.GetDC(weHwnd);
        HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
        HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, frameWidth, frameHeight);
        HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
        
        // 3 = PW_CLIENTONLY | PW_RENDERFULLCONTENT
        boolean printSuccess = User32Ext.INSTANCE.PrintWindow(weHwnd, hdcMemDC, 3);
        
        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader.biWidth = frameWidth;
        bmi.bmiHeader.biHeight = -frameHeight; // top-down
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
        
        if (printSuccess) {
            com.sun.jna.Memory memory = new com.sun.jna.Memory(frameWidth * frameHeight * 4L);
            int getResult = GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, frameHeight, memory, bmi, WinGDI.DIB_RGB_COLORS);
            
            if (getResult != 0) {
                int[] pixels = memory.getIntArray(0, frameWidth * frameHeight);
                latestFrame.set(pixels);
                frameUpdated = true;
            }
        }
        
        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
        GDI32.INSTANCE.DeleteObject(hBitmap);
        GDI32.INSTANCE.DeleteDC(hdcMemDC);
        User32.INSTANCE.ReleaseDC(weHwnd, hdcWindow);
    }
    
    public int[] getLatestFrame() { return latestFrame.get(); }
    public boolean isFrameUpdated() { 
        boolean res = frameUpdated; 
        frameUpdated = false; 
        return res; 
    }
    public int getWidth() { return frameWidth; }
    public int getHeight() { return frameHeight; }
}

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
    private boolean frameUpdated = false;
    
    private WallpaperEngineManager() {}
    
    public static WallpaperEngineManager getInstance() {
        if (instance == null) {
            instance = new WallpaperEngineManager();
        }
        return instance;
    }
    
    public void setWallpaperPath(String path) {
        this.wallpaperPath = path;
    }
    
    public String getWallpaperPath() {
        return this.wallpaperPath;
    }
    
    public interface User32Ext extends User32 {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class);
        boolean PrintWindow(HWND hwnd, HDC hdcBlt, int nFlags);
    }
    
    public void startWallpaper(String projectPath) {
        stopWallpaper();
        try {
            File weDir = new File(projectPath).getParentFile();
            while (weDir != null && !new File(weDir, "wallpaper32.exe").exists() && !new File(weDir, "wallpaper64.exe").exists()) {
                weDir = weDir.getParentFile();
            }
            if (weDir == null) {
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
                    "-height", String.valueOf(frameHeight)
                );
                weProcess = pb.start();
                long pid = weProcess.pid();
                
                // Wait and find HWND by window title
                weHwnd = null;
                long timeout = System.currentTimeMillis() + 8000;
                while (weHwnd == null && System.currentTimeMillis() < timeout) {
                    weHwnd = User32.INSTANCE.FindWindow(null, "WEMinecraft");
                    if (weHwnd == null) {
                        Thread.sleep(200);
                    }
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
    }
    
    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    captureWindow();
                    Thread.sleep(33); // ~30 FPS target
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
        
        // PW_RENDERFULLCONTENT = 2 (allows capturing hardware accelerated windows)
        User32Ext.INSTANCE.PrintWindow(weHwnd, hdcMemDC, 2);
        
        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader.biWidth = frameWidth;
        bmi.bmiHeader.biHeight = -frameHeight; // top-down
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
        
        com.sun.jna.Memory memory = new com.sun.jna.Memory(frameWidth * frameHeight * 4L);
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, frameHeight, memory, bmi, WinGDI.DIB_RGB_COLORS);
        
        int[] pixels = memory.getIntArray(0, frameWidth * frameHeight);
        latestFrame.set(pixels);
        frameUpdated = true;
        
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

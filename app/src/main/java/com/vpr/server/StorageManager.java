package com.vpr.server;

import android.content.Context;
import android.os.StatFs;
import java.io.File;

public class StorageManager {

    private final String VPR_ROOT;
    private final Context ctx;

    public StorageManager(Context ctx) {
        this.ctx = ctx;
        // Pakai internal app storage - tidak butuh permission
        VPR_ROOT = ctx.getFilesDir().getAbsolutePath() + "/vpr";
        initFolders();
    }

    private void initFolders() {
        String[] dirs = {
            VPR_ROOT,
            VPR_ROOT + "/bots",
            VPR_ROOT + "/scripts",
            VPR_ROOT + "/apps",
            VPR_ROOT + "/logs",
            VPR_ROOT + "/storage",
            VPR_ROOT + "/data",
            VPR_ROOT + "/backup"
        };
        for (String d : dirs) {
            try { new File(d).mkdirs(); } catch (Exception ignored) {}
        }
    }

    public long getTotalBytes() {
        try {
            StatFs stat = new StatFs(ctx.getFilesDir().getPath());
            return stat.getTotalBytes();
        } catch (Exception e) { return 0; }
    }

    public long getFreeBytes() {
        try {
            StatFs stat = new StatFs(ctx.getFilesDir().getPath());
            return stat.getAvailableBytes();
        } catch (Exception e) { return 0; }
    }

    public long getVPRUsedBytes() {
        return getFolderSize(new File(VPR_ROOT));
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            size += f.isDirectory() ? getFolderSize(f) : f.length();
        }
        return size;
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int idx = 0;
        double val = bytes;
        while (val >= 1024 && idx < units.length - 1) {
            val /= 1024; idx++;
        }
        return String.format("%.1f %s", val, units[idx]);
    }

    public String getStorageInfo() {
        long total   = getTotalBytes();
        long free    = getFreeBytes();
        long used    = total - free;
        long vprUsed = getVPRUsedBytes();
        return "[ VPR Storage Info ]\n" +
               "─────────────────────────\n" +
               "Total HP      : " + formatSize(total) + "\n" +
               "Terpakai      : " + formatSize(used) + "\n" +
               "Tersisa       : " + formatSize(free) + "\n" +
               "VPR pakai     : " + formatSize(vprUsed) + "\n" +
               "VPR tersedia  : " + formatSize(free) + "\n" +
               "─────────────────────────\n" +
               "Path          : " + VPR_ROOT + "\n";
    }

    public boolean hasEnoughSpace(long required) {
        return getFreeBytes() > required + (100 * 1024 * 1024);
    }

    public String getVPRRoot() { return VPR_ROOT; }
    public String getBotsDir()    { return VPR_ROOT + "/bots"; }
    public String getScriptsDir() { return VPR_ROOT + "/scripts"; }
    public String getAppsDir()    { return VPR_ROOT + "/apps"; }
    public String getStorageDir() { return VPR_ROOT + "/storage"; }
    public String getLogsDir()    { return VPR_ROOT + "/logs"; }
}

package com.vpr.server;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;

public class StorageManager {

    private static final String VPR_ROOT = "/sdcard/vpr";
    private final Context ctx;

    public StorageManager(Context ctx) {
        this.ctx = ctx;
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

    // Total storage fisik HP
    public long getTotalBytes() {
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return stat.getTotalBytes();
        } catch (Exception e) { return 0; }
    }

    // Storage bebas tersisa di HP
    public long getFreeBytes() {
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return stat.getAvailableBytes();
        } catch (Exception e) { return 0; }
    }

    // Total yang dipakai oleh VPR
    public long getVPRUsedBytes() {
        return getFolderSize(new File(VPR_ROOT));
    }

    // Hitung ukuran folder rekursif
    private long getFolderSize(File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) size += getFolderSize(f);
            else size += f.length();
        }
        return size;
    }

    // Format bytes ke string readable
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = 0;
        double val = bytes;
        while (val >= 1024 && idx < units.length - 1) {
            val /= 1024;
            idx++;
        }
        return String.format("%.1f %s", val, units[idx]);
    }

    // Info storage lengkap
    public String getStorageInfo() {
        long total = getTotalBytes();
        long free  = getFreeBytes();
        long used  = total - free;
        long vprUsed = getVPRUsedBytes();
        long vprFree = free;

        return "[ VPR Storage Info ]\n" +
               "─────────────────────────\n" +
               "Total HP      : " + formatSize(total) + "\n" +
               "Terpakai      : " + formatSize(used) + "\n" +
               "Tersisa       : " + formatSize(free) + "\n" +
               "VPR pakai     : " + formatSize(vprUsed) + "\n" +
               "VPR tersedia  : " + formatSize(vprFree) + "\n" +
               "─────────────────────────\n" +
               "Path          : " + VPR_ROOT + "\n";
    }

    // Cek apakah masih ada ruang (minimal 100MB)
    public boolean hasEnoughSpace(long requiredBytes) {
        return getFreeBytes() > requiredBytes + (100 * 1024 * 1024);
    }

    public String getVPRRoot() { return VPR_ROOT; }
}

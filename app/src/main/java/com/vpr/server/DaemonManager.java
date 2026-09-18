package com.vpr.server;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;

public class DaemonManager {

    private static final String TAG = "VPR-Daemon";
    private boolean running = false;
    private Thread thread;
    private final Map<String, Process> procs = new HashMap<>();
    private final StorageManager sm;

    public DaemonManager(Context ctx) {
        this.sm = new StorageManager(ctx);
    }

    // Cari binary di semua lokasi
    public static String findBinary(String... names) {
        String[] searchPaths = {
            "/data/data/com.termux/files/usr/bin/",
            "/data/data/com.termux/files/usr/local/bin/",
            "/system/bin/",
            "/system/xbin/",
            "/sbin/",
            "/usr/bin/",
            "/usr/local/bin/"
        };
        for (String name : names) {
            // Cek path lengkap dulu
            if (name.startsWith("/") && new File(name).exists() && new File(name).canExecute())
                return name;
            // Cek di semua search path
            for (String path : searchPaths) {
                File f = new File(path + name);
                if (f.exists() && f.canExecute()) return f.getAbsolutePath();
            }
        }
        return null;
    }

    public static String getPython() {
        String found = findBinary("python3", "python", "python3.11", "python3.10", "python3.9");
        return found != null ? found : null;
    }

    public static String getNode() {
        String found = findBinary("node", "nodejs", "node18", "node16");
        return found != null ? found : null;
    }

    public static String getShell() {
        String found = findBinary("bash", "sh", "ash", "dash");
        return found != null ? found : "/system/bin/sh";
    }

    public static boolean isTermuxInstalled() {
        return new File("/data/data/com.termux/files/usr/bin/").exists();
    }

    public void start() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    scan(sm.getBotsDir());
                    scan(sm.getScriptsDir());
                    scan(sm.getAppsDir());
                    Thread.sleep(30000);
                } catch (InterruptedException e) { break; }
            }
        });
        thread.start();
        Log.d(TAG, "Daemon started. Root: " + sm.getVPRRoot());
    }

    private void scan(String dir) {
        File folder = new File(dir);
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            String key  = f.getAbsolutePath();
            String name = f.getName();
            Process ex  = procs.get(key);
            if (ex != null) {
                try { ex.exitValue(); procs.remove(key); }
                catch (IllegalThreadStateException e) { continue; }
            }

            try {
                Process p = buildProcess(f);
                if (p != null) {
                    procs.put(key, p);
                    pipeLog(p, new File(sm.getLogsDir(), name + ".log"));
                    Log.d(TAG, "Started: " + name);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed: " + name + " " + e.getMessage());
            }
        }
    }

    public static Process buildProcess(File f) throws IOException {
        String name = f.getName();
        String path = f.getAbsolutePath();
        ProcessBuilder pb = null;

        if (name.endsWith(".sh")) {
            String shell = getShell();
            pb = new ProcessBuilder(shell, path);
        } else if (name.endsWith(".py")) {
            String python = getPython();
            if (python == null) return null;
            pb = new ProcessBuilder(python, path);
        } else if (name.endsWith(".js")) {
            String node = getNode();
            if (node == null) return null;
            pb = new ProcessBuilder(node, path);
        }

        if (pb != null) {
            pb.redirectErrorStream(true);
            pb.directory(f.getParentFile());
            return pb.start();
        }
        return null;
    }

    private void pipeLog(Process p, File log) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                 FileWriter fw = new FileWriter(log, true)) {
                String line;
                while ((line = br.readLine()) != null) {
                    fw.write("[VPR] " + line + "\n");
                    fw.flush();
                }
            } catch (IOException ignored) {}
        }).start();
    }

    public void stop() {
        running = false;
        for (Process p : procs.values()) {
            try { p.destroy(); } catch (Exception ignored) {}
        }
        procs.clear();
        if (thread != null) thread.interrupt();
    }
}

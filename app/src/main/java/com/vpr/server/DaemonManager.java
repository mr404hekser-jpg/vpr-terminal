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

    // Cari Python/Node di berbagai lokasi
    private static final String[] PYTHON_PATHS = {
        "/data/data/com.termux/files/usr/bin/python3",
        "/data/data/com.termux/files/usr/bin/python",
        "/system/bin/python3",
        "/system/bin/python",
        "python3",
        "python"
    };

    private static final String[] NODE_PATHS = {
        "/data/data/com.termux/files/usr/bin/node",
        "/data/data/com.termux/files/usr/bin/nodejs",
        "/system/bin/node",
        "node"
    };

    public DaemonManager(Context ctx) {
        this.sm = new StorageManager(ctx);
    }

    public static String findPython() {
        for (String p : PYTHON_PATHS) {
            if (p.startsWith("/")) {
                if (new File(p).exists()) return p;
            } else { return p; }
        }
        return "python3";
    }

    public static String findNode() {
        for (String p : NODE_PATHS) {
            if (p.startsWith("/")) {
                if (new File(p).exists()) return p;
            } else { return p; }
        }
        return "node";
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
        Log.d(TAG, "Daemon started. VPR root: " + sm.getVPRRoot());
    }

    private void scan(String dir) {
        File folder = new File(dir);
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            String key  = f.getAbsolutePath();
            String name = f.getName();

            // Cek apakah masih jalan
            Process ex = procs.get(key);
            if (ex != null) {
                try { ex.exitValue(); procs.remove(key); }
                catch (IllegalThreadStateException e) { continue; }
            }

            try {
                Process p = null;
                File log = new File(sm.getLogsDir(), name + ".log");

                if (name.endsWith(".sh")) {
                    p = new ProcessBuilder("sh", key)
                        .redirectErrorStream(true).start();
                } else if (name.endsWith(".py")) {
                    p = new ProcessBuilder(findPython(), key)
                        .redirectErrorStream(true).start();
                } else if (name.endsWith(".js")) {
                    p = new ProcessBuilder(findNode(), key)
                        .redirectErrorStream(true).start();
                }

                if (p != null) {
                    procs.put(key, p);
                    pipeLog(p, log);
                    Log.d(TAG, "Started: " + name);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed: " + name + " " + e.getMessage());
            }
        }
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

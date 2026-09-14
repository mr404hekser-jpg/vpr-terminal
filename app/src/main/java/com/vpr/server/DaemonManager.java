package com.vpr.server;
import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;

public class DaemonManager {
    private static final String TAG = "VPR-Daemon";
    private boolean running = false;
    private Thread thread;
    private Map<String, Process> procs = new HashMap<>();
    private String BOT, SCRIPT, APP, LOG;

    public DaemonManager(Context ctx) {
        String root = "/sdcard/vpr";
        BOT    = root + "/bots";
        SCRIPT = root + "/scripts";
        APP    = root + "/apps";
        LOG    = root + "/logs";
        new File(BOT).mkdirs();
        new File(SCRIPT).mkdirs();
        new File(APP).mkdirs();
        new File(LOG).mkdirs();
        new File(root + "/storage").mkdirs();
        new File(root + "/data").mkdirs();
    }

    public void start() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    scan(BOT);
                    scan(SCRIPT);
                    scan(APP);
                    Thread.sleep(30000);
                } catch (InterruptedException e) { break; }
            }
        });
        thread.start();
        Log.d(TAG, "Daemon started");
    }

    private void scan(String dir) {
        File folder = new File(dir);
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            String name = f.getName();
            String key  = f.getAbsolutePath();

            // Cek apakah proses masih hidup
            Process existing = procs.get(key);
            if (existing != null) {
                try {
                    existing.exitValue();
                    // Kalau bisa dapat exitValue = sudah mati → restart
                    procs.remove(key);
                } catch (IllegalThreadStateException e) {
                    continue; // Masih jalan
                }
            }

            try {
                Process p = null;
                File log = new File(LOG, name + ".log");

                if (name.endsWith(".sh")) {
                    p = new ProcessBuilder("sh", key)
                        .redirectErrorStream(true)
                        .start();
                } else if (name.endsWith(".py")) {
                    p = new ProcessBuilder(
                        "/data/data/com.termux/files/usr/bin/python3", key)
                        .redirectErrorStream(true)
                        .start();
                } else if (name.endsWith(".js")) {
                    p = new ProcessBuilder(
                        "/data/data/com.termux/files/usr/bin/node", key)
                        .redirectErrorStream(true)
                        .start();
                }

                if (p != null) {
                    procs.put(key, p);
                    pipeLog(p, log);
                    Log.d(TAG, "Started: " + name);
                }

            } catch (IOException e) {
                Log.e(TAG, "Failed: " + name + " - " + e.getMessage());
            }
        }
    }

    private void pipeLog(Process p, File log) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
        for (Process p : procs.values()) p.destroy();
        procs.clear();
        if (thread != null) thread.interrupt();
    }
}

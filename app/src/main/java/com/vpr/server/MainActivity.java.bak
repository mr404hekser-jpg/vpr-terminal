package com.vpr.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout sessionTabBar;
    private FrameLayout terminalContainer;
    private EditText cmdInput;
    private Button btnSend, btnUpload, btnNewSession;
    private ImageButton btnClearSession;
    private List<SessionView> sessions = new ArrayList<>();
    private int activeSession = 0;
    private int sessionCount = 0;
    private SharedPreferences prefs;
    private Handler mainHandler;
    private StorageManager storage;
    private static final int FILE_PICK = 100;

    private static final String ASCII =
        "  ██╗   ██╗██████╗ ██████╗ \n" +
        "  ██║   ██║██╔══██╗██╔══██╗\n" +
        "  ██║   ██║██████╔╝██████╔╝\n" +
        "  ╚██╗ ██╔╝██╔═══╝ ██╔══██╗\n" +
        "   ╚████╔╝ ██║     ██║  ██║\n" +
        "    ╚═══╝  ╚═╝     ╚═╝  ╚═╝\n" +
        "        S E R V E R  v2.1\n\n" +
        "  Welcome to VPR Server\n" +
        "  Ketik 'storage' untuk info penyimpanan\n" +
        "  Ketik 'help' untuk daftar perintah\n" +
        "  ─────────────────────────────────\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            setContentView(R.layout.activity_main);

            mainHandler = new Handler(Looper.getMainLooper());
            prefs = getSharedPreferences("vpr_sessions", MODE_PRIVATE);
            storage = new StorageManager(this);

            sessionTabBar     = findViewById(R.id.sessionTabBar);
            terminalContainer = findViewById(R.id.terminalContainer);
            cmdInput          = findViewById(R.id.cmdInput);
            btnSend           = findViewById(R.id.btnSend);
            btnUpload         = findViewById(R.id.btnUpload);
            btnNewSession     = findViewById(R.id.btnNewSession);
            btnClearSession   = findViewById(R.id.btnClearSession);

            startService(new Intent(this, VPRService.class));

            int saved = prefs.getInt("session_count", 0);
            if (saved == 0) {
                createSession();
            } else {
                for (int i = 1; i <= saved; i++) restoreSession(i);
                switchSession(0);
            }

            btnSend.setOnClickListener(v -> runCommand());
            btnUpload.setOnClickListener(v -> pickFile());
            btnNewSession.setOnClickListener(v -> createSession());
            btnClearSession.setOnClickListener(v -> confirmClear());
            cmdInput.setOnEditorActionListener((v, a, e) -> { runCommand(); return true; });

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void createSession() {
        try {
            sessionCount++;
            SessionView sv = new SessionView(this, sessionCount, prefs);
            sessions.add(sv);
            addTab(sessions.size() - 1);
            switchSession(sessions.size() - 1);
            sv.print(ASCII);
            // Tampilkan info storage real saat buka session
            sv.print(storage.getStorageInfo() + "\n");
            prefs.edit().putInt("session_count", sessionCount).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void restoreSession(int id) {
        try {
            sessionCount = Math.max(sessionCount, id);
            SessionView sv = new SessionView(this, id, prefs);
            sessions.add(sv);
            addTab(sessions.size() - 1);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addTab(int idx) {
        Button tab = new Button(this);
        tab.setText("S" + (idx + 1));
        tab.setTextColor(0xFF2196F3);
        tab.setBackgroundColor(0xFF1A1A1A);
        tab.setPadding(24, 8, 24, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(4, 0, 4, 0);
        tab.setLayoutParams(lp);
        tab.setOnClickListener(v -> switchSession(idx));
        sessionTabBar.addView(tab);
    }

    private void switchSession(int idx) {
        try {
            if (idx < 0 || idx >= sessions.size()) return;
            activeSession = idx;
            terminalContainer.removeAllViews();
            terminalContainer.addView(sessions.get(idx).getView());
            for (int i = 0; i < sessionTabBar.getChildCount(); i++) {
                android.view.View v = sessionTabBar.getChildAt(i);
                if (v instanceof Button)
                    ((Button) v).setBackgroundColor(i == idx ? 0xFF0D3A5C : 0xFF1A1A1A);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runCommand() {
        try {
            String cmd = cmdInput.getText().toString().trim();
            if (cmd.isEmpty()) return;
            cmdInput.setText("");

            // Built-in commands
            switch (cmd.toLowerCase()) {
                case "storage":
                case "df":
                    sessions.get(activeSession).print("\n" + storage.getStorageInfo() + "\n");
                    return;
                case "help":
                    sessions.get(activeSession).print(
                        "\n[ VPR Commands ]\n" +
                        "─────────────────\n" +
                        "storage / df     — Info penyimpanan real\n" +
                        "ls [path]        — List file\n" +
                        "ls bots          — List file di bots/\n" +
                        "ls scripts       — List file di scripts/\n" +
                        "clear            — Bersihkan terminal\n" +
                        "ps               — Lihat proses aktif\n" +
                        "[file.py]        — Jalankan Python\n" +
                        "[file.sh]        — Jalankan Shell\n" +
                        "[file.js]        — Jalankan Node.js\n" +
                        "─────────────────\n\n"
                    );
                    return;
                case "clear":
                    sessions.get(activeSession).clear();
                    sessions.get(activeSession).print(ASCII);
                    return;
                case "ls bots":
                    listDir("/sdcard/vpr/bots/");
                    return;
                case "ls scripts":
                    listDir("/sdcard/vpr/scripts/");
                    return;
                case "ls apps":
                    listDir("/sdcard/vpr/apps/");
                    return;
                case "ls storage":
                    listDir("/sdcard/vpr/storage/");
                    return;
                case "ps":
                    exec("ps aux");
                    return;
            }

            // Security Tools
            if (cmd.startsWith("scan ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 4) {
                    String h = p[1];
                    int s = Integer.parseInt(p[2]);
                    int e2 = Integer.parseInt(p[3]);
                    SessionView sv2 = sessions.get(activeSession);
                    sv2.print("[VPR] Scanning " + h + " port " + s + "-" + e2 + "...\n");
                    new Thread(() -> {
                        String res = SecurityTools.portScan(h, s, e2);
                        mainHandler.post(() -> sv2.print(res));
                    }).start();
                } else {
                    sessions.get(activeSession).print("Usage: scan [host] [start] [end]\n");
                }
                return;
            }
            if (cmd.startsWith("recon ")) {
                String host = cmd.substring(6).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.hostInfo(host);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("headers ")) {
                String url = cmd.substring(8).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.headerGrab(url);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("cve ")) {
                String url = cmd.substring(4).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.cveCheck(url);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("xss ")) {
                String url = cmd.substring(4).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.xssTest(url);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("stress ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 4) {
                    String url = p[1];
                    int t = Integer.parseInt(p[2]);
                    int r2 = Integer.parseInt(p[3]);
                    SessionView sv2 = sessions.get(activeSession);
                    new Thread(() -> SecurityTools.stressTest(url, t, r2, sv2, mainHandler)).start();
                } else {
                    sessions.get(activeSession).print("Usage: stress [url] [threads] [requests]\n");
                }
                return;
            }
            if (cmd.startsWith("dns ")) {
                String host = cmd.substring(4).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.dnsLookup(host);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("sqli ")) {
                String url = cmd.substring(5).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.sqlTest(url);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("dirbr ")) {
                String url = cmd.substring(6).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> SecurityTools.dirBrute(url, sv2, mainHandler)).start();
                return;
            }
            if (cmd.startsWith("banner ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 3) {
                    String h = p[1];
                    int port = Integer.parseInt(p[2]);
                    SessionView sv2 = sessions.get(activeSession);
                    new Thread(() -> {
                        String res = SecurityTools.bannerGrab(h, port);
                        mainHandler.post(() -> sv2.print(res));
                    }).start();
                } else {
                    sessions.get(activeSession).print("Usage: banner [host] [port]\n");
                }
                return;
            }
            if (cmd.startsWith("ping ")) {
                String host = cmd.substring(5).trim();
                SessionView sv2 = sessions.get(activeSession);
                new Thread(() -> {
                    String res = SecurityTools.ping(host);
                    mainHandler.post(() -> sv2.print(res));
                }).start();
                return;
            }
            if (cmd.startsWith("ls")) {
                exec(cmd);
                return;
            }

            exec(cmd);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void listDir(String path) {
        SessionView sv = sessions.get(activeSession);
        File dir = new File(path);
        sv.print("\n[ " + path + " ]\n");
        if (!dir.exists() || dir.listFiles() == null) {
            sv.print("  (kosong)\n\n");
            return;
        }
        for (File f : dir.listFiles()) {
            String size = StorageManager.formatSize(f.length());
            sv.print("  " + f.getName() + "  (" + size + ")\n");
        }
        sv.print("\n");
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Session?")
            .setMessage("Isi session ini akan dihapus.")
            .setPositiveButton("Hapus", (d, w) -> {
                if (!sessions.isEmpty()) sessions.get(activeSession).clear();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void exec(String cmd) {
        if (sessions.isEmpty()) return;
        SessionView sv = sessions.get(activeSession);
        sv.print("\n$ " + cmd + "\n");

        new Thread(() -> {
            try {
                String[] fc;
                String botPath    = "/sdcard/vpr/bots/" + cmd;
                String scriptPath = "/sdcard/vpr/scripts/" + cmd;

                if (cmd.endsWith(".py")) {
                    String path = new File(botPath).exists() ? botPath : scriptPath;
                    fc = new String[]{"/data/data/com.termux/files/usr/bin/python3", path};
                } else if (cmd.endsWith(".js")) {
                    String path = new File(botPath).exists() ? botPath : scriptPath;
                    fc = new String[]{"/data/data/com.termux/files/usr/bin/node", path};
                } else if (cmd.endsWith(".sh")) {
                    String path = new File(botPath).exists() ? botPath : scriptPath;
                    fc = new String[]{"sh", path};
                } else {
                    fc = new String[]{"sh", "-c", cmd};
                }

                ProcessBuilder pb = new ProcessBuilder(fc);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                char[] buf = new char[512];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    mainHandler.post(() -> sv.print(out));
                }

                int exit = p.waitFor();
                mainHandler.post(() -> sv.print("\n[exit:" + exit + "]\n"));

            } catch (Exception e) {
                mainHandler.post(() -> sv.print("[ERROR] " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void pickFile() {
        try {
            // Cek storage dulu
            if (!storage.hasEnoughSpace(1024 * 1024)) {
                sessions.get(activeSession).print(
                    "\n[VPR] ⚠ Storage hampir penuh!\n" +
                    storage.getStorageInfo() + "\n"
                );
                return;
            }
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "Pilih File"), FILE_PICK);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        try {
            if (req == FILE_PICK && res == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                String name = getFileName(uri);
                if (name != null) copyFileTo(uri, name);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getFileName(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (idx >= 0) return c.getString(idx);
                        }
                    } finally { c.close(); }
                }
            }
            return uri.getLastPathSegment();
        } catch (Exception e) {
            return "file_" + System.currentTimeMillis();
        }
    }

    private void copyFileTo(Uri uri, String name) {
        if (sessions.isEmpty()) return;
        SessionView sv = sessions.get(activeSession);
        new Thread(() -> {
            try {
                String destDir = name.endsWith(".py") || name.endsWith(".js")
                    ? "/sdcard/vpr/bots/"
                    : name.endsWith(".sh")
                    ? "/sdcard/vpr/scripts/"
                    : "/sdcard/vpr/storage/";

                new File(destDir).mkdirs();
                File dest = new File(destDir, name);

                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buf = new byte[8192]; int len;
                long total = 0;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    total += len;
                }
                in.close(); out.close();

                final long fileSize = total;
                mainHandler.post(() -> {
                    sv.print("\n[VPR] ✓ Upload: " + name + "\n");
                    sv.print("[VPR] Ukuran  : " + StorageManager.formatSize(fileSize) + "\n");
                    sv.print("[VPR] Disimpan: " + destDir + name + "\n");
                    sv.print("[VPR] Sisa HP : " + StorageManager.formatSize(storage.getFreeBytes()) + "\n\n");

                    if (name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".sh")) {
                        new AlertDialog.Builder(this)
                            .setTitle("Jalankan " + name + "?")
                            .setPositiveButton("Ya", (d, w) -> exec(name))
                            .setNegativeButton("Nanti", null)
                            .show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> sv.print("[ERROR] Upload gagal: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (SessionView sv : sessions) {
            try { sv.save(); } catch (Exception ignored) {}
        }
    }
}
// patch marker - security tools integrated via exec commands

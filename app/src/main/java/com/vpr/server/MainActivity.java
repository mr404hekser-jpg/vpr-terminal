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
import android.view.View;
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

    private String getASCII() {
        String pythonStatus = DaemonManager.getPython() != null
            ? DaemonManager.getPython() : "NOT FOUND - install Termux + Python";
        String nodeStatus = DaemonManager.getNode() != null
            ? DaemonManager.getNode() : "NOT FOUND - install Termux + Node";
        String shellStatus = DaemonManager.getShell();

        return
        "  VPR SERVER  v2.1\n" +
        "  Virtual Persistent Runtime\n" +
        "  ---\n" +
        "  Shell  : " + shellStatus + "\n" +
        "  Python : " + pythonStatus + "\n" +
        "  Node   : " + nodeStatus + "\n" +
        "  Storage: " + storage.getVPRRoot() + "\n" +
        "  ---\n" +
        "  Ketik \'help\' untuk daftar perintah\n\n";
    }

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
            prefs   = getSharedPreferences("vpr_sessions", MODE_PRIVATE);
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
            cmdInput.setOnEditorActionListener((v, a, e) -> {
                runCommand(); return true;
            });

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void createSession() {
        try {
            sessionCount++;
            SessionView sv = new SessionView(this, sessionCount, prefs);
            sessions.add(sv);
            addTab(sessions.size() - 1);
            switchSession(sessions.size() - 1);
            sv.print(getASCII());
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
                View v = sessionTabBar.getChildAt(i);
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
            if (sessions.isEmpty()) return;
            SessionView sv = sessions.get(activeSession);

            switch (cmd.toLowerCase()) {
                case "help":
                    sv.print("\n[ VPR Server Commands ]\n" +
                        "---\n" +
                        "  storage / df     - Info storage\n" +
                        "  ls bots          - List bots\n" +
                        "  ls scripts       - List scripts\n" +
                        "  ls apps          - List apps\n" +
                        "  ls storage       - List storage\n" +
                        "  clear            - Bersihkan terminal\n" +
                        "  ps               - Proses aktif\n" +
                        "  env              - Cek runtime tersedia\n" +
                        "  [file.py]        - Jalankan Python\n" +
                        "  [file.sh]        - Jalankan Shell\n" +
                        "  [file.js]        - Jalankan Node.js\n" +
                        "---\n\n");
                    return;

                case "storage": case "df":
                    sv.print("\n" + storage.getStorageInfo() + "\n");
                    return;

                case "clear":
                    sv.clear(); sv.print(getASCII()); return;

                case "env":
                    sv.print("\n[ Runtime Environment ]\n");
                    sv.print("  Shell  : " + DaemonManager.getShell() + "\n");
                    String py = DaemonManager.getPython();
                    sv.print("  Python : " + (py != null ? py : "NOT FOUND") + "\n");
                    String nd = DaemonManager.getNode();
                    sv.print("  Node   : " + (nd != null ? nd : "NOT FOUND") + "\n");
                    sv.print("  Termux : " + (DaemonManager.isTermuxInstalled() ? "Installed" : "NOT installed") + "\n\n");
                    return;

                case "ls bots":    listDir(storage.getBotsDir(), sv); return;
                case "ls scripts": listDir(storage.getScriptsDir(), sv); return;
                case "ls apps":    listDir(storage.getAppsDir(), sv); return;
                case "ls storage": listDir(storage.getStorageDir(), sv); return;
                case "ps":         exec("ps", sv); return;
            }

            if (cmd.startsWith("ls")) { exec(cmd, sv); return; }
            exec(cmd, sv);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void listDir(String path, SessionView sv) {
        File dir = new File(path);
        sv.print("\n[ " + path + " ]\n");
        if (!dir.exists() || dir.listFiles() == null) {
            sv.print("  (kosong)\n\n"); return;
        }
        for (File f : dir.listFiles()) {
            sv.print("  " + f.getName() + "  (" +
                StorageManager.formatSize(f.length()) + ")\n");
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

    private void exec(String cmd, SessionView sv) {
        sv.print("\n$ " + cmd + "\n");
        new Thread(() -> {
            try {
                ProcessBuilder pb;
                String botsPath    = storage.getBotsDir() + "/" + cmd;
                String scriptsPath = storage.getScriptsDir() + "/" + cmd;
                String appsPath    = storage.getAppsDir() + "/" + cmd;

                String filePath = new File(botsPath).exists() ? botsPath
                    : new File(scriptsPath).exists() ? scriptsPath
                    : new File(appsPath).exists() ? appsPath
                    : botsPath;

                if (cmd.endsWith(".py")) {
                    String python = DaemonManager.getPython();
                    if (python == null) {
                        mainHandler.post(() -> sv.print(
                            "[ERROR] Python tidak ditemukan.\n" +
                            "Install Termux lalu: pkg install python\n"));
                        return;
                    }
                    pb = new ProcessBuilder(python, filePath);
                } else if (cmd.endsWith(".js")) {
                    String node = DaemonManager.getNode();
                    if (node == null) {
                        mainHandler.post(() -> sv.print(
                            "[ERROR] Node.js tidak ditemukan.\n" +
                            "Install Termux lalu: pkg install nodejs\n"));
                        return;
                    }
                    pb = new ProcessBuilder(node, filePath);
                } else if (cmd.endsWith(".sh")) {
                    pb = new ProcessBuilder(DaemonManager.getShell(), filePath);
                } else {
                    pb = new ProcessBuilder(DaemonManager.getShell(), "-c", cmd);
                }

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
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "Pilih File"), FILE_PICK);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        try {
            if (req == FILE_PICK && res == RESULT_OK
                    && data != null && data.getData() != null) {
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
                    ? storage.getBotsDir()
                    : name.endsWith(".sh")
                    ? storage.getScriptsDir()
                    : storage.getStorageDir();

                new File(destDir).mkdirs();
                File dest = new File(destDir, name);

                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) throw new Exception("Cannot open file");
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buf = new byte[8192];
                int len; long total = 0;
                while ((len = in.read(buf)) > 0) { out.write(buf, 0, len); total += len; }
                in.close(); out.close();

                final long fs = total;
                mainHandler.post(() -> {
                    sv.print("\n[VPR] Upload: " + name + "\n");
                    sv.print("[VPR] Size  : " + StorageManager.formatSize(fs) + "\n");
                    sv.print("[VPR] Path  : " + destDir + "/" + name + "\n");
                    sv.print("[VPR] Sisa  : " + StorageManager.formatSize(storage.getFreeBytes()) + "\n\n");

                    if (name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".sh")) {
                        new AlertDialog.Builder(this)
                            .setTitle("Jalankan " + name + "?")
                            .setPositiveButton("Ya", (d, w) -> exec(name, sv))
                            .setNegativeButton("Nanti", null)
                            .show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> sv.print("[ERROR] Upload: " + e.getMessage() + "\n"));
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

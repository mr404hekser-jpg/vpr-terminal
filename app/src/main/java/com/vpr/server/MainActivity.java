package com.vpr.server;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout sessionTabBar;
    private FrameLayout terminalContainer;
    private EditText cmdInput;
    private Button btnSend, btnUpload, btnNewSession, btnClearSession;
    private List<SessionView> sessions = new ArrayList<>();
    private int activeSession = 0;
    private int sessionCount = 0;
    private SharedPreferences prefs;
    private static final int FILE_PICK = 100;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String ASCII =
        "  ██╗   ██╗██████╗ ██████╗ \n"+
        "  ██║   ██║██╔══██╗██╔══██╗\n"+
        "  ██║   ██║██████╔╝██████╔╝\n"+
        "  ╚██╗ ██╔╝██╔═══╝ ██╔══██╗\n"+
        "   ╚████╔╝ ██║     ██║  ██║\n"+
        "    ╚═══╝  ╚═╝     ╚═╝  ╚═╝\n"+
        "        S E R V E R  v2.1\n\n"+
        "  Welcome to VPR Server\n"+
        "  Storage : 1TB | Sessions : Unlimited\n"+
        "  ─────────────────────────────────\n\n";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("vpr_sessions", MODE_PRIVATE);

        sessionTabBar     = findViewById(R.id.sessionTabBar);
        terminalContainer = findViewById(R.id.terminalContainer);
        cmdInput          = findViewById(R.id.cmdInput);
        btnSend           = findViewById(R.id.btnSend);
        btnUpload         = findViewById(R.id.btnUpload);
        btnNewSession     = findViewById(R.id.btnNewSession);
        btnClearSession   = findViewById(R.id.btnClearSession);

        // Init storage 1TB folder structure
        initStorage();

        // Start VPR Service
        startService(new Intent(this, VPRService.class));

        // Restore sessions atau buat baru
        int savedCount = prefs.getInt("session_count", 0);
        if (savedCount == 0) {
            createSession();
        } else {
            for (int i = 1; i <= savedCount; i++) {
                restoreSession(i);
            }
            switchSession(0);
        }

        btnSend.setOnClickListener(v -> {
            String c = cmdInput.getText().toString().trim();
            if (!c.isEmpty()) { exec(c); cmdInput.setText(""); }
        });

        btnUpload.setOnClickListener(v -> pickFile());
        btnNewSession.setOnClickListener(v -> createSession());
        btnClearSession.setOnClickListener(v -> clearCurrentSession());

        cmdInput.setOnEditorActionListener((v, a, e) -> {
            String c = cmdInput.getText().toString().trim();
            if (!c.isEmpty()) { exec(c); cmdInput.setText(""); }
            return true;
        });
    }

    private void initStorage() {
        // Buat struktur folder 1TB VPR
        String[] dirs = {
            "/sdcard/vpr", "/sdcard/vpr/bots", "/sdcard/vpr/scripts",
            "/sdcard/vpr/apps", "/sdcard/vpr/logs", "/sdcard/vpr/storage",
            "/sdcard/vpr/data", "/sdcard/vpr/backup", "/sdcard/vpr/sessions"
        };
        for (String d : dirs) new File(d).mkdirs();
    }

    private void createSession() {
        sessionCount++;
        String savedContent = prefs.getString("session_" + sessionCount, null);
        SessionView sv = new SessionView(this, sessionCount, prefs);
        sessions.add(sv);
        addSessionTab(sessions.size() - 1);
        switchSession(sessions.size() - 1);
        if (savedContent == null) {
            sv.print(ASCII);
            sv.print("  Session " + sessionCount + " siap.\n\n");
        }
        prefs.edit().putInt("session_count", sessionCount).apply();
    }

    private void restoreSession(int id) {
        sessionCount = Math.max(sessionCount, id);
        SessionView sv = new SessionView(this, id, prefs);
        sessions.add(sv);
        addSessionTab(sessions.size() - 1);
    }

    private void addSessionTab(int idx) {
        Button tab = new Button(this);
        tab.setText("S" + (idx + 1));
        tab.setTextColor(0xFF00FF88);
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
        activeSession = idx;
        terminalContainer.removeAllViews();
        terminalContainer.addView(sessions.get(idx).getView());
        for (int i = 0; i < sessionTabBar.getChildCount(); i++) {
            View v = sessionTabBar.getChildAt(i);
            if (v instanceof Button)
                ((Button) v).setBackgroundColor(i == idx ? 0xFF003322 : 0xFF1A1A1A);
        }
    }

    private void clearCurrentSession() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Session?")
            .setMessage("Isi session ini akan dihapus permanen.")
            .setPositiveButton("Hapus", (d, w) -> {
                sessions.get(activeSession).clear();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void exec(String cmd) {
        SessionView sv = sessions.get(activeSession);
        sv.print("\n$ " + cmd + "\n");

        new Thread(() -> {
            try {
                String[] fc;
                // Deteksi tipe file
                if (cmd.endsWith(".py")) {
                    String path = new File("/sdcard/vpr/bots/" + cmd).exists()
                        ? "/sdcard/vpr/bots/" + cmd : "/sdcard/vpr/scripts/" + cmd;
                    fc = new String[]{"/data/data/com.termux/files/usr/bin/python3", path};
                } else if (cmd.endsWith(".js")) {
                    String path = new File("/sdcard/vpr/bots/" + cmd).exists()
                        ? "/sdcard/vpr/bots/" + cmd : "/sdcard/vpr/scripts/" + cmd;
                    fc = new String[]{"/data/data/com.termux/files/usr/bin/node", path};
                } else if (cmd.endsWith(".sh")) {
                    String path = new File("/sdcard/vpr/bots/" + cmd).exists()
                        ? "/sdcard/vpr/bots/" + cmd : "/sdcard/vpr/scripts/" + cmd;
                    fc = new String[]{"sh", path};
                } else {
                    fc = new String[]{"sh", "-c", cmd};
                }

                ProcessBuilder pb = new ProcessBuilder(fc);
                pb.redirectErrorStream(true); // stdout + stderr jadi satu
                Process p = pb.start();

                // Real-time output stream
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                char[] buf = new char[256];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    mainHandler.post(() -> sv.print(out));
                }

                int exit = p.waitFor();
                mainHandler.post(() -> sv.print("\n[VPR exit:" + exit + "]\n"));

            } catch (Exception e) {
                mainHandler.post(() -> sv.print("[ERROR] " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Pilih File"), FILE_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_PICK && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            copyFile(uri, getName(uri));
        }
    }

    private String getName(Uri uri) {
        String r = null;
        if ("content".equals(uri.getScheme())) {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i >= 0) r = c.getString(i);
                }
            } finally { if (c != null) c.close(); }
        }
        return r != null ? r : uri.getLastPathSegment();
    }

    private void copyFile(Uri uri, String name) {
        SessionView sv = sessions.get(activeSession);
        new Thread(() -> {
            try {
                // Tentukan folder tujuan berdasarkan ekstensi
                String destDir = "/sdcard/vpr/bots/";
                if (name.endsWith(".js")) destDir = "/sdcard/vpr/bots/";
                else if (name.endsWith(".sh")) destDir = "/sdcard/vpr/scripts/";
                else if (!name.endsWith(".py")) destDir = "/sdcard/vpr/storage/";

                new File(destDir).mkdirs();
                File dest = new File(destDir, name);

                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buf = new byte[8192]; int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close(); out.close();

                final String finalDir = destDir;
                mainHandler.post(() -> {
                    sv.print("\n[VPR] ✓ Upload: " + name + "\n");
                    sv.print("[VPR] Path: " + finalDir + name + "\n");
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
        // Auto-save semua session
        for (SessionView sv : sessions) sv.save();
    }
}

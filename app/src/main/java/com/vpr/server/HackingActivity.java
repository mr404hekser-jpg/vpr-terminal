package com.vpr.server;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

public class HackingActivity extends Activity {

    private EditText hackInput;
    private Button btnRun;
    private ImageButton btnBack;
    private FrameLayout container;
    private Handler mainHandler;
    private TextView tv;
    private ScrollView sv;
    private StringBuilder buf = new StringBuilder();

    private static final String MENU =
        "  [ Commands ]\n" +
        "  ─────────────────────────────────\n" +
        "  [ Recon ]\n" +
        "  scan [host] [s] [e]    — Port scanner\n" +
        "  recon [host]           — Host recon + whois\n" +
        "  headers [url]          — HTTP header grab\n" +
        "  dns [host]             — DNS lookup\n" +
        "  ping [host]            — Ping\n" +
        "  banner [host] [port]   — Banner grab\n" +
        "  ─────────────────────────────────\n" +
        "  [ Exploit ]\n" +
        "  cve [url]              — CVE + vuln scan\n" +
        "  xss [url]              — XSS payload tester\n" +
        "  sqli [url]             — SQL injection\n" +
        "  dirbr [url]            — Dir brute force\n" +
        "  exploit [url]          — Generate exploit URLs\n" +
        "  fullscan [url]         — Full scan + exploit URLs\n" +
        "  ─────────────────────────────────\n" +
        "  [ Attack ]\n" +
        "  stress [url] [t] [r]   — Stress test\n" +
        "  clear                  — Clear terminal\n" +
        "  ──────────────────────────────────────\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            setContentView(R.layout.activity_hacking);
            mainHandler = new Handler(Looper.getMainLooper());

            hackInput = findViewById(R.id.hackInput);
            btnRun    = findViewById(R.id.btnHackRun);
            btnBack   = findViewById(R.id.btnBack);
            container = findViewById(R.id.hackTerminalContainer);

            sv = new ScrollView(this);
            sv.setBackgroundColor(Color.parseColor("#0a0a0a"));
            sv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
            tv = new TextView(this);
            tv.setTextColor(Color.parseColor("#FF4444"));
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(11f);
            tv.setPadding(16, 16, 16, 16);
            tv.setTextIsSelectable(true);
            sv.addView(tv);
            container.addView(sv);

            print(SecurityTools.KALI);
            print(MENU);

            btnRun.setOnClickListener(v -> runCmd());
            hackInput.setOnEditorActionListener((v, a, e) -> { runCmd(); return true; });
            btnBack.setOnClickListener(v -> finish());

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void print(String text) {
        buf.append(text);
        if (buf.length() > 50000) buf.delete(0, buf.length() - 50000);
        tv.setText(buf.toString());
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    private SessionView wrapTerminal() {
        return new SessionView(this, 99, null) {
            @Override public void print(String t) {
                mainHandler.post(() -> HackingActivity.this.print(t));
            }
            @Override public void clear() {
                buf.setLength(0); tv.setText("");
            }
            @Override public void save() {}
        };
    }

    private void runCmd() {
        try {
            String cmd = hackInput.getText().toString().trim();
            if (cmd.isEmpty()) return;
            hackInput.setText("");
            print("\nroot@vpr:~# " + cmd + "\n");

            if (cmd.equalsIgnoreCase("clear")) {
                buf.setLength(0); tv.setText("");
                print(SecurityTools.KALI);
                print(MENU);
                return;
            }

            SessionView term = wrapTerminal();

            if (cmd.startsWith("scan ")) {
                String[] p = cmd.split("\\s+");
                if (p.length >= 4) {
                    int s = Integer.parseInt(p[2]);
                    int e = Integer.parseInt(p[3]);
                    print("[*] Scanning " + p[1] + " port " + s + "-" + e + "...\n");
                    final String h = p[1];
                    new Thread(() -> {
                        String res = SecurityTools.portScan(h, s, e);
                        mainHandler.post(() -> print(res));
                    }).start();
                } else print("Usage: scan [host] [start] [end]\n");
                return;
            }

            if (cmd.startsWith("recon ")) {
                String host = cmd.substring(6).trim();
                print("[*] Reconning " + host + "...\n");
                new Thread(() -> {
                    String res = SecurityTools.hostRecon(host);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("headers ")) {
                String url = cmd.substring(8).trim();
                print("[*] Grabbing headers...\n");
                new Thread(() -> {
                    String res = SecurityTools.headerGrab(url);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("cve ")) {
                String url = cmd.substring(4).trim();
                print("[*] Scanning vulnerabilities...\n");
                new Thread(() -> {
                    String res = SecurityTools.cveCheck(url);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("xss ")) {
                String url = cmd.substring(4).trim();
                print("[*] Testing XSS payloads...\n");
                new Thread(() -> {
                    String res = SecurityTools.xssTest(url);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("sqli ")) {
                String url = cmd.substring(5).trim();
                print("[*] Testing SQL injection...\n");
                new Thread(() -> {
                    String res = SecurityTools.sqlTest(url);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("dirbr ")) {
                String url = cmd.substring(6).trim();
                print("[*] Brute forcing directories...\n");
                new Thread(() -> SecurityTools.dirBrute(url, term, mainHandler)).start();
                return;
            }

            if (cmd.startsWith("banner ")) {
                String[] p = cmd.split("\\s+");
                if (p.length >= 3) {
                    print("[*] Grabbing banner " + p[1] + ":" + p[2] + "...\n");
                    new Thread(() -> {
                        String res = SecurityTools.bannerGrab(p[1], Integer.parseInt(p[2]));
                        mainHandler.post(() -> print(res));
                    }).start();
                } else print("Usage: banner [host] [port]\n");
                return;
            }

            if (cmd.startsWith("stress ")) {
                String[] p = cmd.split("\\s+");
                if (p.length >= 4) {
                    print("[*] Starting stress test...\n");
                    new Thread(() -> SecurityTools.stressTest(
                        p[1], Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), term, mainHandler
                    )).start();
                } else print("Usage: stress [url] [threads] [requests]\n");
                return;
            }

            if (cmd.startsWith("dns ")) {
                String host = cmd.substring(4).trim();
                new Thread(() -> {
                    String res = SecurityTools.dnsLookup(host);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("ping ")) {
                String host = cmd.substring(5).trim();
                new Thread(() -> {
                    String res = SecurityTools.ping(host);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("exploit ")) {
                String url = cmd.substring(8).trim();
                print("[*] Generating exploit URLs for " + url + "...\n");
                new Thread(() -> {
                    String res = SecurityTools.generateExploitURLs(url);
                    mainHandler.post(() -> print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("fullscan ")) {
                String url = cmd.substring(9).trim();
                print("[*] Full exploit scan on " + url + "...\n");
                new Thread(() -> SecurityTools.fullExploit(url, term, mainHandler)).start();
                return;
            }

            print("[!] Unknown. Type clear to reset.\n");

        } catch (Exception e) {
            print("[ERROR] " + e.getMessage() + "\n");
        }
    }
}

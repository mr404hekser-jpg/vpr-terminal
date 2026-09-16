package com.vpr.server;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

public class HackingActivity extends Activity {

    private EditText hackInput;
    private Button btnRun;
    private ImageButton btnBack;
    private FrameLayout container;
    private Handler mainHandler;
    private SessionView terminal;

    private static final String KALI_ASCII =
        "\n" +
        "          000011111111111111111110\n" +
        "                                 011111111111\n" +
        "                                            1111111\n" +
        "                                                   111\n" +
        "                       011111111111111111111111111101\n" +
        "          011111100                                000\n" +
        "                                       0111111111111 1\n" +
        "                                 111110               11\n" +
        "                           01110                       1111111110\n" +
        "                       111                            111111111111111100\n" +
        "                   11                               1111             1111110\n" +
        "               11                                 1111                    11111\n" +
        "            1                                    111                          11111\n" +
        "                                                 110                           11111\n" +
        "                                                011                               111\n" +
        "                                                111                                  110\n" +
        "                                                 111\n" +
        "                                                 1111\n" +
        "                                                  1111\n" +
        "                                                    11111\n" +
        "                                                      1111111111111111111\n" +
        "                                                                    0111111111111\n" +
        "                                                                           0111  11110\n" +
        "                                                                               11     110\n" +
        "                                                                                 11      11\n" +
        "                                                                                   11      10\n" +
        "                                                                                     10     11\n" +
        "                                                                                      11      1\n" +
        "                                                                                       11      0\n" +
        "                                                                                        11\n" +
        "                                                                                         1\n" +
        "                                                                                          1\n" +
        "                                                                                          10\n" +
        "                                                                                           0\n" +
        "\n" +
        "  ╔══════════════════════════════════════════╗\n" +
        "  ║      VPR  —  Hacking & Pentest Suite     ║\n" +
        "  ║   The quieter you become,                ║\n" +
        "  ║   the more you are able to hear          ║\n" +
        "  ╚══════════════════════════════════════════╝\n" +
        "\n" +
        "  [ Commands ]\n" +
        "  scan [host] [s] [e]    — Port scanner\n" +
        "  recon [host]           — Host recon + whois\n" +
        "  headers [url]          — HTTP header grab\n" +
        "  cve [url]              — CVE + vuln scan\n" +
        "  xss [url]              — XSS payload tester\n" +
        "  sqli [url]             — SQL injection test\n" +
        "  dirbr [url]            — Dir brute force\n" +
        "  banner [host] [port]   — Service banner grab\n" +
        "  stress [url] [t] [r]   — Stress / load test\n" +
        "  dns [host]             — DNS lookup\n" +
        "  ping [host]            — Ping host\n" +
        "  clear                  — Clear terminal\n" +
        "  ──────────────────────────────────────────\n\n";

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

            // Setup terminal merah
            ScrollView sv = new ScrollView(this);
            sv.setBackgroundColor(Color.parseColor("#0a0a0a"));
            sv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            TextView tv = new TextView(this);
            tv.setTextColor(Color.parseColor("#FF4444"));
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(11f);
            tv.setPadding(16, 16, 16, 16);
            tv.setTextIsSelectable(true);
            sv.addView(tv);
            container.addView(sv);

            // Buat terminal wrapper
            terminal = new SessionView(this, 99, null) {
                private StringBuilder buf = new StringBuilder();
                @Override
                public void print(String text) {
                    buf.append(text);
                    if (buf.length() > 40000)
                        buf.delete(0, buf.length() - 40000);
                    tv.setText(buf.toString());
                    sv.post(() -> sv.fullScroll(android.view.View.FOCUS_DOWN));
                }
                @Override
                public void clear() {
                    buf.setLength(0);
                    tv.setText("");
                }
                @Override
                public void save() {}
            };

            terminal.print(KALI_ASCII);

            btnRun.setOnClickListener(v -> runHackCmd());
            hackInput.setOnEditorActionListener((v, a, e) -> {
                runHackCmd();
                return true;
            });
            btnBack.setOnClickListener(v -> finish());

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runHackCmd() {
        try {
            String cmd = hackInput.getText().toString().trim();
            if (cmd.isEmpty()) return;
            hackInput.setText("");
            terminal.print("\nroot@vpr:~# " + cmd + "\n");

            if (cmd.equalsIgnoreCase("clear")) {
                terminal.clear();
                terminal.print(KALI_ASCII);
                return;
            }

            if (cmd.startsWith("scan ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 4) {
                    String h = p[1];
                    int s = Integer.parseInt(p[2]);
                    int e = Integer.parseInt(p[3]);
                    terminal.print("[*] Starting port scan on " + h + "...\n");
                    new Thread(() -> {
                        String res = SecurityTools.portScan(h, s, e);
                        mainHandler.post(() -> terminal.print(res));
                    }).start();
                } else {
                    terminal.print("Usage: scan [host] [start] [end]\n");
                }
                return;
            }

            if (cmd.startsWith("recon ")) {
                String host = cmd.substring(6).trim();
                terminal.print("[*] Reconning " + host + "...\n");
                new Thread(() -> {
                    String res = SecurityTools.hostRecon(host);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("headers ")) {
                String url = cmd.substring(8).trim();
                terminal.print("[*] Grabbing headers...\n");
                new Thread(() -> {
                    String res = SecurityTools.headerGrab(url);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("cve ")) {
                String url = cmd.substring(4).trim();
                terminal.print("[*] Scanning vulnerabilities...\n");
                new Thread(() -> {
                    String res = SecurityTools.cveCheck(url);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("xss ")) {
                String url = cmd.substring(4).trim();
                terminal.print("[*] Testing XSS payloads...\n");
                new Thread(() -> {
                    String res = SecurityTools.xssTest(url);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("sqli ")) {
                String url = cmd.substring(5).trim();
                terminal.print("[*] Testing SQL injection...\n");
                new Thread(() -> {
                    String res = SecurityTools.sqlTest(url);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("dirbr ")) {
                String url = cmd.substring(6).trim();
                terminal.print("[*] Directory brute forcing...\n");
                new Thread(() -> SecurityTools.dirBrute(url, terminal, mainHandler)).start();
                return;
            }

            if (cmd.startsWith("banner ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 3) {
                    terminal.print("[*] Grabbing banner...\n");
                    new Thread(() -> {
                        String res = SecurityTools.bannerGrab(p[1], Integer.parseInt(p[2]));
                        mainHandler.post(() -> terminal.print(res));
                    }).start();
                } else {
                    terminal.print("Usage: banner [host] [port]\n");
                }
                return;
            }

            if (cmd.startsWith("stress ")) {
                String[] p = cmd.split(" ");
                if (p.length >= 4) {
                    terminal.print("[*] Starting stress test...\n");
                    new Thread(() -> SecurityTools.stressTest(
                        p[1], Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), terminal, mainHandler
                    )).start();
                } else {
                    terminal.print("Usage: stress [url] [threads] [requests]\n");
                }
                return;
            }

            if (cmd.startsWith("dns ")) {
                String host = cmd.substring(4).trim();
                new Thread(() -> {
                    String res = SecurityTools.dnsLookup(host);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            if (cmd.startsWith("ping ")) {
                String host = cmd.substring(5).trim();
                new Thread(() -> {
                    String res = SecurityTools.ping(host);
                    mainHandler.post(() -> terminal.print(res));
                }).start();
                return;
            }

            terminal.print("[!] Unknown command. Type 'clear' to reset.\n");

        } catch (Exception e) {
            terminal.print("[ERROR] " + e.getMessage() + "\n");
        }
    }
}

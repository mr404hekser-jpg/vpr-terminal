package com.vpr.server;

import android.content.Context;
import android.util.Log;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Scriptable;
import org.python.util.PythonInterpreter;
import org.python.core.PyException;
import java.io.*;
import java.util.*;

public class VPRScriptEngine {

    private static final String TAG = "VPR-Engine";
    private final Context ctx;

    public interface OutputListener {
        void onOutput(String text);
        void onError(String text);
        void onDone(int exitCode);
    }

    public VPRScriptEngine(Context ctx) {
        this.ctx = ctx;
    }

    // ─── Shell Command ───
    public void runShell(String command, File workDir, OutputListener cb) {
        new Thread(() -> {
            try {
                String shell = findShell();
                ProcessBuilder pb = new ProcessBuilder(shell, "-c", command);
                pb.redirectErrorStream(true);
                if (workDir != null && workDir.exists()) pb.directory(workDir);

                Map<String, String> env = pb.environment();
                env.put("HOME", ctx.getFilesDir().getAbsolutePath());
                env.put("TMPDIR", ctx.getCacheDir().getAbsolutePath());
                env.put("PATH", "/system/bin:/system/xbin:" +
                    "/data/data/com.termux/files/usr/bin:" +
                    env.getOrDefault("PATH", ""));

                Process p = pb.start();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                char[] buf = new char[512];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    cb.onOutput(out);
                }
                int exit = p.waitFor();
                cb.onDone(exit);
            } catch (Exception e) {
                cb.onError("[ERROR] " + e.getMessage() + "\n");
                cb.onDone(1);
            }
        }).start();
    }

    // ─── Shell File ───
    public void runShellFile(File file, OutputListener cb) {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(findShell(), file.getAbsolutePath());
                pb.redirectErrorStream(true);
                pb.directory(file.getParentFile());
                Map<String, String> env = pb.environment();
                env.put("HOME", ctx.getFilesDir().getAbsolutePath());
                env.put("TMPDIR", ctx.getCacheDir().getAbsolutePath());
                Process p = pb.start();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                char[] buf = new char[512];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    cb.onOutput(out);
                }
                cb.onDone(p.waitFor());
            } catch (Exception e) {
                cb.onError("[ERROR] " + e.getMessage() + "\n");
                cb.onDone(1);
            }
        }).start();
    }

    // ─── JavaScript (Rhino) ───
    public void runJavaScript(String code, String fileName, OutputListener cb) {
        new Thread(() -> {
            org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
            try {
                cx.setOptimizationLevel(-1);
                Scriptable scope = cx.initStandardObjects();

                // console.log
                ScriptableObject.putProperty(scope, "print",
                    new BaseFunction() {
                        @Override
                        public Object call(org.mozilla.javascript.Context cx,
                                           Scriptable scope, Scriptable thisObj,
                                           Object[] args) {
                            StringBuilder sb = new StringBuilder();
                            for (Object a : args) {
                                if (sb.length() > 0) sb.append(" ");
                                sb.append(org.mozilla.javascript.Context.toString(a));
                            }
                            cb.onOutput(sb.append("\n").toString());
                            return org.mozilla.javascript.Context.getUndefinedValue();
                        }
                    });

                cx.evaluateString(scope,
                    "var console = {" +
                    "  log: function(){var s=\'\';for(var i=0;i<arguments.length;i++){if(i>0)s+=\' \';s+=String(arguments[i]);}print(s);}," +
                    "  error: function(){var s=\'[ERR] \';for(var i=0;i<arguments.length;i++){if(i>0)s+=\' \';s+=String(arguments[i]);}print(s);}," +
                    "  warn: function(){var s=\'[WARN] \';for(var i=0;i<arguments.length;i++){if(i>0)s+=\' \';s+=String(arguments[i]);}print(s);}" +
                    "};",
                    "console_setup", 1, null);

                cx.evaluateString(scope,
                    "var process = {argv:[\'node\',\'" + fileName + "\'],env:{},exit:function(c){},stdout:{write:function(s){print(s);}},stderr:{write:function(s){print(\'[ERR] \'+s);}}};",
                    "process_setup", 1, null);

                ScriptableObject.putProperty(scope, "require",
                    new BaseFunction() {
                        @Override
                        public Object call(org.mozilla.javascript.Context cx,
                                           Scriptable scope, Scriptable thisObj,
                                           Object[] args) {
                            cb.onOutput("[VPR] require('" +
                                org.mozilla.javascript.Context.toString(args[0]) +
                                "') - not supported in built-in engine\n");
                            return cx.newObject(scope);
                        }
                    });

                cx.evaluateString(scope, code, fileName, 1, null);
                cb.onDone(0);

            } catch (org.mozilla.javascript.RhinoException e) {
                cb.onError("[JS ERROR] " + e.getMessage() +
                    " line:" + e.lineNumber() + "\n");
                cb.onDone(1);
            } catch (Exception e) {
                cb.onError("[ERROR] " + e.getMessage() + "\n");
                cb.onDone(1);
            } finally {
                org.mozilla.javascript.Context.exit();
            }
        }).start();
    }

    // ─── JavaScript File ───
    public void runJavaScriptFile(File file, OutputListener cb) {
        try {
            StringBuilder code = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) code.append(line).append("\n");
            br.close();
            runJavaScript(code.toString(), file.getName(), cb);
        } catch (Exception e) {
            cb.onError("[ERROR] Cannot read: " + e.getMessage() + "\n");
            cb.onDone(1);
        }
    }

    // ─── Python (Jython) via StringWriter ───
    public void runPython(String code, String fileName, OutputListener cb) {
        new Thread(() -> {
            try {
                // Setup Jython properties
                Properties props = new Properties();
                String cacheDir = ctx.getCacheDir().getAbsolutePath() + "/jython";
                new File(cacheDir).mkdirs();
                props.setProperty("python.home", cacheDir);
                props.setProperty("python.cachedir", cacheDir);
                props.setProperty("python.cachedir.skip", "false");
                props.setProperty("python.verbose", "error");
                props.setProperty("python.security.respectJavaAccessibility", "false");

                PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});

                PythonInterpreter interp = new PythonInterpreter();

                // Capture output via StringWriter polling
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                interp.setOut(pw);
                interp.setErr(pw);

                // Run dengan polling output
                interp.exec(code);
                pw.flush();

                String output = sw.toString();
                if (!output.isEmpty()) cb.onOutput(output);
                cb.onDone(0);
                interp.close();

            } catch (PyException e) {
                cb.onError("[Python ERROR] " + e.getMessage() + "\n");
                cb.onDone(1);
            } catch (Exception e) {
                cb.onError("[ERROR] " + e.getMessage() + "\n");
                cb.onDone(1);
            }
        }).start();
    }

    // ─── Python File ───
    public void runPythonFile(File file, OutputListener cb) {
        try {
            StringBuilder code = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) code.append(line).append("\n");
            br.close();
            runPython(code.toString(), file.getName(), cb);
        } catch (Exception e) {
            cb.onError("[ERROR] Cannot read: " + e.getMessage() + "\n");
            cb.onDone(1);
        }
    }

    // ─── Auto detect & run file ───
    public void runFile(File file, OutputListener cb) {
        String name = file.getName();
        if (name.endsWith(".sh")) {
            runShellFile(file, cb);
        } else if (name.endsWith(".js")) {
            runJavaScriptFile(file, cb);
        } else if (name.endsWith(".py")) {
            String extPy = DaemonManager.getPython();
            if (extPy != null) {
                runShell(extPy + " " + file.getAbsolutePath(),
                    file.getParentFile(), cb);
            } else {
                cb.onOutput("[VPR] Using built-in Jython Python 2.7\n");
                runPythonFile(file, cb);
            }
        } else {
            runShell(file.getAbsolutePath(), file.getParentFile(), cb);
        }
    }

    private String findShell() {
        String found = DaemonManager.findBinary("bash","sh","ash","dash");
        return found != null ? found : "/system/bin/sh";
    }
}

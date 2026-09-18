package com.vpr.server;

import android.content.Context;
import android.util.Log;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.python.util.PythonInterpreter;
import java.io.*;
import java.util.*;

public class VPRScriptEngine {

    private static final String TAG = "VPR-Engine";
    private final android.content.Context appCtx;

    public interface OutputListener {
        void onOutput(String text);
        void onError(String text);
        void onDone(int exitCode);
    }

    public VPRScriptEngine(android.content.Context ctx) {
        this.appCtx = ctx;
    }

    // ─── Run Shell Command ───
    public void runShell(String command, File workDir, OutputListener listener) {
        new Thread(() -> {
            try {
                String shell = findShell();
                ProcessBuilder pb = new ProcessBuilder(shell, "-c", command);
                pb.redirectErrorStream(true);
                if (workDir != null && workDir.exists()) pb.directory(workDir);

                // Set environment
                Map<String, String> env = pb.environment();
                env.put("HOME", appCtx.getFilesDir().getAbsolutePath());
                env.put("TMPDIR", appCtx.getCacheDir().getAbsolutePath());
                env.put("PATH", "/system/bin:/system/xbin:" +
                    "/data/data/com.termux/files/usr/bin:" + env.get("PATH"));

                Process p = pb.start();

                // Real-time output
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                char[] buf = new char[256];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    listener.onOutput(out);
                }

                int exit = p.waitFor();
                listener.onDone(exit);

            } catch (Exception e) {
                listener.onError("[ERROR] " + e.getMessage() + "\n");
                listener.onDone(1);
            }
        }).start();
    }

    // ─── Run Shell File (.sh) ───
    public void runShellFile(File file, OutputListener listener) {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(findShell(), file.getAbsolutePath());
                pb.redirectErrorStream(true);
                pb.directory(file.getParentFile());

                Map<String, String> env = pb.environment();
                env.put("HOME", appCtx.getFilesDir().getAbsolutePath());
                env.put("TMPDIR", appCtx.getCacheDir().getAbsolutePath());

                Process p = pb.start();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                char[] buf = new char[256];
                int len;
                while ((len = br.read(buf, 0, buf.length)) != -1) {
                    final String out = new String(buf, 0, len);
                    listener.onOutput(out);
                }
                int exit = p.waitFor();
                listener.onDone(exit);
            } catch (Exception e) {
                listener.onError("[ERROR] " + e.getMessage() + "\n");
                listener.onDone(1);
            }
        }).start();
    }

    // ─── Run JavaScript (Rhino Engine) ───
    public void runJavaScript(String code, String fileName, OutputListener listener) {
        new Thread(() -> {
            Context cx = Context.enter();
            try {
                cx.setOptimizationLevel(-1); // Interpreted mode for Android

                // Custom output capture
                StringBuilder output = new StringBuilder();
                Scriptable scope = cx.initStandardObjects();

                // Inject print functions
                org.mozilla.javascript.ScriptableObject.putProperty(scope, "print",
                    new org.mozilla.javascript.BaseFunction() {
                        @Override
                        public Object call(Context cx, Scriptable scope,
                                           Scriptable thisObj, Object[] args) {
                            StringBuilder sb = new StringBuilder();
                            for (Object arg : args) sb.append(Context.toString(arg));
                            sb.append("\n");
                            listener.onOutput(sb.toString());
                            return Context.getUndefinedValue();
                        }
                    });

                org.mozilla.javascript.ScriptableObject.putProperty(scope, "console",
                    cx.evaluateString(scope,
                        "({ log: function() { var s=''; for(var i=0;i<arguments.length;i++)" +
                        "{ if(i>0)s+=' '; s+=String(arguments[i]); } print(s); }," +
                        "error: function() { var s='[ERR] '; for(var i=0;i<arguments.length;i++)" +
                        "{ if(i>0)s+=' '; s+=String(arguments[i]); } print(s); } })",
                        "console", 1, null));

                // Inject require stub
                org.mozilla.javascript.ScriptableObject.putProperty(scope, "require",
                    new org.mozilla.javascript.BaseFunction() {
                        @Override
                        public Object call(Context cx, Scriptable scope,
                                           Scriptable thisObj, Object[] args) {
                            listener.onOutput("[VPR] require('" + args[0] + "') - limited support\n");
                            return cx.newObject(scope);
                        }
                    });

                // Process
                org.mozilla.javascript.ScriptableObject.putProperty(scope, "process",
                    cx.evaluateString(scope,
                        "({ argv: ['" + fileName + "'], env: {}, exit: function(c){}, " +
                        "stdout: { write: function(s){ print(s); } }, " +
                        "stderr: { write: function(s){ print('[ERR] '+s); } } })",
                        "process", 1, null));

                cx.evaluateString(scope, code, fileName, 1, null);
                listener.onDone(0);

            } catch (org.mozilla.javascript.RhinoException e) {
                listener.onError("[JS ERROR] " + e.getMessage() +
                    " (line " + e.lineNumber() + ")\n");
                listener.onDone(1);
            } catch (Exception e) {
                listener.onError("[ERROR] " + e.getMessage() + "\n");
                listener.onDone(1);
            } finally {
                Context.exit();
            }
        }).start();
    }

    // ─── Run JavaScript File ───
    public void runJavaScriptFile(File file, OutputListener listener) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder code = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) code.append(line).append("\n");
            br.close();
            runJavaScript(code.toString(), file.getName(), listener);
        } catch (Exception e) {
            listener.onError("[ERROR] Cannot read file: " + e.getMessage() + "\n");
            listener.onDone(1);
        }
    }

    // ─── Run Python (Jython Engine) ───
    public void runPython(String code, String fileName, OutputListener listener) {
        new Thread(() -> {
            PythonInterpreter interp = null;
            try {
                Properties props = new Properties();
                props.put("python.home", appCtx.getCacheDir().getAbsolutePath());
                props.put("python.cachedir", appCtx.getCacheDir().getAbsolutePath() + "/py");
                props.put("python.cachedir.skip", "false");
                props.put("python.verbose", "error");

                PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});

                interp = new PythonInterpreter();

                // Capture output
                PipedOutputStream pout = new PipedOutputStream();
                PipedInputStream pin = new PipedInputStream(pout);
                interp.setOut(pout);
                interp.setErr(pout);

                final PipedInputStream finalPin = pin;
                Thread reader = new Thread(() -> {
                    try {
                        BufferedReader br = new BufferedReader(
                            new InputStreamReader(finalPin));
                        char[] buf = new char[256];
                        int len;
                        while ((len = br.read(buf, 0, buf.length)) != -1) {
                            final String out = new String(buf, 0, len);
                            listener.onOutput(out);
                        }
                    } catch (Exception ignored) {}
                });
                reader.start();

                interp.exec(code);
                pout.flush();
                pout.close();
                reader.join(3000);
                listener.onDone(0);

            } catch (org.python.core.PyException e) {
                listener.onError("[Python ERROR] " + e.getMessage() + "\n");
                listener.onDone(1);
            } catch (Exception e) {
                listener.onError("[ERROR] " + e.getMessage() + "\n");
                listener.onDone(1);
            } finally {
                try { if (interp != null) interp.close(); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ─── Run Python File ───
    public void runPythonFile(File file, OutputListener listener) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder code = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) code.append(line).append("\n");
            br.close();
            runPython(code.toString(), file.getName(), listener);
        } catch (Exception e) {
            listener.onError("[ERROR] Cannot read file: " + e.getMessage() + "\n");
            listener.onDone(1);
        }
    }

    // ─── Run any file by extension ───
    public void runFile(File file, OutputListener listener) {
        String name = file.getName();
        if (name.endsWith(".sh")) {
            runShellFile(file, listener);
        } else if (name.endsWith(".js")) {
            runJavaScriptFile(file, listener);
        } else if (name.endsWith(".py")) {
            // Try external Python first, fallback to Jython
            String extPython = DaemonManager.getPython();
            if (extPython != null) {
                runShell(extPython + " " + file.getAbsolutePath(), file.getParentFile(), listener);
            } else {
                listener.onOutput("[VPR] Using built-in Python (Jython)\n");
                runPythonFile(file, listener);
            }
        } else {
            runShell(file.getAbsolutePath(), file.getParentFile(), listener);
        }
    }

    // ─── Find shell ───
    private String findShell() {
        String found = DaemonManager.findBinary("bash", "sh", "ash", "dash");
        return found != null ? found : "/system/bin/sh";
    }
}

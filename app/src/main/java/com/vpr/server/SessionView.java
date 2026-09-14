package com.vpr.server;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class SessionView {
    private ScrollView scrollView;
    private TextView textView;
    private StringBuilder buffer = new StringBuilder();
    private int sessionId;
    private SharedPreferences prefs;
    private static final int MAX_BUFFER = 50000; // max char di buffer

    public SessionView(Context ctx, int id, SharedPreferences prefs) {
        this.sessionId = id;
        this.prefs = prefs;

        scrollView = new ScrollView(ctx);
        scrollView.setBackgroundColor(Color.parseColor("#0a0a0a"));
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.MATCH_PARENT
        ));

        textView = new TextView(ctx);
        textView.setTextColor(Color.parseColor("#00FF88"));
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(12f);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextIsSelectable(true);
        scrollView.addView(textView);

        // Restore saved content
        String saved = prefs.getString("session_content_" + id, null);
        if (saved != null) {
            buffer.append(saved);
            textView.setText(buffer.toString());
        }
    }

    public void print(String text) {
        buffer.append(text);
        // Trim buffer kalau terlalu panjang
        if (buffer.length() > MAX_BUFFER) {
            buffer.delete(0, buffer.length() - MAX_BUFFER);
        }
        textView.setText(buffer.toString());
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        // Auto-save tiap update
        save();
    }

    public void save() {
        String content = buffer.toString();
        // Simpan max 10000 char terakhir
        if (content.length() > 10000) {
            content = content.substring(content.length() - 10000);
        }
        prefs.edit().putString("session_content_" + sessionId, content).apply();
    }

    public void clear() {
        buffer.setLength(0);
        textView.setText("");
        prefs.edit().remove("session_content_" + sessionId).apply();
    }

    public View getView() { return scrollView; }
    public int getSessionId() { return sessionId; }
}

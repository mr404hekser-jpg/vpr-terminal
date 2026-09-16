package com.vpr.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class SessionView {

    private final ScrollView scrollView;
    private final TextView textView;
    private final StringBuilder buffer = new StringBuilder();
    private final int sessionId;
    private final SharedPreferences prefs;
    private static final int MAX_CHARS = 30000;

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
        textView.setTextColor(Color.parseColor("#2196F3"));
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(12f);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextIsSelectable(true);
        scrollView.addView(textView);

        // Restore konten tersimpan
        String saved = prefs != null ? prefs.getString("sess_" + id, null) : null;
        if (saved != null) {
            buffer.append(saved);
            textView.setText(buffer.toString());
        }
    }

    public void print(String text) {
        buffer.append(text);
        if (buffer.length() > MAX_CHARS) {
            buffer.delete(0, buffer.length() - MAX_CHARS);
        }
        textView.setText(buffer.toString());
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        save();
    }

    public void save() {
        try {
            if (prefs == null) return;
            String content = buffer.toString();
            if (content.length() > 8000) {
                content = content.substring(content.length() - 8000);
            }
            prefs.edit().putString("sess_" + sessionId, content).apply();
        } catch (Exception ignored) {}
    }

    public void clear() {
        buffer.setLength(0);
        textView.setText("");
        prefs.edit().remove("sess_" + sessionId).apply();
    }

    public View getView() { return scrollView; }
}

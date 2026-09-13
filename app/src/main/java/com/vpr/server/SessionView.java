package com.vpr.server;
import android.content.Context;
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
    public SessionView(Context ctx, int id) {
        this.sessionId = id;
        scrollView = new ScrollView(ctx);
        scrollView.setBackgroundColor(Color.parseColor("#0a0a0a"));
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.MATCH_PARENT));
        textView = new TextView(ctx);
        textView.setTextColor(Color.parseColor("#00FF88"));
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(12f);
        textView.setPadding(16,16,16,16);
        scrollView.addView(textView);
    }
    public void print(String text) {
        buffer.append(text);
        textView.setText(buffer.toString());
        scrollView.post(()->scrollView.fullScroll(View.FOCUS_DOWN));
    }
    public void clear() { buffer.setLength(0); textView.setText(""); }
    public View getView() { return scrollView; }
    public int getSessionId() { return sessionId; }
}

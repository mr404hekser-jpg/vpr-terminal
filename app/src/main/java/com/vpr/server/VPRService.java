package com.vpr.server;

import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.core.app.NotificationCompat;

public class VPRService extends Service {

    private static final String CH = "VPR_CH";
    private static final int NOTIF_ID = 1;
    private DaemonManager daemon;
    private Handler restartHandler;
    private boolean intentionallyStopped = false;

    @Override
    public void onCreate() {
        super.onCreate();
        restartHandler = new Handler(Looper.getMainLooper());
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intentionallyStopped = false;
        try {
            startForeground(NOTIF_ID, buildNotif());
            if (daemon == null) {
                daemon = new DaemonManager(getApplicationContext());
                daemon.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { if (daemon != null) daemon.stop(); } catch (Exception ignored) {}
        if (!intentionallyStopped) {
            restartHandler.postDelayed(() -> {
                try {
                    Intent i = new Intent(getApplicationContext(), VPRService.class);
                    if (Build.VERSION.SDK_INT >= 26)
                        startForegroundService(i);
                    else startService(i);
                } catch (Exception ignored) {}
            }, 1000);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent i = new Intent(getApplicationContext(), VPRService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private Notification buildNotif() {
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle("VPR Server")
            .setContentText("Virtual Persistent Runtime — aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CH, "VPR Server", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("VPR background service");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}

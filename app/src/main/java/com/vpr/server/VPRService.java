package com.vpr.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class VPRService extends Service {

    private static final String CHANNEL_ID = "VPR_CHANNEL";
    private DaemonManager daemon;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "VPR Server", NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VPR Server")
                .setContentText("Virtual Persistent Runtime aktif")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            startForeground(1, notif);

            if (daemon == null) {
                daemon = new DaemonManager(getApplicationContext());
                daemon.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (daemon != null) daemon.stop();
        } catch (Exception ignored) {}
        try {
            startService(new Intent(getApplicationContext(), VPRService.class));
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

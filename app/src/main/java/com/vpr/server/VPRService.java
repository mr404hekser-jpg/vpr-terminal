package com.vpr.server;
import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.core.app.NotificationCompat;
public class VPRService extends Service {
    private static final String CH="VPR_CH";
    private DaemonManager daemon;
    @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){NotificationChannel c=new NotificationChannel(CH,"VPR",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    @Override public int onStartCommand(Intent i,int f,int s){startForeground(1,new NotificationCompat.Builder(this,CH).setContentTitle("VPR Server").setContentText("Running").setSmallIcon(android.R.drawable.ic_dialog_info).build());daemon=new DaemonManager(getApplicationContext());daemon.start();return START_STICKY;}
    @Override public void onDestroy(){super.onDestroy();if(daemon!=null)daemon.stop();startService(new Intent(getApplicationContext(),VPRService.class));}
    @Override public IBinder onBind(Intent i){return null;}
}

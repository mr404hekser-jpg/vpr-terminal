package com.vpr.server;
import android.content.*;
public class BootReceiver extends BroadcastReceiver{@Override public void onReceive(Context ctx,Intent intent){if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){Intent s=new Intent(ctx,VPRService.class);if(android.os.Build.VERSION.SDK_INT>=26)ctx.startForegroundService(s);else ctx.startService(s);}}}

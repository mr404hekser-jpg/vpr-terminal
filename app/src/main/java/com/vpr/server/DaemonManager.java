package com.vpr.server;
import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;
public class DaemonManager {
    private static final String TAG="VPR";
    private boolean running=false;
    private Thread thread;
    private List<Process> procs=new ArrayList<>();
    private String BOT,SCRIPT,APP,LOG;
    public DaemonManager(Context ctx){String r="/sdcard/vpr";BOT=r+"/bots";SCRIPT=r+"/scripts";APP=r+"/apps";LOG=r+"/logs";new File(BOT).mkdirs();new File(SCRIPT).mkdirs();new File(APP).mkdirs();new File(LOG).mkdirs();}
    public void start(){running=true;thread=new Thread(()->{while(running){try{scan(BOT);scan(SCRIPT);scan(APP);Thread.sleep(30000);}catch(InterruptedException e){break;}}});thread.start();}
    private void scan(String dir){File f=new File(dir);if(!f.exists())return;File[]files=f.listFiles();if(files==null)return;for(File x:files){String n=x.getName();try{Process p=null;if(n.endsWith(".sh"))p=Runtime.getRuntime().exec(new String[]{"sh",x.getAbsolutePath()});else if(n.endsWith(".py"))p=Runtime.getRuntime().exec(new String[]{"/data/data/com.termux/files/usr/bin/python3",x.getAbsolutePath()});else if(n.endsWith(".js"))p=Runtime.getRuntime().exec(new String[]{"/data/data/com.termux/files/usr/bin/node",x.getAbsolutePath()});if(p!=null){procs.add(p);Log.d(TAG,"Started:"+n);}}catch(IOException e){Log.e(TAG,"Err:"+n);}}}
    public void stop(){running=false;for(Process p:procs)p.destroy();procs.clear();if(thread!=null)thread.interrupt();}
}

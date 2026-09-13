package com.vpr.server;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
public class MainActivity extends Activity {
    private LinearLayout sessionTabBar;
    private FrameLayout terminalContainer;
    private EditText cmdInput;
    private Button btnSend, btnUpload, btnNewSession;
    private List<SessionView> sessions = new ArrayList<>();
    private int activeSession = 0;
    private int sessionCount = 0;
    private static final int FILE_PICK = 100;
    private static final String ASCII =
        "  ██╗   ██╗██████╗ ██████╗ \n"+
        "  ██║   ██║██╔══██╗██╔══██╗\n"+
        "  ██║   ██║██████╔╝██████╔╝\n"+
        "  ╚██╗ ██╔╝██╔═══╝ ██╔══██╗\n"+
        "   ╚████╔╝ ██║     ██║  ██║\n"+
        "    ╚═══╝  ╚═╝     ╚═╝  ╚═╝\n"+
        "        S E R V E R\n\n"+
        "  Welcome to VPR Server\n"+
        "  ─────────────────────────\n\n";
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        sessionTabBar=findViewById(R.id.sessionTabBar);
        terminalContainer=findViewById(R.id.terminalContainer);
        cmdInput=findViewById(R.id.cmdInput);
        btnSend=findViewById(R.id.btnSend);
        btnUpload=findViewById(R.id.btnUpload);
        btnNewSession=findViewById(R.id.btnNewSession);
        startService(new Intent(this,VPRService.class));
        createSession();
        btnSend.setOnClickListener(v->{String c=cmdInput.getText().toString().trim();if(!c.isEmpty()){exec(c);cmdInput.setText("");}});
        btnUpload.setOnClickListener(v->pickFile());
        btnNewSession.setOnClickListener(v->createSession());
        cmdInput.setOnEditorActionListener((v,a,e)->{String c=cmdInput.getText().toString().trim();if(!c.isEmpty()){exec(c);cmdInput.setText("");}return true;});
    }
    private void createSession() {
        sessionCount++;
        SessionView sv=new SessionView(this,sessionCount);
        sessions.add(sv);
        Button tab=new Button(this);
        tab.setText("S"+sessionCount);
        tab.setTextColor(0xFF00FF88);
        tab.setBackgroundColor(0xFF1A1A1A);
        tab.setPadding(20,8,20,8);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(4,0,4,0);
        tab.setLayoutParams(lp);
        int idx=sessions.size()-1;
        tab.setOnClickListener(v->switchSession(idx));
        sessionTabBar.addView(tab);
        switchSession(idx);
        sv.print(ASCII);
        sv.print("  Session "+sessionCount+" siap.\n\n");
    }
    private void switchSession(int idx) {
        activeSession=idx;
        terminalContainer.removeAllViews();
        terminalContainer.addView(sessions.get(idx).getView());
        for(int i=0;i<sessionTabBar.getChildCount();i++){View v=sessionTabBar.getChildAt(i);if(v instanceof Button)((Button)v).setBackgroundColor(i==idx?0xFF003322:0xFF1A1A1A);}
    }
    private void exec(String cmd) {
        SessionView sv=sessions.get(activeSession);
        sv.print("\n$ "+cmd+"\n");
        new Thread(()->{
            try {
                String[] fc;
                if(cmd.endsWith(".py")) fc=new String[]{"/data/data/com.termux/files/usr/bin/python3","/sdcard/vpr/bots/"+cmd};
                else if(cmd.endsWith(".js")) fc=new String[]{"/data/data/com.termux/files/usr/bin/node","/sdcard/vpr/bots/"+cmd};
                else if(cmd.endsWith(".sh")) fc=new String[]{"sh","/sdcard/vpr/bots/"+cmd};
                else fc=new String[]{"sh","-c",cmd};
                Process p=Runtime.getRuntime().exec(fc);
                BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while((line=br.readLine())!=null){final String o=line+"\n";runOnUiThread(()->sv.print(o));}
                BufferedReader er=new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while((line=er.readLine())!=null){final String o="[ERR] "+line+"\n";runOnUiThread(()->sv.print(o));}
                int ex=p.waitFor();
                runOnUiThread(()->sv.print("\n[exit "+ex+"]\n"));
            } catch(Exception e){runOnUiThread(()->sv.print("[ERROR] "+e.getMessage()+"\n"));}
        }).start();
    }
    private void pickFile() {
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i,"Pilih File"),FILE_PICK);
    }
    @Override
    protected void onActivityResult(int req,int res,Intent data) {
        if(req==FILE_PICK&&res==RESULT_OK&&data!=null){Uri uri=data.getData();copyFile(uri,getName(uri));}
    }
    private String getName(Uri uri) {
        String r=null;
        if("content".equals(uri.getScheme())){Cursor c=getContentResolver().query(uri,null,null,null,null);try{if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)r=c.getString(i);}}finally{if(c!=null)c.close();}}
        return r!=null?r:uri.getLastPathSegment();
    }
    private void copyFile(Uri uri,String name) {
        SessionView sv=sessions.get(activeSession);
        new Thread(()->{
            try {
                File d=new File("/sdcard/vpr/bots/");d.mkdirs();
                File dest=new File(d,name);
                InputStream in=getContentResolver().openInputStream(uri);
                FileOutputStream out=new FileOutputStream(dest);
                byte[] buf=new byte[4096];int len;
                while((len=in.read(buf))>0)out.write(buf,0,len);
                in.close();out.close();
                runOnUiThread(()->{
                    sv.print("\n[VPR] Upload: "+name+"\n");
                    sv.print("[VPR] Path: /sdcard/vpr/bots/"+name+"\n");
                    if(name.endsWith(".py")||name.endsWith(".js")||name.endsWith(".sh")){
                        new AlertDialog.Builder(this).setTitle("Jalankan?").setMessage("Run "+name+" sekarang?")
                        .setPositiveButton("Ya",(dd,w)->exec(name)).setNegativeButton("Nanti",null).show();
                    }
                });
            } catch(Exception e){runOnUiThread(()->sv.print("[ERROR] "+e.getMessage()+"\n"));}
        }).start();
    }
}

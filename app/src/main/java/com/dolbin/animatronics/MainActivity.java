package com.dolbin.animatronics;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newCachedThreadPool();
    private SkullView skull;private TextView status;private EditText host,script;private Spinner role,character;
    private TextToSpeech tts;private ServerSocket server;private Socket client;private PrintWriter out;
    private final List<PrintWriter> peers=new CopyOnWriteArrayList<>();
    private volatile boolean running=false;private final int port=48271;
    private final String[] names={"Skeleton","Spirit Ball","Boris 1","Boris 2","Audience"};
    private String sample="Skeleton|Welcome to our haunted house!\nSpirit Ball|You should have seen it last year!\nBoris 1|The neighbors still talk about it!\nBoris 2|Well, most of them.";
    @Override public void onCreate(Bundle b){super.onCreate(b);tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(Locale.US);});build();}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,16,16,16);root.setBackgroundColor(Color.rgb(15,17,28));
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        TextView title=new TextView(this);title.setText("ANIMATRONIC DIALOGUE STUDIO  •  v0.1");title.setTextSize(19);title.setTextColor(Color.WHITE);root.addView(title);
        role=new Spinner(this);role.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Director","Performer","Audience"}));root.addView(role);
        character=new Spinner(this);character.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));root.addView(character);
        host=new EditText(this);host.setSingleLine(true);host.setText("192.168.43.1");host.setHint("Director hotspot IP address");host.setTextColor(Color.WHITE);root.addView(host);
        Button connect=new Button(this);connect.setText("Start Director / Join Director");root.addView(connect);connect.setOnClickListener(v->{if(role.getSelectedItemPosition()==0)startServer();else join(host.getText().toString().trim());});
        status=new TextView(this);status.setText("Not connected • For prototype testing, keep app open");status.setTextColor(Color.LTGRAY);root.addView(status);
        skull=new SkullView(this);root.addView(skull,new LinearLayout.LayoutParams(-1,310));
        script=new EditText(this);script.setText(sample);script.setMinLines(5);script.setTextColor(Color.WHITE);script.setHint("Character|dialogue, one per line");root.addView(script);
        Button play=new Button(this);play.setText("Play Script (Director)");root.addView(play);play.setOnClickListener(v->playScript());
        Button test=new Button(this);test.setText("Test My Character");root.addView(test);test.setOnClickListener(v->speak("Testing one two three. Welcome to the haunted house!"));
        Button stop=new Button(this);stop.setText("Stop");root.addView(stop);stop.setOnClickListener(v->{running=false;tts.stop();skull.setSpeaking(false);broadcast("STOP");});
    }
    private void show(String s){ui.post(()->status.setText(s));}
    private void startServer(){io.execute(()->{try{server=new ServerSocket(port);show("Director listening on port "+port+"; connect other phones to hotspot");while(!server.isClosed()){Socket s=server.accept();PrintWriter pw=new PrintWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"),true);peers.add(pw);show("Performer connected • "+peers.size()+" peer(s)");io.execute(()->read(s,pw));}}catch(Exception e){show("Server: "+e.getMessage());}});}
    private void join(String address){io.execute(()->{try{client=new Socket();client.connect(new InetSocketAddress(address,port),4000);out=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"),true);show("Connected to Director at "+address);read(client,null);}catch(Exception e){show("Connection failed: "+e.getMessage());}});}
    private void read(Socket s,PrintWriter peer){try(BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"))){String line;while((line=r.readLine())!=null){if(line.equals("STOP")){running=false;ui.post(()->{tts.stop();skull.setSpeaking(false);});}else if(line.startsWith("LINE|")){String[] parts=line.split("\\|",3);if(parts.length==3)dispatch(parts[1],parts[2]);}}}catch(Exception ignored){}finally{if(peer!=null)peers.remove(peer);show("Connection ended");}}
    private void broadcast(String s){for(PrintWriter p:peers)p.println(s);}
    private void dispatch(String who,String words){ui.post(()->{int mode=role.getSelectedItemPosition();String assigned=character.getSelectedItem().toString();if(mode==2||assigned.equals(who))speak(words);});}
    private void speak(String words){tts.stop();skull.setSpeaking(true);tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,"line");long duration=Math.max(1000,Math.min(10000,words.length()*75L));ui.postDelayed(()->skull.setSpeaking(false),duration);}
    private void playScript(){if(role.getSelectedItemPosition()!=0){show("Start playback from Director");return;}String text=script.getText().toString();running=true;io.execute(()->{for(String line:text.split("\\n")){if(!running)break;String[] a=line.split("\\|",2);if(a.length!=2)continue;broadcast("LINE|"+a[0]+"|"+a[1]);dispatch(a[0],a[1]);try{Thread.sleep(Math.max(1800,Math.min(10000,a[1].length()*85L)));}catch(InterruptedException ignored){break;}}show("Script finished");});}
    @Override protected void onDestroy(){running=false;try{if(server!=null)server.close();if(client!=null)client.close();}catch(Exception ignored){}if(tts!=null){tts.stop();tts.shutdown();}io.shutdownNow();super.onDestroy();}
}

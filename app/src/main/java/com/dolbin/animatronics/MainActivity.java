package com.dolbin.animatronics;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int SHOW_PORT = 48271;
    private static final int DISCOVERY_PORT = 48272;
    private static final String DISCOVER = "ADS_DISCOVER_V02";
    private static final String HERE = "ADS_DIRECTOR_V02";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final List<PrintWriter> peers = new CopyOnWriteArrayList<>();
    private final String[] names = {"Skeleton", "Spirit Ball", "Boris 1", "Boris 2", "Audience"};

    private SkullView skull;
    private TextView status, networkHelp;
    private EditText host, script;
    private Spinner role, character;
    private TextToSpeech tts;
    private volatile boolean running = false;
    private volatile boolean directorRunning = false;
    private volatile boolean joining = false;
    private ServerSocket server;
    private DatagramSocket discoverySocket;
    private Socket client;
    private PrintWriter out;
    private final String sample = "Skeleton|Welcome to our haunted house!\n"
            + "Spirit Ball|You should have seen it last year!\n"
            + "Boris 1|The neighbors still talk about it!\n"
            + "Boris 2|Well, most of them.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.rgb(15, 17, 28));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("ANIMATRONIC DIALOGUE STUDIO  •  v0.2");
        title.setTextSize(19);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        role = new Spinner(this);
        role.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Director", "Performer", "Audience"}));
        root.addView(role);

        character = new Spinner(this);
        character.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        root.addView(character);

        networkHelp = new TextView(this);
        networkHelp.setTextColor(Color.rgb(172, 216, 249));
        networkHelp.setTextSize(13);
        root.addView(networkHelp);
        updateNetworkHelp();

        Button refresh = new Button(this);
        refresh.setText("Refresh Network Addresses");
        refresh.setOnClickListener(v -> updateNetworkHelp());
        root.addView(refresh);

        host = new EditText(this);
        host.setSingleLine(true);
        host.setTextColor(Color.WHITE);
        host.setHint("Director IP address for manual connection");
        host.setHintTextColor(Color.LTGRAY);
        root.addView(host);

        Button start = new Button(this);
        start.setText("START DIRECTOR / JOIN BY ADDRESS");
        start.setOnClickListener(v -> {
            if (role.getSelectedItemPosition() == 0) startServer();
            else join(host.getText().toString().trim());
        });
        root.addView(start);

        Button find = new Button(this);
        find.setText("FIND DIRECTOR AUTOMATICALLY");
        find.setOnClickListener(v -> {
            if (role.getSelectedItemPosition() == 0) {
                show("Select Performer or Audience to find a Director. Director should use START.");
            } else {
                findDirector();
            }
        });
        root.addView(find);

        status = new TextView(this);
        status.setText("Not connected • Phones must share a local Wi-Fi network");
        status.setTextColor(Color.LTGRAY);
        status.setTextIsSelectable(true);
        root.addView(status);

        skull = new SkullView(this);
        root.addView(skull, new LinearLayout.LayoutParams(-1, 310));

        script = new EditText(this);
        script.setText(sample);
        script.setMinLines(5);
        script.setTextColor(Color.WHITE);
        script.setHint("Character|dialogue, one per line");
        root.addView(script);

        Button play = new Button(this);
        play.setText("PLAY SCRIPT (DIRECTOR)");
        play.setOnClickListener(v -> playScript());
        root.addView(play);

        Button test = new Button(this);
        test.setText("TEST MY CHARACTER");
        test.setOnClickListener(v -> speak("Testing one two three. Welcome to the haunted house!"));
        root.addView(test);

        Button stop = new Button(this);
        stop.setText("STOP PLAYBACK");
        stop.setOnClickListener(v -> {
            running = false;
            tts.stop();
            skull.setSpeaking(false);
            broadcast("STOP");
        });
        root.addView(stop);
    }

    private void updateNetworkHelp() {
        List<String> addresses = NetworkInfo.ipv4Addresses();
        String gateway = NetworkInfo.wifiGateway(this);
        String text = "This phone's local IPv4 addresses: "
                + (addresses.isEmpty() ? "none detected" : android.text.TextUtils.join(", ", addresses))
                + "\nWi-Fi gateway: " + (gateway == null ? "not detected" : gateway)
                + "\nDirector: start server and use an address shown above. "
                + "Performer: tap FIND DIRECTOR or enter the Director's address.";
        networkHelp.setText(text);
    }

    private void show(String message) { ui.post(() -> status.setText(message)); }

    private void startServer() {
        if (directorRunning) {
            show("Director server already running on port " + SHOW_PORT
                    + ".\n" + addressDescription());
            return;
        }
        directorRunning = true;
        io.execute(() -> {
            try {
                server = new ServerSocket(SHOW_PORT);
                show("DIRECTOR READY • port " + SHOW_PORT + "\n" + addressDescription()
                        + "\nConnect other phones to this hotspot or the same Wi-Fi.");
                listenForDiscovery();
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                    peers.add(writer);
                    show("Director connected • " + peers.size() + " peer(s)\n" + addressDescription());
                    io.execute(() -> read(socket, writer));
                }
            } catch (Exception error) {
                if (!isFinishing()) show("Director server error: " + error.getMessage());
            } finally {
                directorRunning = false;
            }
        });
    }

    private String addressDescription() {
        List<String> addresses = NetworkInfo.ipv4Addresses();
        return "Possible Director IPs: " + (addresses.isEmpty() ? "not detected"
                : android.text.TextUtils.join(", ", addresses));
    }

    /** The Director answers discovery requests; all data stays on local Wi-Fi. */
    private void listenForDiscovery() {
        io.execute(() -> {
            try {
                discoverySocket = new DatagramSocket(null);
                discoverySocket.setReuseAddress(true);
                discoverySocket.bind(new InetSocketAddress(DISCOVERY_PORT));
                discoverySocket.setSoTimeout(1500);
                while (directorRunning && !discoverySocket.isClosed()) {
                    byte[] bytes = new byte[128];
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    try {
                        discoverySocket.receive(packet);
                        String request = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                        if (!DISCOVER.equals(request)) continue;
                        byte[] reply = HERE.getBytes("UTF-8");
                        discoverySocket.send(new DatagramPacket(reply, reply.length,
                                packet.getAddress(), packet.getPort()));
                    } catch (SocketTimeoutException ignored) {}
                }
            } catch (Exception error) {
                if (directorRunning) show("Director TCP ready, but automatic discovery unavailable: "
                        + error.getMessage() + "\nManual IP entry still works.");
            } finally {
                if (discoverySocket != null) discoverySocket.close();
            }
        });
    }

    private Network wifiNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return null;
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities c = cm.getNetworkCapabilities(n);
                if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return n;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Prefer Wi-Fi even if Android marks cellular as its default Internet network. */
    private Socket socketOverWifi() throws IOException {
        Network wifi = wifiNetwork();
        if (wifi != null) return wifi.getSocketFactory().createSocket();
        return new Socket();
    }

    private void join(String address) {
        if (address.isEmpty()) {
            show("No Director IP entered. Tap FIND DIRECTOR AUTOMATICALLY instead.");
            return;
        }
        if (joining) return;
        joining = true;
        show("Connecting to " + address + ":" + SHOW_PORT + " ...");
        io.execute(() -> {
            boolean connected = tryJoin(address, 3500);
            joining = false;
            if (!connected) {
                show("Could not reach " + address + ":" + SHOW_PORT
                        + ". Check that Director says READY and both phones share Wi-Fi. "
                        + "The Wi-Fi gateway is not always the Director on a home router.");
            }
        });
    }

    private boolean tryJoin(String address, int timeoutMs) {
        Socket candidate = null;
        try {
            candidate = socketOverWifi();
            candidate.connect(new InetSocketAddress(address, SHOW_PORT), timeoutMs);
            Socket old = client;
            client = candidate;
            if (old != null && !old.isClosed()) old.close();
            out = new PrintWriter(new OutputStreamWriter(candidate.getOutputStream(), "UTF-8"), true);
            String label = role.getSelectedItemPosition() == 2 ? "Audience"
                    : character.getSelectedItem().toString();
            out.println("HELLO|" + label);
            show("CONNECTED TO DIRECTOR at " + address + ":" + SHOW_PORT
                    + "\nReady for dialogue.");
            final Socket current = candidate;
            io.execute(() -> read(current, null));
            return true;
        } catch (Exception error) {
            try { if (candidate != null) candidate.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    private void findDirector() {
        if (joining) return;
        joining = true;
        show("Searching for Director on local Wi-Fi...");
        io.execute(() -> {
            try {
                String gateway = NetworkInfo.wifiGateway(this);
                // On a phone hotspot the Wi-Fi gateway usually is the Director,
                // while on a normal Wi-Fi router this attempt simply fails.
                if (gateway != null && tryJoin(gateway, 850)) {
                    ui.post(() -> host.setText(gateway));
                    return;
                }
                try (DatagramSocket search = new DatagramSocket()) {
                    Network wifi = wifiNetwork();
                    if (wifi != null) wifi.bindSocket(search);
                    search.setBroadcast(true);
                    search.setSoTimeout(3500);
                    byte[] message = DISCOVER.getBytes("UTF-8");
                    for (InetAddress broadcast : NetworkInfo.broadcastAddresses()) {
                        try {
                            search.send(new DatagramPacket(message, message.length,
                                    broadcast, DISCOVERY_PORT));
                        } catch (Exception ignored) {}
                    }
                    // A unicast probe to the gateway helps on networks filtering broadcast.
                    if (gateway != null) {
                        try {
                            search.send(new DatagramPacket(message, message.length,
                                    InetAddress.getByName(gateway), DISCOVERY_PORT));
                        } catch (Exception ignored) {}
                    }
                    long deadline = System.currentTimeMillis() + 3600;
                    while (System.currentTimeMillis() < deadline) {
                        byte[] bytes = new byte[128];
                        DatagramPacket reply = new DatagramPacket(bytes, bytes.length);
                        try {
                            search.receive(reply);
                            String response = new String(reply.getData(), 0, reply.getLength(), "UTF-8");
                            if (!HERE.equals(response)) continue;
                            String address = reply.getAddress().getHostAddress();
                            if (tryJoin(address, 1600)) {
                                ui.post(() -> host.setText(address));
                                return;
                            }
                        } catch (SocketTimeoutException timeout) {
                            break;
                        }
                    }
                }
                show("Director not found automatically. Confirm Director is running and both "
                        + "phones share Wi-Fi. Refresh the Director's addresses and enter its "
                        + "local IP manually.");
            } catch (Exception e) {
                show("Auto-discovery unavailable: " + e.getMessage()
                        + "\nUse the Director IP field for manual connection.");
            } finally {
                joining = false;
            }
        });
    }

    private void read(Socket socket, PrintWriter peer) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("STOP")) {
                    running = false;
                    ui.post(() -> {
                        tts.stop();
                        skull.setSpeaking(false);
                    });
                } else if (line.startsWith("HELLO|") && peer != null) {
                    show("Director connected: " + line.substring(6)
                            + " • " + peers.size() + " peer(s)\n" + addressDescription());
                } else if (line.startsWith("LINE|")) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) dispatch(parts[1], parts[2]);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (peer != null) {
                peers.remove(peer);
                show("Director ready • " + peers.size() + " peer(s) remaining\n"
                        + addressDescription());
            } else if (!isFinishing()) {
                show("Connection to Director lost. Tap FIND DIRECTOR to reconnect.");
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void broadcast(String message) {
        for (PrintWriter writer : peers) writer.println(message);
    }

    private void dispatch(String who, String words) {
        ui.post(() -> {
            int mode = role.getSelectedItemPosition();
            String assigned = character.getSelectedItem().toString();
            if (mode == 2 || assigned.equals(who)) speak(words);
        });
    }

    private void speak(String words) {
        tts.stop();
        skull.setSpeaking(true);
        tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, "line");
        long duration = Math.max(1000, Math.min(10000, words.length() * 75L));
        ui.postDelayed(() -> skull.setSpeaking(false), duration);
    }

    private void playScript() {
        if (role.getSelectedItemPosition() != 0) {
            show("Start script playback from the Director.");
            return;
        }
        String text = script.getText().toString();
        running = true;
        io.execute(() -> {
            for (String line : text.split("\\n")) {
                if (!running) break;
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) continue;
                broadcast("LINE|" + parts[0] + "|" + parts[1]);
                dispatch(parts[0], parts[1]);
                try {
                    Thread.sleep(Math.max(1800, Math.min(10000, parts[1].length() * 85L)));
                } catch (InterruptedException ignored) { break; }
            }
            show("Script finished • " + peers.size() + " peer(s) connected.");
        });
    }

    @Override protected void onDestroy() {
        running = false;
        directorRunning = false;
        try {
            if (discoverySocket != null) discoverySocket.close();
            if (server != null) server.close();
            if (client != null) client.close();
        } catch (Exception ignored) {}
        if (tts != null) { tts.stop(); tts.shutdown(); }
        io.shutdownNow();
        super.onDestroy();
    }
}

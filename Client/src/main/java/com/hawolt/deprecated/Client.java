package com.hawolt.deprecated;

import com.hawolt.audio.SystemAudio;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import com.hawolt.source.impl.SoundcloudAudioSource;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Client extends WebSocketClient {
    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final List<StreamUpdateListener> list = new LinkedList<>();
    private final AbstractAudioSource source;
    private ScheduledFuture<?> future;
    private long timestamp;
    private String roomId;
    private Audio current;

    public Client(AbstractAudioSource source, URI serverUri) {
        super(serverUri);
        this.source = source;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Logger.debug("[websocket] connected at {}", System.currentTimeMillis());
    }

    private final Map<String, Consumer<JSONObject>> pending = new HashMap<>();

    public void execute(String command, Consumer<JSONObject> consumer, String... args) {
        String uuid = UUID.randomUUID().toString();
        JSONObject object = new JSONObject();
        object.put("command", command);
        object.put("uuid", uuid);
        object.put("args", String.join(" ", args));
        pending.put(uuid, consumer);
        Logger.debug("[websocket-out] {}", object.toString());
        send(object.toString());
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoomId() {
        return roomId;
    }


    public AudioSource getAudioSource() {
        return source;
    }


    @Override
    public void onMessage(String message) {
        Logger.debug("[websocket-in] {}", message);
        try {
            JSONObject object = new JSONObject(message);
            if (object.has("instruction")) {
                String instruction = object.getString("instruction");
                switch (instruction) {
                    case "list":
                        JSONArray users = object.getJSONArray("users");
                        GraphicalUserInterface.getModel().clear();
                        for (int i = 0; i < users.length(); i++) {
                            GraphicalUserInterface.getModel().addElement(users.getString(i));
                        }
                        break;
                    case "close":
                        SystemAudio.closeSourceDataLine();
                        break;
                    case "seek":
                        this.timestamp = object.getLong("timestamp");
                        break;
                    case "revalidate":
                        SystemAudio.closeSourceDataLine();
                        source.load(object.getString("url"));
                        break;
                    case "preload":
                        source.preload(object.getString("url"));
                        break;
                }
            } else if (object.has("uuid")) {
                String uuid = object.getString("uuid");
                if (!pending.containsKey(uuid)) return;
                pending.get(uuid).accept(object);
            }
        } catch (JSONException e) {
            Logger.error("Unable to parse message as JSON:{}", message);
            Logger.error(e);
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        if (this.future == null || future.isCancelled()) {
            this.future = service.scheduleAtFixedRate(() -> {
                Logger.debug("Attempting to reconnect...");
                reconnect();
                if (!isClosed()) future.cancel(true);
            }, 5, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onError(Exception e) {
        Logger.error(e.getMessage());
    }

    public void addStreamUpdateListener(StreamUpdateListener listener) {
        list.add(listener);
    }

    private static String host = "ws://hawolt.com:48157/?sudo=false&name=anon";

    private static Client createAndConnectClientInstance(AbstractAudioSource source) {
        try {
            URI uri = new URI(host);
            Client client = new Client(source, uri);
            client.connect();
            return client;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        AbstractAudioSource source = new SoundcloudAudioSource();
        Client client = createAndConnectClientInstance(source);

        FontUIResource resource = new FontUIResource("Dialog", Font.PLAIN, 18);
        GraphicalUserInterface.setUIFont(resource);
        JFrame frame = GraphicalUserInterface.create(client);

        JTextField username = GraphicalUserInterface.getUsernameField();
        JButton button = GraphicalUserInterface.getNameButton();
        button.addActionListener(listener -> {
            username.setEnabled(false);
            button.setEnabled(false);
            String name = username.getText();
            String roomId = client.getRoomId() == null ? "nil" : client.roomId;
            client.execute("name", object -> {
                boolean success = object.getString("result").equals(name);
                Logger.debug("name:{}", success);
            }, name, roomId);
        });

        do {
            try {
                client.current = source.await(100L);
                Optional<Audio> next = source.peek();
                long timestamp = System.currentTimeMillis();
                for (StreamUpdateListener listener : client.list) {
                    listener.onAudioUpdate(client.current, timestamp);
                    next.ifPresent(listener::onAudioPeekUpdate);
                }
                Logger.debug("[audio-player] now playing: {}", client.current.name());
                ByteArrayInputStream inputStream = new ByteArrayInputStream(client.current.data());

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                SystemAudio.setAudioInputStream(audioInputStream);
                SystemAudio.openSourceDataLine(audioInputStream.getFormat());

                byte[] buffer = new byte[4096];
                int read;

                if (GraphicalUserInterface.isHost()) {
                    client.execute("seek", object -> {
                        boolean success = object.getString("result").equals(client.getRoomId());
                        Logger.debug("seek:{}", success);
                    }, client.getRoomId(), String.valueOf(System.currentTimeMillis()));
                }

                while (SystemAudio.sourceDataLine.isOpen() && (read = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    int offset = client.getAudioPointerOffset(client.current.data());
                    if (offset != 0) {
                        Logger.debug("[player] seek to {}", offset);
                        audioInputStream.skip(offset);
                        continue;
                    }
                    SystemAudio.sourceDataLine.write(buffer, 0, read);
                }
                SystemAudio.closeSourceDataLine();
                audioInputStream.close();
            } catch (InterruptedException | UnsupportedAudioFileException | IOException e) {
                Logger.error(e);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.error(ex);
                }
            }
        } while (true);
    }

    public int getAudioPointerOffset(byte[] b) {
        if (timestamp == 0) return 0;
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(b));
            AudioFormat audioFormat = audioInputStream.getFormat();
            float sampleRate = audioFormat.getSampleRate();
            double timestampSeconds = (System.currentTimeMillis() - timestamp) / 1000D;
            double frameOffset = (timestampSeconds * sampleRate);
            this.timestamp = 0;
            audioInputStream.close();
            return (int) (frameOffset * audioFormat.getFrameSize());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
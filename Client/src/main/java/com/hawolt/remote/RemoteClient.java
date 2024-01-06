package com.hawolt.remote;

import com.hawolt.logger.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RemoteClient extends WebSocketClient {
    private static String host = "ws://hawolt.com:48157/?sudo=false&name=%s";

    public static RemoteClient createAndConnectClientInstance(String username) {
        try {
            URI uri = new URI(String.format(host, username));
            RemoteClient client = new RemoteClient(uri);
            client.connect();
            return client;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Consumer<JSONObject>> pending = new HashMap<>();
    private final List<InstructionListener> list = new LinkedList<>();
    private final Map<String, JSONObject> heap = new HashMap<>();
    private ScheduledFuture<?> future;

    public RemoteClient(URI serverUri) {
        super(serverUri);
    }

    public void addInstructionListener(InstructionListener listener) {
        this.list.add(listener);
    }

    private JSONObject createMessage(String command, String... args) {
        JSONObject object = new JSONObject();
        object.put("command", command);
        object.put("uuid", UUID.randomUUID().toString());
        object.put("args", String.join(" ", args));
        return object;
    }

    public String executeAsynchronous(String command, Consumer<JSONObject> consumer, String... args) {
        JSONObject object = createMessage(command, args);
        String uuid = object.getString("uuid");
        pending.put(uuid, consumer);
        Logger.debug("[websocket-out] {}", object.toString());
        send(object.toString());
        return uuid;
    }

    public JSONObject executeBlocking(String command, String... args) {
        JSONObject object = createMessage(command, args);
        String uuid = object.getString("uuid");
        Logger.debug("[websocket-out] {}", object.toString());
        send(object.toString());
        while (!heap.containsKey(uuid)) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return heap.remove(uuid);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Logger.debug("[websocket] connected at {}", System.currentTimeMillis());
    }

    @Override
    public void onMessage(String message) {
        Logger.debug("[websocket-in] {}", message);
        try {
            JSONObject object = new JSONObject(message);
            if (object.has("instruction")) {
                for (InstructionListener listener : list) {
                    listener.onInstruction(object);
                }
            } else if (object.has("uuid")) {
                String uuid = object.getString("uuid");
                if (!pending.containsKey(uuid)) {
                    heap.put(uuid, object);
                } else {
                    pending.get(uuid).accept(object);
                }
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
}
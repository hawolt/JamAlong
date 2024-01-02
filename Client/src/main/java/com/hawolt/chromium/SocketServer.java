package com.hawolt.chromium;

import com.hawolt.logger.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.event.WindowEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SocketServer extends WebSocketServer {
    public static final List<String> cache = new LinkedList<>();
    private static final List<String> queue = new ArrayList<>();
    private static SocketServer instance;
    private static boolean connected;

    public SocketServer(InetSocketAddress address) {
        super(address);
    }

    public static void launch(int websocketPort) {
        SocketServer.instance = new SocketServer(new InetSocketAddress(websocketPort));
        SocketServer.instance.start();
    }

    public static void forward(String message) {
        SocketServer.cache.add(message);
        if (SocketServer.connected) {
            instance.broadcast(message);
        } else {
            SocketServer.queue.add(message);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Logger.debug("Chromium connected to local WebSocket server");
        if (SocketServer.connected = true) {
            for (String message : queue) {
                forward(message);
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Logger.debug("Chromium disconnected from local WebSocket server");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Logger.error(e);
    }

    @Override
    public void onStart() {
        Logger.debug("Started WebSocket server");
    }
}

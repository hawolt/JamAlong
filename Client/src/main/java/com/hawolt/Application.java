package com.hawolt;

import com.hawolt.audio.AudioManager;
import com.hawolt.chromium.SocketServer;
import com.hawolt.discord.RichPresence;
import com.hawolt.localhost.LocalExecutor;
import com.hawolt.media.impl.SoundcloudAudioSource;
import com.hawolt.misc.Debouncer;
import com.hawolt.remote.RemoteClient;
import com.hawolt.settings.SettingManager;

import java.net.ServerSocket;
import java.util.Optional;

/**
 * Utility class to manage all Objects required to run JamAlong
 */
public class Application {
    private final Debouncer debouncer = new Debouncer();
    private Optional<RichPresence> richPresence;
    private SoundcloudAudioSource audioSource;
    private SettingManager settingManager;
    private LocalExecutor localExecutor;
    private RemoteClient remoteClient;
    private SocketServer socketServer;
    private AudioManager audioManager;
    private ServerSocket serverSocket;
    private int websocketPort;
    private boolean graceful;
    private String version;

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public Debouncer getDebouncer() {
        return debouncer;
    }

    public void setSettingManager(SettingManager settingManager) {
        this.settingManager = settingManager;
    }

    public SettingManager getSettingManager() {
        return settingManager;
    }

    public void setAudioSource(SoundcloudAudioSource audioSource) {
        this.audioSource = audioSource;
    }

    public SoundcloudAudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public void setSocketServer(SocketServer socketServer) {
        this.socketServer = socketServer;
    }

    public SocketServer getSocketServer() {
        return socketServer;
    }

    public void setRichPresence(Optional<RichPresence> richPresence) {
        this.richPresence = richPresence;
    }

    public Optional<RichPresence> getRichPresence() {
        return richPresence;
    }

    public void setRemoteClient(RemoteClient remoteClient) {
        this.remoteClient = remoteClient;
    }

    public RemoteClient getRemoteClient() {
        return remoteClient;
    }

    public void setLocalExecutor(LocalExecutor localExecutor) {
        this.localExecutor = localExecutor;
    }

    public LocalExecutor getLocalExecutor() {
        return localExecutor;
    }

    public void setWebSocketPort(int websocketPort) {
        this.websocketPort = websocketPort;
    }

    public int getWebSocketPort() {
        return websocketPort;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setGracefulShutdown(boolean b) {
        this.graceful = b;
    }

    public boolean isGraceful() {
        return graceful;
    }

    public void nullifyServerSocket() {
        if (serverSocket == null) return;
        this.serverSocket = null;
    }
}

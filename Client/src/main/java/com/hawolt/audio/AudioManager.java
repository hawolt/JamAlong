package com.hawolt.audio;

import com.hawolt.Application;
import com.hawolt.Main;
import com.hawolt.cryptography.SHA256;
import com.hawolt.discord.RichPresence;
import com.hawolt.localhost.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.exceptions.AudioMixerUnavailableException;
import com.hawolt.logger.Logger;
import com.hawolt.misc.HostType;
import com.hawolt.remote.InstructionListener;
import com.hawolt.remote.RemoteClient;
import com.hawolt.media.Audio;
import com.hawolt.media.AudioSource;
import com.hawolt.media.StreamUpdateListener;
import com.hawolt.media.impl.AbstractAudioSource;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioManager implements Runnable, InstructionListener {
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final List<StreamUpdateListener> list = new LinkedList<>();
    private final List<String> skip = new ArrayList<>();
    private final AbstractAudioSource source;
    private final RemoteClient remoteClient;
    private final AudioSystemWrapper audio;
    private final Application application;
    private boolean gatekeeper = true;
    private long timestamp;
    private Audio current;

    private AudioManager(Application application) throws AudioMixerUnavailableException {
        this.application = application;
        this.source = application.getAudioSource();
        this.audio = new AudioSystemWrapper();
        this.remoteClient = application.getRemoteClient();
        this.remoteClient.addInstructionListener(this);
    }

    public static AudioManager start(Application application) throws AudioMixerUnavailableException {
        AudioManager playbackHandler = new AudioManager(application);
        playbackHandler.service.execute(playbackHandler);
        return playbackHandler;
    }

    public void addStreamUpdateListener(StreamUpdateListener listener) {
        this.list.add(listener);
    }

    public void clearStreamUpdateListeners() {
        this.list.clear();
    }

    public void stop() {
        Thread.currentThread().interrupt();
        service.shutdown();
    }

    public void reset() {
        this.source.clear();
        this.audio.closeSourceDataLine();
    }

    @Override
    public void run() {
        do {
            if (Thread.currentThread().isInterrupted()) break;
            try {
                current = source.await(100L);
                Optional<Audio> next = source.peek();
                long timestamp = System.currentTimeMillis();
                for (StreamUpdateListener listener : list) {
                    listener.onAudioUpdate(current, timestamp);
                    next.ifPresent(listener::onAudioPeekUpdate);
                }
                Logger.info("[audio-player] now playing: {}", SHA256.hash(current.name()));
                if (!gatekeeper) revealCurrentlyPlayingSong();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(current.data());

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                this.audio.setAudioInputStream(audioInputStream);
                this.audio.openSourceDataLine(audioInputStream.getFormat());

                byte[] buffer = new byte[256];
                int read;

                LocalExecutor localExecutor = application.getLocalExecutor();
                if (localExecutor.getHostType() == HostType.HOST) {
                    remoteClient.executeAsynchronous("seek", object -> {
                        boolean success = object.getString("result").equals(localExecutor.getPartyId());
                        Logger.debug("seek:{}", success);
                    }, localExecutor.getPartyId(), String.valueOf(System.currentTimeMillis()));
                }

                Optional<RichPresence> richPresence = application.getRichPresence();
                richPresence.ifPresent(presence -> presence.set(localExecutor.getPartyId()));
                checkSkipList();
                while (this.audio.sourceDataLine.isOpen() && (read = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    checkSkipList();
                    int offset = getAudioPointerOffset(current.data());
                    if (offset != 0) {
                        Logger.debug("[player] seek to {} of {}", offset, current.data().length);
                        audioInputStream.skip(offset);
                        continue;
                    }
                    this.audio.sourceDataLine.write(buffer, 0, read);
                }
                Logger.info("stopped playing {}", SHA256.hash(current.name()));
                this.audio.closeSourceDataLine();
                audioInputStream.close();
            } catch (InterruptedException | UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                Logger.error(e);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.error(ex);
                }
            }
        } while (true);
    }

    private void revealCurrentlyPlayingSong() {
        JSONObject track = new JSONObject();
        track.put("instruction", "reveal");
        track.put("name", current.name());
        application.getSocketServer().forward(track.toString());
    }

    private void hideCurrentlyPlayingSong() {
        JSONObject track = new JSONObject();
        track.put("instruction", "reveal");
        track.put("name", "");
        application.getSocketServer().forward(track.toString());
    }

    private void checkSkipList() {
        if (!skip.contains(current.source())) return;
        Logger.info("[player] skip {}", SHA256.hash(current.name()));
        skip.remove(current.source());
        this.audio.closeSourceDataLine();
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

    public void skip() {
        LocalExecutor localExecutor = application.getLocalExecutor();
        remoteClient.executeAsynchronous("skip", object -> {
            boolean success = object.getString("result").equals(localExecutor.getPartyId());
            Logger.debug("skip:{}", success);
        }, localExecutor.getPartyId(), current.source());
        this.audio.closeSourceDataLine();
    }

    public AudioSource getAudioSource() {
        return source;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Audio getCurrent() {
        return current;
    }

    @Override
    public void onInstruction(JSONObject object) {
        SocketServer socketServer = application.getSocketServer();
        String instruction = object.getString("instruction");
        switch (instruction) {
            case "chat", "list":
                socketServer.forward(object.toString());
                break;
            case "rediscover":
                if (application.getLocalExecutor().getHostType() == HostType.UNKNOWN) {
                    socketServer.forward(object.toString());
                }
                break;
            case "gatekeeper":
                socketServer.forward(object.toString());
                this.gatekeeper = object.getBoolean("status");
                if (!gatekeeper && current != null) revealCurrentlyPlayingSong();
                else hideCurrentlyPlayingSong();
                Optional<RichPresence> richPresence = application.getRichPresence();
                String partyId = application.getLocalExecutor().getPartyId();
                richPresence.ifPresent(presence -> presence.set(partyId));
                break;
            case "reset-gatekeeper":
                this.gatekeeper = true;
                break;
            case "skip":
                String toSkip = object.getString("track");
                this.skip.add(toSkip);
                break;
            case "close":
                this.source.clear();
                this.audio.closeSourceDataLine();
                socketServer.forward(object.toString());
                break;
            case "seek":
                this.timestamp = object.getLong("timestamp");
                break;
            case "revalidate":
                this.audio.closeSourceDataLine();
                source.load(object.getString("url"));
                break;
            case "preload":
                source.preload(object.getString("url"));
                break;
        }
    }

    public void setGatekeeper(boolean gatekeeper) {
        this.gatekeeper = gatekeeper;
    }

    public AudioSystemWrapper getSystemAudio() {
        return audio;
    }

    public boolean isGatekeeper() {
        return gatekeeper;
    }
}

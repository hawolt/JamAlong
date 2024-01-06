package com.hawolt;

import com.hawolt.audio.SystemAudio;
import com.hawolt.localhost.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.exceptions.AudioMixerUnavailableException;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaybackHandler implements Runnable, InstructionListener {
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final List<StreamUpdateListener> list = new LinkedList<>();
    private final List<String> skip = new ArrayList<>();

    private final AbstractAudioSource source;
    private final RemoteClient remoteClient;
    private final SystemAudio audio;
    private long timestamp;
    private Audio current;
    private long reset;

    private PlaybackHandler(RemoteClient remoteClient, AbstractAudioSource source) throws AudioMixerUnavailableException {
        this.source = source;
        this.audio = new SystemAudio();
        this.remoteClient = remoteClient;
        this.remoteClient.addInstructionListener(this);
    }

    public static PlaybackHandler start(RemoteClient remoteClient, AbstractAudioSource source) throws AudioMixerUnavailableException {
        PlaybackHandler playbackHandler = new PlaybackHandler(remoteClient, source);
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
                Logger.debug("[audio-player] now playing: {}", current.name());
                ByteArrayInputStream inputStream = new ByteArrayInputStream(current.data());

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                this.audio.setAudioInputStream(audioInputStream);
                this.audio.openSourceDataLine(audioInputStream.getFormat());

                byte[] buffer = new byte[4096];
                int read;

                if (LocalExecutor.HOST_TYPE == HostType.HOST) {
                    remoteClient.executeAsynchronous("seek", object -> {
                        boolean success = object.getString("result").equals(LocalExecutor.PARTY_ID);
                        Logger.debug("seek:{}", success);
                    }, LocalExecutor.PARTY_ID, String.valueOf(System.currentTimeMillis()));
                }
                Main.presence.ifPresent(presence -> presence.set(LocalExecutor.PARTY_ID));
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
                Logger.debug("stopped playing {}", current.name());
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

    private void checkSkipList() {
        if (!skip.contains(current.source())) return;
        Logger.debug("[player] skip {}", current.source());
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
        remoteClient.executeAsynchronous("skip", object -> {
            boolean success = object.getString("result").equals(LocalExecutor.PARTY_ID);
            Logger.debug("skip:{}", success);
        }, LocalExecutor.PARTY_ID, current.source());
        this.audio.closeSourceDataLine();
    }

    public AudioSource getAudioSource() {
        return source;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void onInstruction(JSONObject object) {
        String instruction = object.getString("instruction");
        switch (instruction) {
            case "skip":
                String toSkip = object.getString("track");
                this.skip.add(toSkip);
                break;
            case "list":
                SocketServer.forward(object.toString());
                break;
            case "close":
                SocketServer.forward(object.toString());
                this.audio.closeSourceDataLine();
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

    public SystemAudio getSystemAudio() {
        return audio;
    }
}

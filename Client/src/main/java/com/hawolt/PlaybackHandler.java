package com.hawolt;

import com.hawolt.audio.SystemAudio;
import com.hawolt.chromium.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import org.json.JSONObject;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaybackHandler implements Runnable, InstructionListener {
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final List<StreamUpdateListener> list = new LinkedList<>();
    private final AbstractAudioSource source;
    private final RemoteClient remoteClient;
    private long timestamp;
    private Audio current;

    private PlaybackHandler(RemoteClient remoteClient, AbstractAudioSource source) {
        this.source = source;
        this.remoteClient = remoteClient;
        this.remoteClient.addInstructionListener(this);
    }

    public static PlaybackHandler start(RemoteClient remoteClient, AbstractAudioSource source) {
        PlaybackHandler playbackHandler = new PlaybackHandler(remoteClient, source);
        playbackHandler.service.execute(playbackHandler);
        return playbackHandler;
    }

    public void stop() {
        Thread.currentThread().interrupt();
        service.shutdown();
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
                SystemAudio.setAudioInputStream(audioInputStream);
                SystemAudio.openSourceDataLine(audioInputStream.getFormat());

                byte[] buffer = new byte[4096];
                int read;

                if (LocalExecutor.HOST_TYPE == HostType.HOST) {
                    remoteClient.executeAsynchronous("seek", object -> {
                        boolean success = object.getString("result").equals(LocalExecutor.PARTY_ID);
                        Logger.debug("seek:{}", success);
                    }, LocalExecutor.PARTY_ID, String.valueOf(System.currentTimeMillis()));
                }

                while (SystemAudio.sourceDataLine.isOpen() && (read = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    int offset = getAudioPointerOffset(current.data());
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
        SocketServer.forward(object.toString());
    }
}

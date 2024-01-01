package com.hawolt.audio;

import com.hawolt.logger.Logger;

import javax.sound.sampled.*;
import java.util.LinkedList;
import java.util.List;

public class SystemAudio {
    private static final Line.Info PLAYBACK_DEVICE = new Line.Info(SourceDataLine.class);
    private static final AudioFormat BASE_AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            AudioSystem.NOT_SPECIFIED,
            16, 2, 4,
            AudioSystem.NOT_SPECIFIED, true
    );
    private static final DataLine.Info BASE_DATA_LINE = new DataLine.Info(Clip.class, BASE_AUDIO_FORMAT);
    private static final List<AudioOutputDevice> SUPPORTED = new LinkedList<>();
    public static AudioOutputDevice SELECTED_MIXER;
    public static SourceDataLine sourceDataLine;
    public static AudioFormat audioFormat;
    public static float gain;

    static {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(PLAYBACK_DEVICE) && mixer.isLineSupported(BASE_DATA_LINE)) {
                SUPPORTED.add(new AudioOutputDevice(mixer));
            }
        }
        setSelectedAudioOutputDevice(getDefaultMixer());
    }

    public static void closeSourceDataLine() {
        if (sourceDataLine == null) return;
        sourceDataLine.stop();
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    public static void setSelectedAudioOutputDevice(AudioOutputDevice device) {
        SystemAudio.closeSourceDataLine();
        SystemAudio.SELECTED_MIXER = device;
    }

    public static void setAudioInputStream(AudioInputStream audioInputStream) {
        SystemAudio.audioFormat = audioInputStream.getFormat();
    }

    public static void openSourceDataLine(AudioFormat audioFormat) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            SystemAudio.sourceDataLine = (SourceDataLine) SystemAudio.SELECTED_MIXER.mixer().getLine(info);
            SystemAudio.sourceDataLine.open(audioFormat);
            SystemAudio.sourceDataLine.start();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        SystemAudio.setGain(SystemAudio.gain);
    }

    public static List<AudioOutputDevice> getSupportedOutputDeviceList() {
        return SUPPORTED;
    }

    public static AudioOutputDevice getDefaultMixer() {
        if (SUPPORTED.isEmpty()) return null;
        return SUPPORTED.get(0);
    }

    public static void setGain(float value) {
        SystemAudio.gain = value;
        Logger.debug("[system-audio] changed gain to {}", value);
        if (SystemAudio.sourceDataLine == null) return;
        FloatControl control = (FloatControl) SystemAudio.sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
        control.setValue(value);
    }
}

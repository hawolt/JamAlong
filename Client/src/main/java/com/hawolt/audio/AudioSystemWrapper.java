package com.hawolt.audio;

import com.hawolt.exceptions.AudioMixerUnavailableException;
import com.hawolt.exceptions.GainOutOfBoundsException;

import javax.sound.sampled.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to manage System audio devices and audio playback behaviour.
 */
public class AudioSystemWrapper {
    private static final Line.Info PLAYBACK_DEVICE = new Line.Info(SourceDataLine.class);
    private static final AudioFormat BASE_AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            AudioSystem.NOT_SPECIFIED,
            16, 2, 4,
            AudioSystem.NOT_SPECIFIED, true
    );
    private static final DataLine.Info BASE_DATA_LINE = new DataLine.Info(Clip.class, BASE_AUDIO_FORMAT);
    private final List<AudioOutputDevice> availableOutputDeviceList = new LinkedList<>();
    private float gain = -40f;


    public AudioOutputDevice audioOutputDevice;
    public SourceDataLine sourceDataLine;
    public AudioFormat audioFormat;

    public AudioSystemWrapper() throws AudioMixerUnavailableException {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(PLAYBACK_DEVICE) && mixer.isLineSupported(BASE_DATA_LINE)) {
                availableOutputDeviceList.add(new AudioOutputDevice(mixer));
            }
        }
        this.setSelectedAudioOutputDevice(getDefaultMixer());
    }

    /**
     * configures the AudioOutputDevice
     *
     * @param device the AudioOutputDevice that should be used for audio playback
     */
    public void setSelectedAudioOutputDevice(AudioOutputDevice device) {
        this.closeSourceDataLine();
        this.audioOutputDevice = device;
    }

    /**
     * configures the AudioInputStream
     *
     * @param audioInputStream the AudioInputStream which has the required Format
     *                         to open a new SourceDataLine when switching AudioOutputDevice
     */
    public void setAudioInputStream(AudioInputStream audioInputStream) {
        this.audioFormat = audioInputStream.getFormat();
    }

    /**
     * opens a SourceDataLine for audio playback
     *
     * @param audioFormat the AudioFormat required to open a new SourceDataLine
     * @throws LineUnavailableException when unable to open a new SourceDataLine
     */
    public void openSourceDataLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        this.sourceDataLine = (SourceDataLine) this.audioOutputDevice.mixer().getLine(info);
        this.sourceDataLine.open(audioFormat);
        this.sourceDataLine.start();
        this.setGain(gain);
    }

    /**
     * used to get a list of available AudioOutputDevices
     *
     * @return a list with every available AudioOutputDevice
     */
    public List<AudioOutputDevice> getAvailableOutputDeviceList() {
        return availableOutputDeviceList;
    }

    /**
     * used to get the default AudioOutputDevice
     *
     * @return the default AudioOutputDevice
     */
    public AudioOutputDevice getDefaultMixer() throws AudioMixerUnavailableException {
        if (availableOutputDeviceList.isEmpty()) throw new AudioMixerUnavailableException();
        return availableOutputDeviceList.get(0);
    }

    /**
     * stops the current SourceDataLine used to write audio data
     */
    public void closeSourceDataLine() {
        if (sourceDataLine == null) return;
        this.sourceDataLine.stop();
        this.sourceDataLine.drain();
        this.sourceDataLine.close();
    }

    /**
     * sets the Gain for audio playback
     *
     * @param value the Gain value to set
     * @throws GainOutOfBoundsException thrown when the Gain value to set is lower than -80 or higher than 6
     */
    public void setGain(float value) throws GainOutOfBoundsException {
        if (value < -80 || value > 6) throw new GainOutOfBoundsException(value);
        this.setGainInternally(value);
        if (sourceDataLine == null) return;
        this.adjustFloatControl();
    }

    private void setGainInternally(float gain) {
        this.gain = gain;
    }

    private void adjustFloatControl() {
        FloatControl control = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
        control.setValue(gain);
    }

    public float getGain() {
        return gain;
    }
}

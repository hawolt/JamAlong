package com.hawolt.audio;

import javax.sound.sampled.Mixer;
import java.util.Objects;

/**
 * Wrapper class to see the actual Name of the audio output device.
 *
 * @param mixer the Mixer representing an audio output device
 */
public record AudioOutputDevice(Mixer mixer) {

    @Override
    public String toString() {
        return mixer.getMixerInfo().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioOutputDevice device = (AudioOutputDevice) o;
        return Objects.equals(mixer, device.mixer);
    }

}

package com.hawolt.exceptions;

/**
 * to be thrown when no Mixer is able to play back audio data in the required format.
 */
public class AudioMixerUnavailableException extends Exception {
    public AudioMixerUnavailableException() {
        super("Unable to find a valid playback device");
    }
}

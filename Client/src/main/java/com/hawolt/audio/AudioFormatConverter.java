package com.hawolt.audio;

import com.hawolt.logger.Logger;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Utility Class to convert from MP3 audio data to WAVE audio data.
 */
public class AudioFormatConverter {
    /**
     * Used to convert MP3 audio data to WAVE format.
     *
     * @param b the MP3 audio data
     * @return audio data in WAVE format
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public static ByteArrayOutputStream convertToWaveFormatFromMP3(byte[] b) throws UnsupportedAudioFileException, IOException {
        return convertToWaveFormatFromMP3(new ByteArrayInputStream(b));
    }

    /**
     * Used to convert a stream of MP3 audio data to WAVE format.
     *
     * @param stream the Stream containing MP3 audio data
     * @return audio data in WAVE format
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public static ByteArrayOutputStream convertToWaveFormatFromMP3(InputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(new BufferedInputStream(stream));
        AudioFormat sourceFormat = mp3Stream.getFormat();
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(), 16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false);
        AudioInputStream converted = AudioSystem.getAudioInputStream(format, mp3Stream);
        File file = File.createTempFile(UUID.randomUUID().toString(), ".sct");
        AudioSystem.write(converted, AudioFileFormat.Type.WAVE, file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(Files.readAllBytes(file.toPath()));
        if (file.delete()) {
            Logger.debug("[audio-format-converter] deleted temporary conversion file:{}", file.getName());
        }
        return outputStream;
    }
}

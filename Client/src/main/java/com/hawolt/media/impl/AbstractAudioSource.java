package com.hawolt.media.impl;

import com.hawolt.cryptography.SHA256;
import com.hawolt.logger.Logger;
import com.hawolt.media.Audio;
import com.hawolt.media.AudioSource;

import java.util.LinkedList;
import java.util.Optional;

public abstract class AbstractAudioSource implements AudioSource {
    private final LinkedList<Audio> list = new LinkedList<>();

    @Override
    public void push(Audio audio) {
        Logger.info("PUSH AUDIO: {}", SHA256.hash(audio.name()));
        list.add(audio);
    }

    @Override
    public Audio pop() {
        Audio audio = list.isEmpty() ? null : list.remove(0);
        if (audio != null) Logger.info("POP AUDIO: {}", SHA256.hash(audio.name()));
        if (list.size() <= 4 && getCurrentlyLoadingReferences() == 0) {
            loadNextPendingReference();
        }
        return audio;
    }

    @Override
    public Audio await(long interval) throws InterruptedException {
        Audio audio;
        while ((audio = pop()) == null) {
            Thread.sleep(interval);
        }
        return audio;
    }

    @Override
    public Optional<Audio> peek() {
        Audio audio = list.isEmpty() ? null : list.get(0);
        return Optional.ofNullable(audio);
    }

    @Override
    public Audio peek(long interval) throws InterruptedException {
        Optional<Audio> audio;
        while ((audio = peek()).isEmpty()) {
            Thread.sleep(interval);
        }
        return audio.get();
    }

    @Override
    public LinkedList<Audio> getCurrentQueue() {
        return list;
    }
}

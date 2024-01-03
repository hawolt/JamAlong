package com.hawolt.source.impl;

import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;

import java.util.LinkedList;
import java.util.Optional;

public abstract class AbstractAudioSource implements AudioSource {
    private final LinkedList<Audio> list = new LinkedList<>();

    @Override
    public void push(Audio audio) {
        Logger.debug("pushing: {}", audio.name());
        list.push(audio);
    }

    @Override
    public Audio pop() {
        Audio audio = list.isEmpty() ? null : list.remove(0);
        if (audio != null) Logger.debug("popping: {}", audio.name());
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
    public void clear() {
        this.list.clear();
    }
}

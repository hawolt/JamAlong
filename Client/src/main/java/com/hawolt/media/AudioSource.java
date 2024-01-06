package com.hawolt.media;

import java.util.Optional;

public interface AudioSource {

    Audio await(long interval) throws InterruptedException;

    Audio peek(long interval) throws InterruptedException;

    void setPartyLeaveTimestamp(long timestamp);

    void preload(String path);

    void load(String path);

    void push(Audio audio);

    Optional<Audio> peek();

    void clear();

    Audio pop();
}

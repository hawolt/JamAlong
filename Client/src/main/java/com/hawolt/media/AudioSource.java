package com.hawolt.media;

import java.util.LinkedList;
import java.util.Optional;

public interface AudioSource {
    LinkedList<Audio> getCurrentQueue();

    Audio await(long interval) throws InterruptedException;

    Audio peek(long interval) throws InterruptedException;

    int getCurrentlyLoadingReferences();

    void setPartyLeaveTimestamp(long timestamp);

    void preload(String path);

    void load(String path);

    void loadNextPendingReference();

    void push(Audio audio);

    Optional<Audio> peek();

    void clear();

    Audio pop();
}

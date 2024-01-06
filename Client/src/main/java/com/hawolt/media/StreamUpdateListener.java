package com.hawolt.media;

public interface StreamUpdateListener {
    void onAudioUpdate(Audio audio, long timestamp);

    void onAudioPeekUpdate(Audio audio);
}

package com.hawolt.source;

public interface StreamUpdateListener {
    void onAudioUpdate(Audio audio, long timestamp);

    void onAudioPeekUpdate(Audio audio);
}

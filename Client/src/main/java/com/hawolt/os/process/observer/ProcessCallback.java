package com.hawolt.os.process.observer;

public interface ProcessCallback {
    void onStateChange(State state);

    enum State {
        UNKNOWN, STARTED, RUNNING, TERMINATED, NOT_RUNNING
    }
}

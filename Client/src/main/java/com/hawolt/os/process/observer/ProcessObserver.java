package com.hawolt.os.process.observer;

import com.hawolt.os.SystemManager;

import java.io.IOException;


public class ProcessObserver implements Runnable {
    private final ProcessCallback callback;
    private final String name;
    private ProcessCallback.State state = ProcessCallback.State.UNKNOWN;

    public ProcessObserver(String name, ProcessCallback callback) {
        this.callback = callback;
        this.name = name;
    }

    @Override
    public void run() {
        ProcessCallback.State current;
        try {
            boolean running = SystemManager.getInstance().isProcessRunning(name);
            if (running) {
                if (state == ProcessCallback.State.RUNNING || state == ProcessCallback.State.STARTED) {
                    current = ProcessCallback.State.RUNNING;
                } else {
                    current = ProcessCallback.State.STARTED;
                }
            } else {
                if (state == ProcessCallback.State.STARTED || state == ProcessCallback.State.RUNNING) {
                    current = ProcessCallback.State.TERMINATED;
                } else {
                    current = ProcessCallback.State.NOT_RUNNING;
                }
            }
        } catch (IOException e) {
            current = ProcessCallback.State.UNKNOWN;
        }
        if (state != current) callback.onStateChange(current);
        this.state = current;
    }
}

package com.hawolt.os.process.impl;

import com.hawolt.os.process.ProcessReference;

public class WindowsProcess extends ProcessReference {
    public WindowsProcess(String line) {
        super(line);
    }

    @Override
    public void configure(String line) {
        int lastIndex = line.lastIndexOf(" ");
        String pid = line.substring(lastIndex + 1);
        String remainder = line.substring(0, lastIndex);
        this.name = remainder.trim();
        this.pid = Integer.parseInt(pid);
    }
}

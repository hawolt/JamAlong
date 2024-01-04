package com.hawolt.os.process.impl;

import com.hawolt.os.process.ProcessReference;

public class WindowsProcess extends ProcessReference {
    private String command;

    public WindowsProcess(String line) {
        super(line);
    }

    @Override
    public void configure(String line) {
        int lastIndex = line.lastIndexOf(" ");
        String pid = line.substring(lastIndex + 1);
        String remainder = line.substring(0, lastIndex);
        String[] data = remainder.split(" ", 2);
        this.name = data[0];
        this.command = data.length == 1 ? "" : data[1];
        this.pid = Integer.parseInt(pid);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "WindowsProcess{" +
                "command='" + command + '\'' +
                ", name='" + name + '\'' +
                ", pid=" + pid +
                '}';
    }
}

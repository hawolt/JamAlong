package com.hawolt.os.process.impl;

import com.hawolt.os.process.ProcessReference;

public class UnixProcess extends ProcessReference {

    public UnixProcess(String line) {
        super(line);
    }

    @Override
    public void configure(String line) {
        String[] data = line.split(" ", 12);
        this.name = data[10];
        this.pid = Integer.parseInt(data[1]);
        if (name.startsWith("[") && name.endsWith("]")) {
            this.name = name.substring(1, name.length() - 1);
        } else if (name.contains("/")) {
            this.name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name.startsWith("-")) {
            this.name = name.substring(1);
        }
        if (name.endsWith(":")) {
            this.name = name.substring(0, name.length() - 1);
        }
        this.name = name.trim();
    }

}

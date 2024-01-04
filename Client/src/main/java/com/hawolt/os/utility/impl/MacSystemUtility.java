package com.hawolt.os.utility.impl;

import com.hawolt.os.process.ProcessReference;
import com.hawolt.os.process.impl.UnixProcess;
import com.hawolt.os.utility.BasicSystemUtility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MacSystemUtility extends BasicSystemUtility {
    @Override
    public String translate(String path) {
        return path;
    }

    @Override
    public List<ProcessReference> getProcessList() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("ps", "aux");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<ProcessReference> references = new ArrayList<>();
        try (InputStream stream = process.getInputStream()) {
            String[] list = readStream(stream).split("\n");
            for (int i = 1; i < list.length; i++) {
                String line = list[i];
                line = line.trim().replaceAll(" +", " ");
                if (line.isEmpty()) continue;
                references.add(new UnixProcess(line));
            }
        }
        return references;
    }

    @Override
    public void kill(int pid) throws IOException {

    }
}

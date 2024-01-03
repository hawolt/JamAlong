package com.hawolt.os.utility;


import com.hawolt.os.process.ProcessReference;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

public abstract class BasicSystemUtility implements SystemUtility {
    protected static @NotNull String readStream(InputStream stream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    @Override
    public Optional<ProcessReference> getProcessByPID(int pid) throws IOException {
        return getProcessList()
                .stream()
                .filter(process -> process.getPID() == pid)
                .findAny();
    }

    @Override
    public int[] getProcessByName(String name) throws IOException {
        return getProcessList()
                .stream()
                .filter(process -> process.getName().equals(name))
                .mapToInt(ProcessReference::getPID)
                .toArray();
    }

    @Override
    public boolean isProcessRunning(String name) throws IOException {
        return getProcessByName(name).length > 0;
    }
}

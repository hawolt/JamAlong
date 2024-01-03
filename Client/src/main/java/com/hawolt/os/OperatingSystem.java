package com.hawolt.os;

import java.util.Locale;

public class OperatingSystem {
    protected static OSType detectedOS;

    public static OSType getOperatingSystemType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin"))) {
                detectedOS = OSType.MAC;
            } else if (OS.contains("win")) {
                detectedOS = OSType.WINDOWS;
            } else if (OS.contains("nux") || OS.contains("nix")) {
                detectedOS = OSType.LINUX;
            } else {
                detectedOS = OSType.UNKNOWN;
            }
        }
        return detectedOS;
    }

    public enum OSType {
        WINDOWS, MAC, LINUX, UNKNOWN
    }
}

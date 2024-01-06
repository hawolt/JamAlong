package com.hawolt.misc;

import java.io.IOException;
import java.util.Locale;

public class Network {
    public static void browse(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        Runtime rt = Runtime.getRuntime();
        if (os.contains("mac")) {
            rt.exec("open " + url);
        } else if (os.contains("nix") || os.contains("nux")) {
            rt.exec(new String[]{"xdg-open", url});
        } else {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        }
    }
}
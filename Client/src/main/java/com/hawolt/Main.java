package com.hawolt;

import com.hawolt.chromium.Jamalong;
import com.hawolt.chromium.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.io.Core;
import com.hawolt.io.JsonSource;
import com.hawolt.io.RunLevel;
import com.hawolt.logger.Logger;
import com.hawolt.source.impl.AbstractAudioSource;
import com.hawolt.source.impl.SoundcloudAudioSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {
    private static final String[] REQUIRED_VM_OPTIONS = new String[]{
            "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED",
            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports=java.base/java.lang=ALL-UNNAMED"
    };

    private static boolean validateExportOptions() {
        String version = System.getProperty("java.version");
        int major = Integer.parseInt(version.split("\\.")[0]);
        if (major <= 15) return true;
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String option : REQUIRED_VM_OPTIONS) {
            if (!arguments.contains(option)) return false;
        }
        return true;
    }

    private static ProcessBuilder getApplicationRestartCommand(List<String> arguments) throws Exception {
        String bin = String.join(File.separator, System.getProperty("java.home"), "bin", "java");
        File self = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!self.getName().endsWith(".jar")) {
            System.err.println("Please manually add the required VM options:");
            for (String requiredVmOption : REQUIRED_VM_OPTIONS) {
                System.err.println(requiredVmOption);
            }
            System.err.println("these options are required to properly run Chromium in OSR mode on Java 9 or higher");
            throw new Exception("Please add the required VM Options or downgrade your Java version");
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(bin);
        command.addAll(Arrays.asList(REQUIRED_VM_OPTIONS));
        command.add("-jar");
        command.add(self.getPath());
        command.addAll(arguments);
        for (int i = 0; i < command.size(); i++) {
            System.out.println(i + " " + command.get(i));
        }
        return new ProcessBuilder(command);
    }

    public static void main(String[] args) {
        try {
            JsonSource source = JsonSource.of(Core.read(RunLevel.get("project.json")).toString());
            Logger.info("Writing log for Jamalong-{}", source.getOrDefault("version", "UNKNOWN-VERSION"));
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch Jamalong, exiting (1).");
            System.exit(1);
        }
        List<String> arguments = Arrays.asList(args);
        boolean useOSR = arguments.contains("--osr");
        if (useOSR && !validateExportOptions()) {
            try {
                ProcessBuilder builder = getApplicationRestartCommand(arguments);
                Logger.info("Restarting with required VM Options");
                Process process = builder.start();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        } else {
            try {
                AbstractAudioSource source = new SoundcloudAudioSource();
                RemoteClient remoteClient = RemoteClient.createAndConnectClientInstance(source);
                PlaybackHandler playbackHandler = PlaybackHandler.start(remoteClient, source);
                Random random = new Random();
                int webserverPort = random.nextInt(30000) + 20000;
                int websocketPort = random.nextInt(30000) + 20000;
                Javalin.create(config -> config.addStaticFiles("/html", Location.CLASSPATH))
                        .before("/v1/*", context -> {
                            context.header("Access-Control-Allow-Origin", "*");
                        })
                        .routes(() -> LocalExecutor.configure(websocketPort, playbackHandler, source, remoteClient))
                        .start(webserverPort);
                SocketServer.launch(websocketPort);
                Jamalong.create(webserverPort, useOSR);
            } catch (IOException e) {
                Logger.error(e);
                System.err.println("Unable to launch Jamalong, exiting (1).");
                System.exit(1);
            }
        }
    }
}

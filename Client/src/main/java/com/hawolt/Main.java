package com.hawolt;

import com.hawolt.audio.SystemAudio;
import com.hawolt.chromium.Jamalong;
import com.hawolt.chromium.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.discord.RichPresence;
import com.hawolt.http.Request;
import com.hawolt.http.Response;
import com.hawolt.http.misc.DownloadCallback;
import com.hawolt.io.Core;
import com.hawolt.io.JsonSource;
import com.hawolt.io.RunLevel;
import com.hawolt.logger.Logger;
import com.hawolt.settings.ClientSettings;
import com.hawolt.settings.SettingManager;
import com.hawolt.source.impl.AbstractAudioSource;
import com.hawolt.source.impl.SoundcloudAudioSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final ExecutorService pool = Executors.newCachedThreadPool();
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

    private static final ExecutorService service = Executors.newSingleThreadExecutor();

    private static void singletonInstance() {
        try {
            ServerSocket socket = new ServerSocket(StaticConstant.SELF_PORT);
            service.execute(() -> {
                do {
                    try {
                        socket.accept();
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                } while (!Thread.currentThread().isInterrupted());
            });
        } catch (IOException e) {
            System.exit(1);
        }
    }

    public static Optional<RichPresence> presence;
    public static String version;

    public static void main(String[] args) {
        List<String> arguments = Arrays.asList(args);
        if (!arguments.contains("--allow-multiple-clients")) {
            Main.singletonInstance();
        }
        try {
            JsonSource source = JsonSource.of(Core.read(RunLevel.get(StaticConstant.PROJECT_DATA)).toString());
            Main.version = source.getOrDefault("version", "UNKNOWN-VERSION");
            Logger.info("Writing log for JamAlong-{}", version);
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch JamAlong, exiting (1).");
            System.exit(1);
        } catch (JSONException e) {
            Logger.error(e);
            System.err.println("Unable to check for newer release");
        }

        boolean useOSR = arguments.contains("--osr");
        if (useOSR && !

                validateExportOptions()) {
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
                SettingManager manager = new SettingManager();
                ClientSettings clientSettings = manager.getClientSettings();
                String username = clientSettings.getUsername();
                AbstractAudioSource source = new SoundcloudAudioSource();
                RemoteClient remoteClient = RemoteClient.createAndConnectClientInstance(username);
                PlaybackHandler playbackHandler = PlaybackHandler.start(remoteClient, source);
                SystemAudio.setGain(clientSettings.getClientVolumeGain());
                Random random = new Random();
                int webserverPort = random.nextInt(30000) + 20000;
                int websocketPort = random.nextInt(30000) + 20000;
                Javalin.create(config -> config.addStaticFiles("/html", Location.CLASSPATH))
                        .before("/v1/*", context -> {
                            context.header("Access-Control-Allow-Origin", "*");
                        })
                        .routes(() -> LocalExecutor.configure(manager, websocketPort, playbackHandler, source, remoteClient))
                        .start(webserverPort);
                SocketServer.launch(websocketPort);
                Jamalong.create(webserverPort, useOSR);
                Main.presence = RichPresence.create(remoteClient, playbackHandler);
                Main.presence.ifPresent(remoteClient::addInstructionListener);
            } catch (IOException e) {
                Logger.error(e);
                System.err.println("Unable to launch Jamalong, exiting (1).");
                System.exit(1);
            }
        }
    }
}

package com.hawolt;

import com.hawolt.audio.AudioManager;
import com.hawolt.chromium.Jamalong;
import com.hawolt.cryptography.AES256;
import com.hawolt.localhost.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.discord.RichPresence;
import com.hawolt.exceptions.AudioMixerUnavailableException;
import com.hawolt.io.Core;
import com.hawolt.io.JsonSource;
import com.hawolt.io.RunLevel;
import com.hawolt.logger.Logger;
import com.hawolt.misc.Debouncer;
import com.hawolt.misc.ExecutorManager;
import com.hawolt.remote.RemoteClient;
import com.hawolt.settings.ClientSettings;
import com.hawolt.settings.SettingManager;
import com.hawolt.media.impl.AbstractAudioSource;
import com.hawolt.media.impl.SoundcloudAudioSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.json.JSONException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        // initialize Application
        List<String> arguments = Arrays.asList(args);
        boolean useOSR = arguments.contains("--osr");
        boolean allowMultipleClients = arguments.contains("--allow-multiple-clients");
        ExecutorManager.registerService("pool", Executors.newCachedThreadPool());
        Application application = new Application();
        Main.bootstrap(application, arguments, useOSR, allowMultipleClients);
        try {
            // initialize Settings
            SettingManager settingManager = new SettingManager();
            ClientSettings clientSettings = settingManager.getClientSettings();
            String username = clientSettings.getUsername();
            application.setSettingManager(settingManager);
            // initialize Client for communication with Server
            RemoteClient remoteClient = RemoteClient.createAndConnectClientInstance(username);
            application.setRemoteClient(remoteClient);
            // initialize Core application features
            application.setAudioSource(new SoundcloudAudioSource());
            AudioManager audioManager = AudioManager.start(application);
            audioManager.getSystemAudio().setGain(clientSettings.getClientVolumeGain());
            application.setAudioManager(audioManager);
            // initialize communication methods
            Random random = new Random();
            int websocketPort = random.nextInt(30000) + 20000;
            application.setWebSocketPort(websocketPort);
            int webserverPort = random.nextInt(30000) + 20000;
            LocalExecutor localExecutor = new LocalExecutor(application);
            application.setLocalExecutor(localExecutor);
            // initialize local web server for communication from the frontend communication
            Javalin.create(config -> config.addStaticFiles("/html", Location.CLASSPATH))
                    .before("/v1/*", context -> {
                        context.header("Access-Control-Allow-Origin", "*");
                    })
                    .routes(localExecutor::configure)
                    .start(webserverPort);
            // initialize websocket server for communication towards the frontend
            SocketServer socketServer = SocketServer.launch(websocketPort);
            application.setSocketServer(socketServer);
            // initialize UI
            if (Main.failed) Thread.sleep(3000L);
            Jamalong.create(webserverPort, useOSR);
            // initialize RPC
            Optional<RichPresence> richPresence = RichPresence.create(application);
            application.setRichPresence(richPresence);
            application.getRichPresence().ifPresent(presence -> {
                application.getRemoteClient().addInstructionListener(presence);
            });
        } catch (IOException | AudioMixerUnavailableException | InterruptedException e) {
            Logger.error(e);
            System.err.println("Unable to launch Jamalong, exiting (1).");
            System.exit(1);
        }

    }

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

    private static void bootstrap(Application application, List<String> arguments, boolean useOSR, boolean allowMultipleClients) {
        if (!allowMultipleClients) {
            singletonInstance(application);
        }
        try {
            JsonSource source = JsonSource.of(Core.read(RunLevel.get(StaticConstant.PROJECT_DATA)).toString());
            String version = source.getOrDefault("version", "UNKNOWN-VERSION");
            application.setVersion(version);
            Logger.info("Writing log for JamAlong-{}", version);
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch JamAlong, exiting (1).");
            System.exit(1);
        } catch (JSONException e) {
            Logger.error(e);
            System.err.println("Unable to check for newer release");
        }
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
                System.exit(1);
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    private static boolean failed;

    private static void singletonInstance(Application application) {
        ExecutorService service = ExecutorManager.getService("pool");
        service.execute(() -> {
            int attempts = 0;
            do {
                Logger.debug("attempt {}", attempts);
                if (application.isGraceful()) break;
                try {
                    ServerSocket socket = new ServerSocket(StaticConstant.SELF_PORT);
                    application.setServerSocket(socket);
                    socket.accept();
                } catch (IOException e) {
                    if (!application.isGraceful()) Logger.error(e);
                }
                Main.failed = true;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Logger.error(e);
                }
            } while (++attempts < 3);
            Logger.info("SingletonInstance down");
            if (!application.isGraceful()) System.exit(1);
        });
    }
}

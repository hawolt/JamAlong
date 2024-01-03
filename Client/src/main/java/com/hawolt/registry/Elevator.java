package com.hawolt.registry;

import com.hawolt.Main;
import com.hawolt.logger.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Elevator {
    public static final String ELEVATOR =
            "powershell.exe -Command \"Start-Process cmd \\\"/c java -jar \\\"\\\"\"%s\\\"\\\"\" elevate\\\" -Verb RunAs\"";

    public static void addRegistryKeyOrElevate(String protocol, boolean elevated) throws Exception {
        File self = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        RegisterCustomProtocol.checkup("C:\\Users\\Niklas\\Nextcloud\\IntelliJ IDEA\\Github\\soundcloud-together\\Client\\target\\JamAlong-1.11.jar", protocol);
        if (!self.getName().endsWith(".jar")) {
            throw new Exception("Unable to restart automatically missing Administrator privileges");
        }
        if (elevated) {
            ProcessBuilder builder = new ProcessBuilder("java", "-jar", self.getPath());
            builder.start();
            Logger.debug("Exiting current Admin Application and restarting to kill cmd prompt");
            System.exit(0);
        }
        try {
            if (RegisterCustomProtocol.checkup(self.getPath(), protocol)) return;
            RegisterCustomProtocol.register(protocol);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            Logger.error(e);
        } catch (RegistryPermissionException e) {
            int selection = JOptionPane.showOptionDialog(
                    null,
                    "<html>Administrator privilege required for full functionality but not needed.<br>Selecting Yes will prompt you to grant permission.<html>",
                    "Elevation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    null,
                    null
            );
            if (selection != 0) return;
            if (!elevated) {
                ProcessExecutor.execute(String.format(ELEVATOR, self.getPath()), false, false);
                Logger.debug("Exiting current Application for Elevated one");
                System.exit(0);
            }
        }
    }
}

package com.hawolt.chromium;

import com.hawolt.io.RunLevel;
import com.hawolt.logger.Logger;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Jamalong {
    public static JFrame frame;

    public static void create(int port, boolean useOSR) throws IOException {
        JFrame frame = new JFrame();
        String icon = "Jamalong.png";
        frame.setIconImage(ImageIO.read(RunLevel.get(icon)));
        frame.setTitle("Jamalong");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (CefApp.getState() != CefApp.CefAppState.NONE) CefApp.getInstance().dispose();
                frame.dispose();
                System.exit(0);
            }
        });
        Container container = frame.getContentPane();
        container.setPreferredSize(new Dimension(350, 100));
        container.setLayout(new BorderLayout());
        VisualProgressHandler handler = new VisualProgressHandler();
        container.add(handler, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Path path = Paths.get(System.getProperty("java.io.tmpdir")).resolve("jcef-bundle");
        try {
            Logger.debug("{}", "http://127.0.0.1:" + port);
            Chromium chromium = new Chromium("http://127.0.0.1:" + port, path, useOSR, handler);
            frame.dispose();
            container.removeAll();
            container.setBackground(new Color(224, 224, 224));
            container.setPreferredSize(new Dimension(420, 180));
            container.add(chromium.getBrowserUI(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
            Logger.error(e);
        }
        Jamalong.frame = frame;
    }
}

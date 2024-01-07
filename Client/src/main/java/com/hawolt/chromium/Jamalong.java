package com.hawolt.chromium;

import com.hawolt.io.RunLevel;
import com.hawolt.logger.Logger;
import io.javalin.http.Handler;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Graphical main component for JamAlong.
 */
public class Jamalong {
    private static final Color base = new Color(76, 74, 72);
    private static Point initialClick;

    public static JFrame frame;

    public static void create(int port, boolean useOSR) throws IOException {
        JFrame frame = new JFrame();
        frame.setIconImage(ImageIO.read(RunLevel.get("html/assets/Jamalong.png")));
        frame.setTitle("JamAlong");
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
            frame.setUndecorated(true);
            container.removeAll();
            container.setBackground(new Color(224, 224, 224));
            container.setPreferredSize(new Dimension(550, 400));
            JComponent component = (JComponent) container;
            component.setBackground(new Color(76, 74, 72));
            component.setBorder(new EmptyBorder(1, 1, 1, 1));
            JPanel move = getHeader(frame);
            addWindowStyle(move, ImageIO.read(RunLevel.get("html/assets/Jamalong26.png")));
            addWindowInteraction(move);
            move.setBackground(base);
            container.add(move, BorderLayout.NORTH);
            container.add(chromium.getBrowserUI(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
            Logger.error(e);
        }
        Jamalong.frame = frame;
    }

    private static void addWindowStyle(JPanel move, BufferedImage image) {
        LogoComponent logo = new LogoComponent(image);
        JPanel style = new JPanel(new BorderLayout(5, 0));
        style.setBackground(new Color(76, 74, 72));
        style.add(logo, BorderLayout.WEST);
        JLabel label = new JLabel("JamAlong");
        label.setFont(new Font("Dialog", Font.BOLD, 18));
        label.setForeground(new Color(252, 247, 255));
        style.add(label, BorderLayout.CENTER);
        move.add(style, BorderLayout.WEST);
    }

    private static void addWindowInteraction(JPanel move) {
        JPanel main = new JPanel(new GridLayout(0, 2));
        JamalongButton button1 = new JamalongButton(base, new Color(87, 85, 83));
        button1.addActionListener(listener -> Jamalong.frame.setState(JFrame.ICONIFIED));
        button1.setText("—");
        main.add(button1);
        JamalongButton button2 = new JamalongButton(base, new Color(196, 43, 28));
        button2.addActionListener(listener -> System.exit(1));
        button2.setText("✖");
        main.add(button2);
        move.add(main, BorderLayout.EAST);
    }

    @NotNull
    private static JPanel getHeader(Frame source) {
        JPanel move = new JPanel();
        move.setLayout(new BorderLayout());
        move.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                Jamalong.initialClick = e.getPoint();
            }
        });
        move.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                final int thisX = source.getLocation().x;
                final int thisY = source.getLocation().y;
                final int xMoved = e.getX() - Jamalong.initialClick.x;
                final int yMoved = e.getY() - Jamalong.initialClick.y;
                final int X = thisX + xMoved;
                final int Y = thisY + yMoved;
                source.setLocation(X, Y);
            }
        });
        move.setBackground(new Color(224, 224, 224));
        move.setPreferredSize(new Dimension(0, 34));
        return move;
    }
}

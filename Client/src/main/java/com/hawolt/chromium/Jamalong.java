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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Jamalong {
    private static Point initialClick;
    private static Rectangle previous;
    private static boolean toggle;
    public static final Handler MINIMIZE = context -> {
        Jamalong.frame.setState(JFrame.ICONIFIED);
    };
    public static final Handler MAXIMIZE = context -> {
        Jamalong.toggle = !Jamalong.toggle;
        if (Jamalong.toggle) {
            Jamalong.previous = Jamalong.frame.getBounds();
            DisplayMode mode = Jamalong.frame.getGraphicsConfiguration().getDevice().getDisplayMode();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(Jamalong.frame.getGraphicsConfiguration());
            Jamalong.frame.setMaximizedBounds(new Rectangle(
                    mode.getWidth() - insets.right - insets.left,
                    mode.getHeight() - insets.top - insets.bottom
            ));
            Jamalong.frame.setExtendedState(Jamalong.frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else {
            Jamalong.frame.setBounds(Jamalong.previous);
        }
    };
    public static JFrame frame;

    public static void create(boolean useOSR) throws IOException {
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
            Chromium chromium = new Chromium("http://127.0.0.1:35199/home.html", path, useOSR, handler);
            frame.dispose();
            frame.setUndecorated(true);
            container.removeAll();
            container.setBackground(new Color(224, 224, 224));
            container.setPreferredSize(new Dimension(1010, 620));
            JComponent component = (JComponent) container;
            component.setBorder(new EmptyBorder(0, 5, 5, 5));
            JPanel move = getHeader(frame);
            container.add(move, BorderLayout.NORTH);
            container.add(chromium.getBrowserUI(), BorderLayout.CENTER);
            ComponentResizer resizer = new ComponentResizer();
            resizer.registerComponent(frame);
            resizer.setSnapSize(new Dimension(10, 10));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
            Logger.error(e);
        }
        Jamalong.frame = frame;
    }

    @NotNull
    private static JPanel getHeader(Frame source) {
        JPanel move = new JPanel();
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
        move.setPreferredSize(new Dimension(0, 6));
        return move;
    }

    private static final int[] CURSOR_MAPPING = new int[]{
            Cursor.NW_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR,
            Cursor.NE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR,
            Cursor.NW_RESIZE_CURSOR, 0, 0, 0, Cursor.NE_RESIZE_CURSOR,
            Cursor.W_RESIZE_CURSOR, 0, 0, 0, Cursor.E_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR, 0, 0, 0, Cursor.SE_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR,
            Cursor.SE_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR
    };

    private static final int BORDER_DRAG_THICKNESS = 5;
    private static final int CORNER_DRAG_WIDTH = 16;
    private static boolean isHolding, move, resize;
    private static Rectangle bounds;
    private static Point drag = new Point(0, 0);

    private static int getCorner(int x, int y) {
        Insets insets = frame.getInsets();
        int dx = getPosition(x - insets.left, frame.getWidth() - insets.left - insets.right);
        int dy = getPosition(y - insets.top, frame.getHeight() - insets.top - insets.bottom);
        return dx != -1 && dy != -1 ? dy * 5 + dx : -1;
    }

    private static int getPosition(int spot, int width) {
        if (spot < BORDER_DRAG_THICKNESS) {
            return 0;
        } else if (spot < CORNER_DRAG_WIDTH) {
            return 1;
        } else if (spot >= (width - BORDER_DRAG_THICKNESS)) {
            return 4;
        } else if (spot >= (width - CORNER_DRAG_WIDTH)) {
            return 3;
        }
        return 2;
    }

    private static int getCursor(int corner) {
        return corner != -1 ? CURSOR_MAPPING[corner] : 0;
    }
}

package com.hawolt.chromium;

import javax.swing.*;
import java.awt.*;

public class SmoothLabel extends JLabel {

    public SmoothLabel(String text) {
        super(text);
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintComponent(g2d);
    }
}
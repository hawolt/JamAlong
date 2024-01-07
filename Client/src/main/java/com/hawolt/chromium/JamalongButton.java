package com.hawolt.chromium;

import javax.swing.*;
import java.awt.*;

public class JamalongButton extends JButton {

    private final Color base, hover;

    public JamalongButton(Color base, Color hover) {
        this.setPreferredSize(new Dimension(45, 28));
        this.setContentAreaFilled(false);
        this.setForeground(Color.WHITE);
        this.setFocusPainted(false);
        this.setBackground(base);
        this.setBorder(null);
        this.hover = hover;
        this.base = base;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isPressed()) {
            g.setColor(hover.brighter());
        } else if (getModel().isRollover()) {
            g.setColor(hover);
        } else {
            g.setColor(base);
        }
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}

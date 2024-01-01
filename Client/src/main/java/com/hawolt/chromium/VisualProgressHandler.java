package com.hawolt.chromium;

import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;

import java.awt.*;

public class VisualProgressHandler extends Graphics2DComponent implements IProgressHandler {

    private final Color _BACKGROUND = new Color(54, 51, 51);
    private final Color _NOT_LOADED = new Color(116, 30, 30);
    private final Color _LOADED = new Color(66, 147, 69);

    private final Font _FONT = new Font("Arial", Font.PLAIN, 18);
    private final int _HEIGHT = 32, _WIDTH = 300;

    private EnumProgress e = EnumProgress.INITIALIZING;
    private float f;

    @Override
    protected void paint(Graphics2D g) {
        Dimension dimension = getSize();
        g.setColor(_BACKGROUND);
        g.fillRect(0, 0, dimension.width, dimension.height);
        int offsetX = (dimension.width >> 1) - (_WIDTH >> 1);
        int offsetY = (dimension.height >> 1) - (_HEIGHT >> 1);
        g.setColor(Color.BLACK);
        g.drawRect(offsetX, offsetY, _WIDTH, _HEIGHT);
        g.setColor(_NOT_LOADED);
        g.fillRect(offsetX + 1, offsetY + 1, _WIDTH - 1, _HEIGHT - 1);
        g.setColor(_LOADED);
        if (e == EnumProgress.DOWNLOADING) {
            g.fillRect(offsetX + 1, offsetY + 1, (int) (f * 3), _HEIGHT - 1);
        } else {
            g.fillRect(offsetX + 1, offsetY + 1, _WIDTH - 1, _HEIGHT - 1);
        }
        g.setColor(Color.BLACK);
        g.setFont(_FONT);
        FontMetrics metrics = g.getFontMetrics();
        String text = e == EnumProgress.DOWNLOADING ? String.format("%s - %s%%", e.name(), f) : e.name();
        if (e == EnumProgress.DOWNLOADING && f == -1) return;
        int statusWidth = metrics.stringWidth(text);
        g.drawString(text, offsetX + 1 + ((_WIDTH >> 1) - (statusWidth >> 1)), offsetY + (_HEIGHT >> 1) + (metrics.getAscent() >> 1) - 1);
    }

    @Override
    public void handleProgress(EnumProgress e, float f) {
        this.e = e;
        this.f = f;
        this.repaint();
    }

}

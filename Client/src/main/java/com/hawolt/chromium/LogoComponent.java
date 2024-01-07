package com.hawolt.chromium;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LogoComponent extends Graphics2DComponent {
    private final BufferedImage image;

    public LogoComponent(BufferedImage image) {
        this.setPreferredSize(new Dimension(30, 34));
        this.image = image;
    }

    @Override
    protected void paint(Graphics2D g) {
        Dimension dimension = getSize();
        int x = (dimension.width >> 1) - (image.getWidth() >> 1);
        int y = (dimension.height >> 1) - (image.getHeight() >> 1);
        g.drawImage(image, x, y, null);
    }
}

package com.nhom5.netmonitor.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Rounded dark panel used by the screen mockups. */
class SurfacePanel extends JPanel {
    SurfacePanel(int x, int y, int width, int height) {
        setLayout(null);
        setBounds(x, y, width, height);
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiTheme.CARD);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g.setColor(UiTheme.EDGE);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g.dispose();
        super.paintComponent(graphics);
    }
}

package com.nhom5.netmonitor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** Small vector icons matched to the Figma sidebar navigation. */
final class SidebarIcon extends JComponent {
    enum Kind { DASHBOARD, CONNECTIONS, STATISTICS, SOCKET }

    private final Kind kind;
    private final Color color;

    SidebarIcon(Kind kind, Color color, int x, int y) {
        this.kind = kind;
        this.color = color;
        setBounds(x, y, 24, 24);
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case DASHBOARD -> drawDashboard(g);
            case CONNECTIONS -> drawConnections(g);
            case STATISTICS -> drawStatistics(g);
            case SOCKET -> drawSocket(g);
        }
        g.dispose();
    }

    private static void drawDashboard(Graphics2D g) {
        g.drawRoundRect(3, 3, 7, 7, 2, 2);
        g.drawRoundRect(14, 3, 7, 7, 2, 2);
        g.drawRoundRect(3, 14, 7, 7, 2, 2);
        g.drawRoundRect(14, 14, 7, 7, 2, 2);
    }

    private static void drawConnections(Graphics2D g) {
        g.drawLine(6, 17, 12, 7); g.drawLine(12, 7, 19, 17); g.drawLine(6, 17, 19, 17);
        g.drawOval(3, 14, 6, 6); g.drawOval(9, 3, 6, 6); g.drawOval(16, 14, 6, 6);
    }

    private static void drawStatistics(Graphics2D g) {
        g.drawLine(3, 21, 21, 21);
        g.drawRoundRect(5, 13, 3, 8, 1, 1);
        g.drawRoundRect(11, 8, 3, 13, 1, 1);
        g.drawRoundRect(17, 4, 3, 17, 1, 1);
    }

    private static void drawSocket(Graphics2D g) {
        g.drawLine(4, 8, 19, 8); g.drawLine(15, 4, 19, 8); g.drawLine(15, 12, 19, 8);
        g.drawLine(20, 16, 5, 16); g.drawLine(9, 12, 5, 16); g.drawLine(9, 20, 5, 16);
    }
}

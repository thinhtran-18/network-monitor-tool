package com.nhom5.netmonitor.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

/** Shared colors and small helpers for the NetMonitor Swing views. */
final class UiTheme {
    static final Color BG = new Color(11, 18, 32);
    static final Color SIDEBAR = new Color(17, 29, 48);
    static final Color CARD = new Color(24, 38, 61);
    static final Color EDGE = new Color(35, 51, 75);
    static final Color INPUT = new Color(11, 20, 36);
    static final Color WHITE = new Color(232, 238, 248);
    static final Color MUTED = new Color(142, 158, 181);
    static final Color BLUE = new Color(37, 99, 235);
    static final Color TEAL = new Color(45, 212, 191);
    static final Color UPLOAD = new Color(96, 165, 250);
    static final Color GREEN = new Color(34, 197, 94);
    static final Color AMBER = new Color(245, 158, 11);

    private UiTheme() { }

    static JLabel label(String text, int x, int y, int w, int h, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, w, h);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }
}

package com.nhom5.netmonitor.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Shared frame chrome used by the Connections, Statistics, and Socket Test pages. */
abstract class MonitorFrame extends JFrame {
    private final JPanel root = new JPanel(null);
    private final int width;
    private final int height;

    MonitorFrame(String pageTitle, int activePage, int width, int height) {
        super("NetMonitor - " + pageTitle);
        this.width = width;
        this.height = height;
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setResizable(false);
        setLocationRelativeTo(null);
        root.setBackground(UiTheme.BG);
        root.add(new Sidebar(activePage, height));
        root.add(new Header(pageTitle, width));
        setContentPane(root);
    }

    protected final void addPageComponent(java.awt.Component component) { root.add(component); }

    private static final class Sidebar extends JPanel {
        Sidebar(int activePage, int height) {
            setLayout(null);
            setBounds(0, 0, 240, height);
            setBackground(UiTheme.SIDEBAR);
            setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.EDGE));
            add(UiTheme.label("NetMonitor", 58, 20, 160, 32, 18, UiTheme.WHITE, true));
            String[] pages = {"Dashboard", "Connections", "Statistics", "Socket Test"};
            SidebarIcon.Kind[] icons = {SidebarIcon.Kind.DASHBOARD, SidebarIcon.Kind.CONNECTIONS,
                SidebarIcon.Kind.STATISTICS, SidebarIcon.Kind.SOCKET};
            for (int i = 0; i < pages.length; i++) {
                int index = i;
                JPanel row = new JPanel(null);
                row.setBounds(12, 80 + i * 48, 216, 44);
                row.setBackground(i == activePage ? new Color(31, 48, 75) : UiTheme.SIDEBAR);
                if (i == activePage) {
                    JPanel marker = new JPanel(); marker.setBackground(UiTheme.BLUE); marker.setBounds(0, 8, 3, 28); row.add(marker);
                }
                Color iconColor = i == activePage ? UiTheme.WHITE : UiTheme.MUTED;
                JLabel text = UiTheme.label(pages[i], 52, 0, 150, 44, 15,
                        iconColor, i == activePage);
                SidebarIcon icon = new SidebarIcon(icons[i], iconColor, 16, 10);
                row.add(icon);
                row.add(text);
                row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                MouseAdapter navigation = new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { navigate(index); }
                };
                row.addMouseListener(navigation); text.addMouseListener(navigation); icon.addMouseListener(navigation);
                add(row);
            }
            add(UiTheme.label("v1.0.0", 20, height - 46, 150, 24, 12, UiTheme.MUTED, false));
        }

        private void navigate(int index) {
            JFrame next = switch (index) {
                case 0 -> new DashboardFrame();
                case 1 -> new ConnectionsFrame();
                case 2 -> new StatisticsFrame();
                default -> new SocketTestFrame();
            };
            next.setVisible(true);
            SwingUtilities.getWindowAncestor(this).dispose();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UiTheme.BLUE); g.fillRoundRect(20, 20, 28, 28, 7, 7);
            g.setColor(Color.WHITE); g.setStroke(new java.awt.BasicStroke(2f));
            g.drawPolyline(new int[]{25, 30, 33, 37, 41}, new int[]{34, 34, 27, 39, 32}, 5);
            g.setColor(UiTheme.EDGE); g.drawLine(0, getHeight() - 60, getWidth(), getHeight() - 60);
            g.dispose();
        }
    }

    private static final class Header extends JPanel {
        Header(String title, int frameWidth) {
            int headerWidth = frameWidth - 240;
            setLayout(null); setBounds(240, 0, headerWidth, 64); setBackground(UiTheme.SIDEBAR);
            setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.EDGE));
            add(UiTheme.label(title, 24, 5, 430, 30, 22, UiTheme.WHITE, true));
            add(UiTheme.label("Network Monitor Tool", 24, 34, 300, 20, 13, UiTheme.MUTED, false));
            JPanel badge = new JPanel(null); badge.setBounds(headerWidth - 340, 17, 128, 30); badge.setBackground(new Color(18, 54, 52));
            badge.add(UiTheme.label("●  Đang giám sát", 11, 0, 112, 30, 12, UiTheme.GREEN, true)); add(badge);
            JLabel clock = UiTheme.label("Cập nhật: " + time(), headerWidth - 188, 0, 140, 64, 12, UiTheme.MUTED, false);
            add(clock);
            Timer timer = new Timer(1000, e -> clock.setText("Cập nhật: " + time())); timer.start();
            JButton close = new JButton("×"); close.setBounds(headerWidth - 40, 13, 28, 28);
            close.setForeground(UiTheme.MUTED); close.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            close.setBorderPainted(false); close.setContentAreaFilled(false); close.setFocusPainted(false);
            close.addActionListener(e -> System.exit(0)); add(close);
        }
        private static String time() { return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); }
    }
}

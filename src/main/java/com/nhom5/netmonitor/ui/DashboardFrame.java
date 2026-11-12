package com.nhom5.netmonitor.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Dashboard demo matching the 1440 x 900 Figma frame. */
public final class DashboardFrame extends JFrame {
    private static final Color BG = new Color(11, 18, 32);
    private static final Color SIDEBAR = new Color(17, 29, 48);
    private static final Color CARD = new Color(24, 38, 61);
    private static final Color EDGE = new Color(35, 51, 75);
    private static final Color WHITE = new Color(232, 238, 248);
    private static final Color MUTED = new Color(142, 158, 181);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color TEAL = new Color(45, 212, 191);
    private static final Color UPLOAD = new Color(96, 165, 250);

    public DashboardFrame() {
        super("NetMonitor Dashboard");
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(null);
        root.setBackground(BG);
        root.add(new Sidebar());
        root.add(new Header());
        root.add(new MetricCard("Download Speed", "12.48", "MB/s", "Cao nhất: 18.92 MB/s", TEAL, "↓"));
        root.add(new MetricCard("Upload Speed", "3.27", "MB/s", "Cao nhất: 6.15 MB/s", UPLOAD, "↑"));
        root.add(new TotalsCard());
        root.add(new UsageCard());
        root.add(new TrafficChart());
        root.add(new NetworkCard());
        setContentPane(root);
    }

    private static final class Sidebar extends JPanel {
        Sidebar() {
            setLayout(null);
            setBounds(0, 0, 240, 900);
            setBackground(SIDEBAR);
            setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, EDGE));
            add(label("NetMonitor", 58, 22, 160, 28, 18, WHITE, true));
            String[] names = {"Dashboard", "Connections", "Statistics", "Socket Test"};
            SidebarIcon.Kind[] icons = {SidebarIcon.Kind.DASHBOARD, SidebarIcon.Kind.CONNECTIONS,
                SidebarIcon.Kind.STATISTICS, SidebarIcon.Kind.SOCKET};
            for (int i = 0; i < names.length; i++) {
                int pageIndex = i;
                int y = 80 + i * 48;
                JPanel row = new JPanel(null);
                row.setBounds(12, y, 216, 44);
                row.setBackground(i == 0 ? new Color(31, 48, 75) : SIDEBAR);
                row.setOpaque(true);
                if (i == 0) {
                    JPanel marker = new JPanel(); marker.setBackground(BLUE); marker.setBounds(0, 8, 3, 28); row.add(marker);
                }
                Color iconColor = i == 0 ? WHITE : MUTED;
                SidebarIcon icon = new SidebarIcon(icons[i], iconColor, 16, 10);
                javax.swing.JLabel item = label(names[i], 52, 0, 150, 44, 15, iconColor, i == 0);
                item.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                MouseAdapter navigation = new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { navigateTo(pageIndex); }
                };
                item.addMouseListener(navigation);
                row.addMouseListener(navigation);
                row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                row.add(icon);
                row.add(item);
                add(row);
            }
            add(label("v1.0.0", 20, 854, 180, 24, 12, MUTED, false));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(EDGE); g.drawLine(0, 840, getWidth(), 840);
            g.setColor(new Color(37, 99, 235)); g.fillRoundRect(20, 20, 28, 28, 7, 7);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(WHITE); g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawPolyline(new int[]{25, 30, 33, 37, 41}, new int[]{34, 34, 27, 39, 32}, 5);
        }

        private void navigateTo(int pageIndex) {
            JFrame next = switch (pageIndex) {
                case 0 -> new DashboardFrame();
                case 1 -> new ConnectionsFrame();
                case 2 -> new StatisticsFrame();
                default -> new SocketTestFrame();
            };
            next.setVisible(true);
            SwingUtilities.getWindowAncestor(this).dispose();
        }
    }

    private static final class Header extends JPanel {
        private final javax.swing.JLabel clock = label("Cập nhật: 14:32:08", 1012, 0, 140, 64, 12, MUTED, false);
        Header() {
            setLayout(null); setBounds(240, 0, 1200, 64); setBackground(SIDEBAR);
            setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, EDGE));
            add(label("Dashboard", 24, 6, 280, 29, 22, WHITE, true));
            add(label("Network Monitor Tool", 24, 34, 280, 22, 13, MUTED, false));
            JPanel status = new JPanel(null); status.setBounds(860, 18, 128, 30); status.setBackground(new Color(18, 54, 52));
            status.add(label("●  Đang giám sát", 10, 0, 112, 30, 12, new Color(34, 197, 94), true)); add(status);
            add(clock);
            Timer timer = new Timer(1000, e -> clock.setText("Cập nhật: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
            timer.start();
            JPanel close = new JPanel(null); close.setBounds(1160, 0, 28, 64); close.setOpaque(false);
            close.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { System.exit(0); } });
            close.add(label("×", 5, 15, 25, 30, 20, MUTED, false)); add(close);
        }
    }

    private static class Card extends JPanel {
        Card(int x, int y, int w, int h) { setLayout(null); setBounds(x, y, w, h); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD); g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            g2.setColor(EDGE); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10); g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class MetricCard extends Card {
        MetricCard(String title, String value, String unit, String foot, Color accent, String icon) {
            super(264 + (title.startsWith("Upload") ? 292 : 0), 88, 276, 120);
            add(label(title, 20, 20, 210, 22, 14, MUTED, false));
            add(label(value, 20, 49, 140, 42, 31, accent, true));
            add(label(unit, 106, 62, 90, 25, 13, MUTED, false));
            add(label(foot, 20, 88, 235, 20, 13, MUTED, false));
            JPanel badge = new JPanel(null); badge.setBounds(230, 20, 26, 26); badge.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
            badge.add(label(icon, 5, 0, 20, 25, 18, accent, true)); add(badge);
        }
    }

    private static final class TotalsCard extends Card {
        TotalsCard() {
            super(848, 88, 276, 120); add(label("Tổng dữ liệu", 20, 20, 220, 22, 14, MUTED, false));
            add(label("●  Đã nhận", 20, 52, 115, 23, 14, MUTED, false)); add(label("8.64  GB", 156, 52, 100, 23, 15, WHITE, true));
            add(label("●  Đã gửi", 20, 82, 115, 23, 14, MUTED, false)); add(label("2.31  GB", 156, 82, 100, 23, 15, WHITE, true));
            add(label("●", 20, 51, 16, 20, 12, TEAL, true)); add(label("●", 20, 81, 16, 20, 12, UPLOAD, true));
        }
    }

    private static final class UsageCard extends Card {
        UsageCard() {
            super(1140, 88, 276, 120); add(label("Băng thông sử dụng", 20, 20, 235, 22, 14, MUTED, false));
            add(label("42%", 20, 51, 110, 40, 30, WHITE, true)); add(label("của 100 Mbps", 120, 63, 130, 24, 13, MUTED, false));
        }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setColor(EDGE); g2.fillRoundRect(20,94,236,8,8,8); g2.setColor(BLUE); g2.fillRoundRect(20,94,99,8,8,8); g2.dispose(); }
    }

    private static final class TrafficChart extends Card {
        private final int[] down = {54,42,48,39,51,34,41,38,28,35,46,43,50,40,55,49,37,44,32,40,52,47,60,53,45,50,41,46,36,42,55,48,51,44,39,47,53,42,50,46};
        private final int[] up = {15,18,13,16,12,14,10,13,11,17,14,12,16,18,15,12,14,11,16,13,17,15,12,14,16,13,11,15,18,14,12,16,13,15,17,12,14,11,15,13};
        TrafficChart() {
            super(264, 224, 752, 652);
            add(label("Lưu lượng mạng theo thời gian", 20, 20, 360, 28, 17, WHITE, true));
            add(label("━  Download     ━  Upload     │   60 giây gần nhất", 430, 20, 300, 26, 12, MUTED, false));
            add(label("MB/s", 20, 61, 100, 22, 13, MUTED, false));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int left=64, top=100, right=getWidth()-28, bottom=getHeight()-48;
            g2.setFont(new Font("Segoe UI",Font.PLAIN,12));
            for(int i=0;i<=4;i++){ int y=top+(bottom-top)*i/4; g2.setColor(EDGE);g2.drawLine(left,y,right,y);g2.setColor(MUTED);g2.drawString(Integer.toString(20-i*5),18,y+4); }
            int[] xs=new int[down.length], yd=new int[down.length], yu=new int[up.length];
            for(int i=0;i<down.length;i++){xs[i]=left+(right-left)*i/(down.length-1);yd[i]=bottom-(bottom-top)*down[i]/80;yu[i]=bottom-(bottom-top)*up[i]/80;}
            g2.setColor(new Color(45,212,191,24));int[] px=new int[xs.length+2],py=new int[xs.length+2];px[0]=left;py[0]=bottom;System.arraycopy(xs,0,px,1,xs.length);System.arraycopy(yd,0,py,1,yd.length);px[px.length-1]=right;py[py.length-1]=bottom;g2.fillPolygon(px,py,px.length);
            g2.setStroke(new BasicStroke(2.2f));g2.setColor(TEAL);g2.drawPolyline(xs,yd,xs.length);g2.setColor(UPLOAD);g2.drawPolyline(xs,yu,xs.length);
            g2.setColor(MUTED);String[] ticks={"14:31:10","14:31:20","14:31:30","14:31:40","14:31:50","14:32:00","14:32:08"};
            for(int i=0;i<ticks.length;i++){int x=left+(right-left)*i/(ticks.length-1);g2.drawString(ticks[i],x-24,bottom+24);}g2.dispose();
        }
    }

    private static final class NetworkCard extends Card {
        NetworkCard() {
            super(1032,224,384,652);add(label("Card mạng đang theo dõi",20,20,340,28,17,WHITE,true));
            JPanel select=new JPanel(null);select.setBounds(20,60,344,40);select.setBackground(new Color(17,29,48));select.setBorder(javax.swing.BorderFactory.createLineBorder(EDGE));select.add(label("Wi-Fi (Intel Wi-Fi 6 AX201)     ⌄",12,0,320,40,13,WHITE,false));add(select);
            String[][] rows={{"Trạng thái","● Hoạt động"},{"Địa chỉ IPv4","192.168.1.25"},{"Địa chỉ MAC","A4:C3:F0:5B:2E:91"},{"Tốc độ liên kết","100 Mbps"},{"Gói tin đã gửi","1,482,903"},{"Gói tin đã nhận","3,917,466"},{"Lỗi gửi/nhận","0 / 2"}};
            for(int i=0;i<rows.length;i++){int y=118+i*40;add(label(rows[i][0],20,y,150,36,13,MUTED,false));add(label(rows[i][1],160,y,204,36,13,i==0?new Color(34,197,94):WHITE,true));}
        }
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setColor(EDGE);for(int y=155;y<440;y+=40)g2.drawLine(20,y,getWidth()-20,y);g2.dispose();}
    }

    private static javax.swing.JLabel label(String text,int x,int y,int w,int h,int size,Color color,boolean bold){
        javax.swing.JLabel l=new javax.swing.JLabel(text);l.setBounds(x,y,w,h);l.setForeground(color);l.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size));return l;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }
}

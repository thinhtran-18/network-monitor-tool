package com.nhom5.netmonitor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Statistics page mockup with sample charts and totals. */
public final class StatisticsFrame extends MonitorFrame {
    public StatisticsFrame() {
        super("Statistics", 2, 1440, 900);
        addTimeControls();
        addMetricCards();
        addPageComponent(new RateChart());
        addPageComponent(new TrafficBreakdown());
    }

    private void addTimeControls() {
        JPanel group = new JPanel(null); group.setBounds(372, 94, 446, 38); group.setBackground(UiTheme.CARD);
        String[] ranges = {"15 phút", "1 giờ", "6 giờ", "24 giờ", "7 ngày"};
        int[] widths = {94, 92, 92, 92, 76}; int x = 0;
        for (int i = 0; i < ranges.length; i++) {
            JButton b = new JButton(ranges[i]); b.setBounds(x, 0, widths[i], 38); x += widths[i];
            b.setForeground(i == 1 ? Color.WHITE : UiTheme.MUTED); b.setBackground(i == 1 ? UiTheme.BLUE : UiTheme.CARD);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13)); b.setFocusPainted(false);
            b.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE)); group.add(b);
        }
        addPageComponent(UiTheme.label("Khoảng thời gian", 264, 94, 104, 38, 12, UiTheme.MUTED, false));
        addPageComponent(group);
        JButton export = new JButton("Xuất báo cáo"); export.setBounds(1240, 94, 176, 38);
        export.setBackground(UiTheme.BLUE); export.setForeground(Color.WHITE); export.setFocusPainted(false);
        export.setFont(new Font("Segoe UI", Font.BOLD, 13)); export.setBorder(BorderFactory.createEmptyBorder());
        addPageComponent(export);
    }

    private void addMetricCards() {
        Metric[] metrics = {
            new Metric("Tổng đã nhận", "8.64", "GB", "trong 1 giờ qua", UiTheme.TEAL),
            new Metric("Tổng đã gửi", "2.31", "GB", "trong 1 giờ qua", UiTheme.UPLOAD),
            new Metric("Tốc độ trung bình", "Download   9.84 MB/s", "Upload      2.76 MB/s", "", UiTheme.TEAL),
            new Metric("Tốc độ cao nhất", "Download  18.92 MB/s", "Upload       6.15 MB/s", "lúc 14:12:35", UiTheme.TEAL)
        };
        int[] xs = {264, 556, 848, 1140};
        for (int i = 0; i < metrics.length; i++) {
            SurfacePanel card = new SurfacePanel(xs[i], 152, 276, 112);
            Metric m = metrics[i]; card.add(UiTheme.label(m.title, 20, 14, 235, 22, 13, UiTheme.MUTED, false));
            if (i < 2) {
                card.add(UiTheme.label(m.line1, 20, 42, 130, 40, 30, m.accent, true));
                card.add(UiTheme.label(m.line2, 142, 54, 50, 25, 13, UiTheme.MUTED, false));
                card.add(UiTheme.label(m.foot, 20, 79, 220, 20, 12, UiTheme.MUTED, false));
            } else {
                card.add(UiTheme.label("●  " + m.line1, 20, 39, 240, 22, 12, UiTheme.MUTED, false));
                card.add(UiTheme.label("●  " + m.line2, 20, 72, 240, 22, 12, UiTheme.MUTED, false));
                if (!m.foot.isEmpty()) card.add(UiTheme.label(m.foot, 170, 14, 92, 20, 11, UiTheme.MUTED, false));
            }
            addPageComponent(card);
        }
    }

    private record Metric(String title, String line1, String line2, String foot, Color accent) { }

    private static final class RateChart extends SurfacePanel {
        RateChart() {
            super(264, 280, 1152, 280);
            add(UiTheme.label("Lưu lượng theo thời gian", 20, 18, 380, 26, 16, UiTheme.WHITE, true));
            add(UiTheme.label("Download       Upload", 930, 18, 200, 24, 12, UiTheme.MUTED, false));
            add(UiTheme.label("MB/s", 20, 55, 80, 20, 12, UiTheme.MUTED, false));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left=64, top=86, right=getWidth()-28, bottom=getHeight()-42;
            for(int i=0;i<=4;i++){
                int y=top+(bottom-top)*i/4; g.setColor(UiTheme.EDGE); g.drawLine(left,y,right,y);
                g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.setColor(UiTheme.MUTED);g.drawString(Integer.toString(20-i*5),30,y+4);
            }
            int[] d={43,39,42,55,47,50,37,33,26,34,29,39,32,35,52,58,47,44,38,45,43,53,48,61,59,45,40,46,50,37,42,43,56,50,45,51,44,48,43,49,46,52};
            int[] u={13,11,9,12,10,8,7,12,11,8,7,16,14,12,11,13,15,12,13,10,12,14,11,13,22,24,20,18,17,14,12,10,11,12,11,13,12,9,8,7,8,9};
            int[] xs=new int[d.length], yd=new int[d.length], yu=new int[u.length];
            for(int i=0;i<d.length;i++){xs[i]=left+(right-left)*i/(d.length-1);yd[i]=bottom-(bottom-top)*d[i]/80;yu[i]=bottom-(bottom-top)*u[i]/80;}
            int[] px=new int[xs.length+2],py=new int[xs.length+2];px[0]=left;py[0]=bottom;System.arraycopy(xs,0,px,1,xs.length);System.arraycopy(yd,0,py,1,yd.length);px[px.length-1]=right;py[py.length-1]=bottom;
            g.setColor(new Color(45,212,191,22));g.fillPolygon(px,py,px.length);
            g.setStroke(new BasicStroke(2));g.setColor(UiTheme.TEAL);g.drawPolyline(xs,yd,xs.length);g.setColor(UiTheme.UPLOAD);g.drawPolyline(xs,yu,xs.length);
            String[] ticks={"13:32","13:42","13:52","14:02","14:12","14:22","14:32"};
            for(int i=0;i<ticks.length;i++){int x=left+(right-left)*i/(ticks.length-1);g.setColor(UiTheme.EDGE);g.drawLine(x,top,x,bottom);g.setColor(UiTheme.MUTED);g.drawString(ticks[i],x-18,bottom+20);}
            g.dispose();
        }
    }

    private static final class TrafficBreakdown extends SurfacePanel {
        private final String[] ips={"142.250.199.78","20.190.151.9","151.101.1.69","13.107.42.14","172.217.24.14","192.168.1.1"};
        private final double[] values={3.42,1.86,1.12,0.94,0.61,0.28};
        TrafficBreakdown() {
            super(264,576,1152,300);
            add(UiTheme.label("Thống kê lưu lượng",20,18,350,28,16,UiTheme.WHITE,true));
            JButton byIp=new JButton("Theo IP");byIp.setBounds(920,18,100,34);byIp.setBackground(UiTheme.BLUE);byIp.setForeground(Color.WHITE);byIp.setFocusPainted(false);add(byIp);
            JButton byPort=new JButton("Theo Port");byPort.setBounds(1020,18,100,34);byPort.setBackground(UiTheme.CARD);byPort.setForeground(UiTheme.MUTED);byPort.setFocusPainted(false);add(byPort);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int labelX=28, barX=170, maxW=790, startY=80;
            for(int i=0;i<ips.length;i++){
                int y=startY+i*34;g.setFont(new Font("Consolas",Font.PLAIN,12));g.setColor(UiTheme.MUTED);g.drawString(ips[i],labelX,y+17);
                int w=(int)(maxW*values[i]/4.0);g.setColor(UiTheme.BLUE);g.fillRoundRect(barX,y,w,22,5,5);
                g.setColor(UiTheme.MUTED);g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.drawString(String.format(java.util.Locale.US,"%.2f GB",values[i]),barX+w+8,y+16);
            }
            g.dispose();
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new StatisticsFrame().setVisible(true)); }
}

package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.BandwidthSample;
import com.nhom5.netmonitor.monitor.bandwidth.BandwidthMonitor.InterfaceInfo;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.nhom5.netmonitor.ui.UiTheme.*;

/** Huyền's dashboard cards displaying the selected adapter's live measurements. */
public final class DashboardFrame extends MonitorFrame {
    private final MetricCard down = new MetricCard("Download Speed", TEAL, "↓");
    private final MetricCard up = new MetricCard("Upload Speed", UPLOAD, "↑");
    private final TotalsCard totals = new TotalsCard();
    private final UsageCard usage = new UsageCard();
    private final TrafficChart chart = new TrafficChart(18, 60, 716, 572);
    private final NetworkCard network = new NetworkCard();
    private boolean loadingCards;

    DashboardFrame(MonitorSession session) {
        super(session, "Dashboard", 0, 1440, 900);
        addPageComponent(down);
        addPageComponent(up);
        addPageComponent(totals);
        addPageComponent(usage);
        Card traffic = new Card(264, 224, 752, 652);
        traffic.add(label("Lưu lượng mạng theo thời gian", 20, 20, 360, 28, 17, WHITE, true));
        traffic.add(label("Download  /  Upload · 60 giây", 486, 20, 246, 26, 12, MUTED, false));
        traffic.add(chart);
        addPageComponent(traffic);
        addPageComponent(network);
        network.cards.addActionListener(e -> {
            if (!loadingCards) session.select((InterfaceInfo) network.cards.getSelectedItem());
        });
        session.loadCards(this);
    }

    void setCards(List<InterfaceInfo> cards, InterfaceInfo selected) {
        loadingCards = true;
        network.cards.setModel(new javax.swing.DefaultComboBoxModel<>(cards.toArray(InterfaceInfo[]::new)));
        network.cards.setSelectedItem(selected);
        loadingCards = false;
    }

    void showSample(BandwidthSample sample, long received, long sent, double maxDown, double maxUp, String status) {
        down.update(sample == null ? null : sample.recvBytesPerSec(), maxDown);
        up.update(sample == null ? null : sample.sentBytesPerSec(), maxUp);
        totals.received.setText(sample == null ? "--" : TrafficChart.volume(received));
        totals.sent.setText(sample == null ? "--" : TrafficChart.volume(sent));
        usage.update(sample);
        network.update(sample, status);
        showStatus(sample == null ? status : "Dữ liệu thật", sample == null ? 0 : sample.timestampMillis());
    }

    void showHistory(List<BandwidthSample> history) { chart.showSamples(history, 60_000); }

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
        private final JLabel value;
        private final JLabel peak;

        MetricCard(String title, Color accent, String icon) {
            super(264 + (title.startsWith("Upload") ? 292 : 0), 88, 276, 120);
            add(label(title, 20, 20, 210, 22, 14, MUTED, false));
            value = label("--", 20, 49, 235, 42, 26, accent, true);
            peak = label("Cao nhất: --", 20, 88, 235, 20, 13, MUTED, false);
            add(value);
            add(peak);
            JPanel badge = new JPanel(null); badge.setBounds(230, 20, 26, 26); badge.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
            badge.add(label(icon, 5, 0, 20, 25, 18, accent, true)); add(badge);
        }
        void update(Double rate, double max) {
            value.setText(rate == null ? "--" : TrafficChart.rate(rate));
            peak.setText(rate == null ? "Cao nhất: --" : "Cao nhất: " + TrafficChart.rate(max));
        }
    }

    private static final class TotalsCard extends Card {
        private final JLabel received = label("--", 150, 52, 110, 23, 15, WHITE, true);
        private final JLabel sent = label("--", 150, 82, 110, 23, 15, WHITE, true);
        TotalsCard() {
            super(848, 88, 276, 120); add(label("Tổng dữ liệu trong phiên", 20, 20, 220, 22, 14, MUTED, false));
            add(label("●  Đã nhận", 20, 52, 115, 23, 14, MUTED, false)); add(received);
            add(label("●  Đã gửi", 20, 82, 115, 23, 14, MUTED, false)); add(sent);
            add(label("●", 20, 51, 16, 20, 12, TEAL, true)); add(label("●", 20, 81, 16, 20, 12, UPLOAD, true));
        }
    }

    private static final class UsageCard extends Card {
        private final JLabel value = label("--", 20, 51, 120, 40, 28, WHITE, true);
        private final JLabel link = label("", 138, 59, 124, 25, 12, MUTED, false);
        private double percent;

        UsageCard() {
            super(1140, 88, 276, 120);
            add(label("Băng thông sử dụng", 20, 20, 235, 22, 14, MUTED, false));
            add(value);
            add(link);
        }

        void update(BandwidthSample sample) {
            percent = sample == null ? 0 : sample.usagePercent().orElse(0);
            value.setText(sample != null && sample.usagePercent().isPresent()
                    ? String.format(Locale.ROOT, "%.1f%%", percent) : "--");
            link.setText(sample != null && sample.linkSpeedBitsPerSec() > 0
                    ? String.format(Locale.ROOT, "của %.0f Mbps", sample.linkSpeedBitsPerSec() / 1_000_000.0)
                    : "");
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(EDGE);
            g.fillRoundRect(20, 94, 236, 8, 8, 8);
            g.setColor(BLUE);
            g.fillRoundRect(20, 94, (int) (236 * percent / 100), 8, 8, 8);
        }
    }

    private static final class NetworkCard extends Card {
        private final JComboBox<InterfaceInfo> cards = new JComboBox<>();
        private final JLabel[] values = new JLabel[7];

        NetworkCard() {
            super(1032, 224, 384, 652);
            add(label("Card mạng đang theo dõi", 20, 20, 340, 28, 17, WHITE, true));
            cards.setBounds(20, 60, 344, 40);
            cards.setBackground(INPUT);
            cards.setForeground(WHITE);
            add(cards);
            String[] captions = {"Trạng thái", "Địa chỉ IPv4", "Địa chỉ MAC", "Tốc độ liên kết",
                "Gói tin đã gửi", "Gói tin đã nhận", "Lỗi gửi / nhận"};
            for (int i = 0; i < captions.length; i++) {
                int y = 118 + i * 40;
                add(label(captions[i], 20, y, 140, 36, 13, MUTED, false));
                values[i] = label("--", 160, y, 204, 36, 13, i == 0 ? GREEN : WHITE, true);
                add(values[i]);
            }
        }

        void update(BandwidthSample sample, String status) {
            var stats = sample == null ? null : sample.adapterStats();
            values[0].setText(sample == null ? status : "●  Đang nhận mẫu");
            values[1].setText(stats == null || stats.ipv4().isBlank() ? "--" : stats.ipv4());
            values[1].setToolTipText(values[1].getText());
            values[2].setText(stats == null || stats.mac().isBlank() ? "--" : stats.mac());
            values[3].setText(sample == null || sample.linkSpeedBitsPerSec() <= 0 ? "--"
                    : String.format(Locale.ROOT, "%.0f Mbps", sample.linkSpeedBitsPerSec() / 1_000_000.0));
            values[4].setText(stats == null ? "--" : Long.toString(stats.packetsSent()));
            values[5].setText(stats == null ? "--" : Long.toString(stats.packetsReceived()));
            values[6].setText(stats == null ? "--" : stats.sendErrors() + " / " + stats.receiveErrors());
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(EDGE);
            for (int y = 155; y < 430; y += 40) g.drawLine(20, y, getWidth() - 20, y);
        }
    }

    public static void main(String[] args) { MonitorSession.launch(0); }
}

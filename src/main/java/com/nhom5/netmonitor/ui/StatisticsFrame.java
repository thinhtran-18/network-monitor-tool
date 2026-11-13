package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.BandwidthSample;
import com.nhom5.netmonitor.core.ConnectionInfo;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

/** Statistics cards and breakdown from the original layout, backed by the active session. */
public final class StatisticsFrame extends MonitorFrame {
    private final JLabel[] values = new JLabel[4];
    private final JLabel[] details = new JLabel[4];
    private final JLabel[] breakdown = new JLabel[5];
    private final JProgressBar[] bars = new JProgressBar[5];
    private final TrafficChart chart = new TrafficChart(18, 52, 1116, 212);
    private List<BandwidthSample> history = List.of();
    private List<ConnectionInfo> connections = List.of();
    private long duration = 60_000;
    private boolean groupByPort;

    StatisticsFrame(MonitorSession session) {
        super(session, "Statistics", 2, 1440, 900);
        addTimeControls();
        addMetricCards();
        SurfacePanel rates = new SurfacePanel(264, 280, 1152, 280);
        rates.add(UiTheme.label("Lưu lượng theo thời gian", 20, 18, 380, 26, 16, UiTheme.WHITE, true));
        rates.add(UiTheme.label("Download  /  Upload", 930, 18, 200, 24, 12, UiTheme.MUTED, false));
        rates.add(chart);
        addPageComponent(rates);
        addBreakdown();
    }

    private void addTimeControls() {
        addPageComponent(UiTheme.label("Khoảng biểu đồ", 264, 94, 104, 38, 12, UiTheme.MUTED, false));
        ButtonGroup group = new ButtonGroup();
        int x = 372;
        for (int minutes : new int[] {1, 5, 15}) {
            JToggleButton button = new JToggleButton(minutes + " phút", minutes == 1);
            button.setBounds(x, 94, 94, 38);
            button.addActionListener(e -> {
                duration = minutes * 60_000L;
                chart.showSamples(history, duration);
            });
            group.add(button);
            addPageComponent(button);
            x += 94;
        }
        JButton export = new JButton("Xuất CSV");
        export.setBounds(1240, 94, 176, 38);
        export.setBackground(UiTheme.BLUE);
        export.setForeground(Color.WHITE);
        export.addActionListener(e -> exportHistory());
        addPageComponent(export);
    }

    private void addMetricCards() {
        String[] titles = {"Tổng đã nhận", "Tổng đã gửi", "Tốc độ trung bình", "Tốc độ cao nhất"};
        for (int i = 0; i < titles.length; i++) {
            SurfacePanel card = new SurfacePanel(264 + 292 * i, 152, 276, 112);
            card.add(UiTheme.label(titles[i], 20, 14, 235, 22, 13, UiTheme.MUTED, false));
            values[i] = UiTheme.label("--", 20, 42, 240, 32, i < 2 ? 23 : 15, UiTheme.TEAL, true);
            details[i] = UiTheme.label("Trong phiên chọn card", 20, 77, 240, 22, 13, UiTheme.MUTED, false);
            card.add(values[i]);
            card.add(details[i]);
            addPageComponent(card);
        }
    }

    private void addBreakdown() {
        SurfacePanel card = new SurfacePanel(264, 576, 1152, 300);
        card.add(UiTheme.label("Số kết nối theo đầu bên kia", 20, 18, 580, 28, 16, UiTheme.WHITE, true));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 2; i++) {
            boolean byPort = i == 1;
            JToggleButton button = new JToggleButton(byPort ? "Theo Port" : "Theo IP", !byPort);
            button.setBounds(920 + i * 100, 18, 100, 34);
            button.addActionListener(e -> {
                groupByPort = byPort;
                updateBreakdown();
            });
            group.add(button);
            card.add(button);
        }
        for (int i = 0; i < breakdown.length; i++) {
            int y = 66 + 36 * i;
            breakdown[i] = UiTheme.label("", 28, y, 390, 32, 14, UiTheme.WHITE, false);
            bars[i] = new JProgressBar(0, 100);
            bars[i].setBounds(428, y + 7, 682, 18);
            bars[i].setBackground(UiTheme.INPUT);
            bars[i].setForeground(UiTheme.BLUE);
            bars[i].setBorderPainted(false);
            bars[i].setVisible(false);
            card.add(breakdown[i]);
            card.add(bars[i]);
        }
        addPageComponent(card);
        updateBreakdown();
    }

    void showData(List<BandwidthSample> history, List<ConnectionInfo> connections, long received, long sent,
                  double avgDown, double avgUp, double maxDown, double maxUp) {
        boolean ready = !history.isEmpty();
        if (this.history != history && !this.history.equals(history)) chart.showSamples(history, duration);
        this.history = history;
        if (this.connections != connections) {
            this.connections = connections;
            updateBreakdown();
        }
        values[0].setText(ready ? TrafficChart.volume(received) : "--");
        values[1].setText(ready ? TrafficChart.volume(sent) : "--");
        values[2].setText(ready ? "Nhận " + TrafficChart.rate(avgDown) : "--");
        details[2].setText(ready ? "Gửi " + TrafficChart.rate(avgUp) : "");
        values[3].setText(ready ? "Nhận " + TrafficChart.rate(maxDown) : "--");
        details[3].setText(ready ? "Gửi " + TrafficChart.rate(maxUp) : "");
    }

    private void updateBreakdown() {
        Map<String, Long> counts = connections.stream()
                .filter(row -> row.remotePort() > 0)
                .collect(Collectors.groupingBy(row -> groupByPort
                        ? Integer.toString(row.remotePort()) : row.remoteAddress(), Collectors.counting()));
        var top = counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue()
                .reversed().thenComparing(Map.Entry.comparingByKey())).limit(breakdown.length).toList();
        long largest = top.isEmpty() ? 1 : top.get(0).getValue();
        for (int i = 0; i < breakdown.length; i++) {
            bars[i].setVisible(i < top.size());
            if (i < top.size()) bars[i].setValue((int) (100.0 * top.get(i).getValue() / largest));
            breakdown[i].setText(i < top.size()
                    ? top.get(i).getKey() + "     " + top.get(i).getValue() + " kết nối"
                    : i == 0 ? "Chưa có kết nối với đầu bên kia" : "");
        }
    }

    private void exportHistory() {
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có mẫu băng thông để xuất.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("netmonitor.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        var target = chooser.getSelectedFile().toPath();
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this, "Ghi đè tệp đã có?",
                "Xuất CSV", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        List<BandwidthSample> snapshot = history;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws IOException {
                try (var writer = Files.newBufferedWriter(target)) {
                    writer.write("timestamp_ms,recv_bytes_per_second,sent_bytes_per_second\n");
                    for (BandwidthSample sample : snapshot) {
                        writer.write(sample.timestampMillis() + "," + sample.recvBytesPerSec()
                                + "," + sample.sentBytesPerSec() + "\n");
                    }
                }
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(StatisticsFrame.this, "Đã lưu " + target);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StatisticsFrame.this, "Không thể xuất CSV: " + e.getMessage());
                }
            }
        }.execute();
    }

    public static void main(String[] args) { MonitorSession.launch(2); }
}

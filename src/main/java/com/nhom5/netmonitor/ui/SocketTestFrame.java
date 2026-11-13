package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.network.SocketService;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/** Original Socket Test controls backed by the session TCP service. */
public final class SocketTestFrame extends MonitorFrame {
    private final JLabel statusText = UiTheme.label("Chưa kết nối", 20, 46, 220, 30, 19, UiTheme.MUTED, true);
    private final JLabel sidebarStatus = UiTheme.label("●  Chưa kết nối", 20, 362, 320, 28, 11, UiTheme.MUTED, false);

    private final JLabel count = UiTheme.label("0 bản ghi", 250, 18, 100, 28, 12, UiTheme.MUTED, false);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Thời gian", "Loại", "Nội dung", "Dung lượng", "Tốc độ card"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    SocketTestFrame(MonitorSession session) {
        super(session, "Socket Test", 3, 1512, 979);
        showStatus("TCP · Chờ kết nối", 0);
        addConfiguration();
        addSummaryCards();
        addLogPanel();
    }

    private void addConfiguration() {
        SurfacePanel config = new SurfacePanel(264, 88, 360, 400);
        config.add(UiTheme.label("Cấu hình kết nối", 20, 20, 300, 28, 17, UiTheme.WHITE, true));
        config.add(UiTheme.label("Chế độ", 20, 55, 120, 24, 12, UiTheme.MUTED, false));
        JPanel mode = new JPanel(null); mode.setBounds(20, 80, 320, 36); mode.setBackground(UiTheme.INPUT); mode.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE));
        JRadioButton server = new JRadioButton("Server"), client = new JRadioButton("Client", true);
        for (JRadioButton radio : new JRadioButton[]{server, client}) {
            radio.setBounds(radio == server ? 0 : 160, 0, 160, 36); radio.setHorizontalAlignment(JLabel.CENTER);
            radio.setForeground(radio.isSelected() ? Color.WHITE : UiTheme.MUTED); radio.setBackground(radio.isSelected() ? UiTheme.BLUE : UiTheme.INPUT);
            radio.setFont(new Font("Segoe UI", Font.PLAIN, 13)); radio.setFocusPainted(false); mode.add(radio);
        }
        ButtonGroup group = new ButtonGroup(); group.add(server); group.add(client);
        server.addActionListener(e -> { server.setBackground(UiTheme.BLUE); server.setForeground(Color.WHITE); client.setBackground(UiTheme.INPUT); client.setForeground(UiTheme.MUTED); });
        client.addActionListener(e -> { client.setBackground(UiTheme.BLUE); client.setForeground(Color.WHITE); server.setBackground(UiTheme.INPUT); server.setForeground(UiTheme.MUTED); });
        config.add(mode);
        config.add(UiTheme.label("Địa chỉ IP", 20, 126, 200, 22, 12, UiTheme.MUTED, false));
        JTextField ip = field("127.0.0.1"); ip.setBounds(20, 153, 320, 36); config.add(ip);
        config.add(UiTheme.label("Port", 20, 194, 200, 22, 12, UiTheme.MUTED, false));
        JTextField port = field("8888"); port.setBounds(20, 221, 320, 36); config.add(port);
        JButton connect = actionButton("Connect", 20, 285, 150, 36, UiTheme.BLUE, Color.WHITE);
        JButton disconnect = actionButton("Disconnect", 186, 285, 154, 36, UiTheme.CARD, new Color(151, 66, 76));
        server.addActionListener(e -> ip.setEnabled(false));
        client.addActionListener(e -> ip.setEnabled(true));
        connect.addActionListener(e -> {
            String host = ip.getText().strip();
            boolean listening = server.isSelected();
            int number;
            try {
                number = Integer.parseInt(port.getText().strip());
                if (number < 1 || number > 65535 || (!listening && host.isEmpty())) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException invalid) {
                JOptionPane.showMessageDialog(this, "Nhập địa chỉ máy chủ và port từ 1 đến 65535.");
                return;
            }
            session.runSocketAction(() -> {
                if (listening) session.socket.startServer(number);
                else session.socket.connect(host, number);
            });
        });
        disconnect.addActionListener(e -> session.runSocketAction(session.socket::stop));
        config.add(connect); config.add(disconnect);
        config.add(UiTheme.label("Trạng thái", 20, 338, 120, 30, 13, UiTheme.MUTED, false));
        sidebarStatus.setBounds(20, 362, 320, 28); config.add(sidebarStatus);
        addPageComponent(config);
    }

    private void addSummaryCards() {
        addPageComponent(summaryCard(640, "Trạng thái", "Chưa kết nối", "--", 0));
        addPageComponent(summaryCard(928, "Dữ liệu qua TCP", "Mẫu card mạng", "Xem tốc độ card trong nhật ký", 1));
        addPageComponent(summaryCard(1216, "Phép đo đường truyền", "--", "Chưa đo tốc độ và độ trễ", 2));
    }

    private SurfacePanel summaryCard(int x, String title, String value, String footer, int type) {
        SurfacePanel card = new SurfacePanel(x, 88, 256, 112);
        card.add(UiTheme.label(title, 20, 18, 210, 20, 12, UiTheme.MUTED, false));
        if (type == 0) card.add(statusText);
        else card.add(UiTheme.label(value, 20, 48, 210, 30, 23, UiTheme.MUTED, true));
        card.add(UiTheme.label(footer, 20, 78, 220, 22, 12, UiTheme.MUTED, false));
        return card;
    }

    private void addLogPanel() {
        SurfacePanel log = new SurfacePanel(640, 216, 848, 739);
        log.add(UiTheme.label("Nhật ký truyền dữ liệu", 20, 18, 230, 28, 16, UiTheme.WHITE, true));
        log.add(count);
        JButton clear = actionButton("Xóa nhật ký", 660, 18, 164, 36, UiTheme.CARD, UiTheme.MUTED);
        log.add(clear);
        JTable table = new JTable(model);
        table.setRowHeight(36); table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(UiTheme.CARD); table.setForeground(UiTheme.WHITE); table.setGridColor(UiTheme.EDGE);
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(1, 40));
        table.getTableHeader().setBackground(UiTheme.SIDEBAR); table.getTableHeader().setForeground(UiTheme.MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table); scroll.setBounds(1, 64, 846, 674); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.CARD); scroll.setBackground(UiTheme.CARD); log.add(scroll);
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(85);
        table.getColumnModel().getColumn(2).setPreferredWidth(340);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(210);
        clear.addActionListener(e -> {
            model.setRowCount(0);
            count.setText("0 bản ghi");
        });
        addPageComponent(log);
    }

    void showSocketStatus(String status) {
        String shortStatus = status.startsWith("Server") ? "Đang lắng nghe"
                : status.startsWith("Đã kết nối") ? "Đã kết nối"
                : status.startsWith("Lỗi") || status.startsWith("Mất") ? "Lỗi kết nối"
                : status.startsWith("Đang") ? status : "Chưa kết nối";
        statusText.setText(shortStatus);
        sidebarStatus.setText(status);
        sidebarStatus.setToolTipText(status);
        showStatus("TCP · " + shortStatus, System.currentTimeMillis());
    }

    void appendEvent(SocketService.Event event) {
        if (model.getRowCount() >= 500) model.removeRow(0);
        SocketService.Metrics sample = event.metrics();
        String rate = sample == null ? "--" : TrafficChart.rate(sample.recvBytesPerSec())
                + " ↓ / " + TrafficChart.rate(sample.sentBytesPerSec()) + " ↑";
        model.addRow(new Object[] {formatTime(System.currentTimeMillis()), event.type(), event.message(),
            event.bytes() > 0 ? event.bytes() + " B" : "--", rate});
        count.setText(model.getRowCount() + " bản ghi");
    }

    private static JTextField field(String text) {
        JTextField field = new JTextField(text); field.setBackground(UiTheme.INPUT); field.setForeground(UiTheme.WHITE);
        field.setCaretColor(UiTheme.WHITE); field.setFont(new Font("Consolas", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.EDGE), BorderFactory.createEmptyBorder(0, 12, 0, 8)));
        return field;
    }

    private static JButton actionButton(String text, int x, int y, int w, int h, Color bg, Color fg) {
        JButton b = new JButton(text); b.setBounds(x, y, w, h); b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13)); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE)); return b;
    }

    public static void main(String[] args) { MonitorSession.launch(3); }
}
